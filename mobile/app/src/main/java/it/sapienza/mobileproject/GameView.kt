package it.sapienza.mobileproject

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import it.sapienza.mobileproject.entities.game.*
import kotlin.reflect.KVisibility



@Suppress("UNREACHABLE_CODE")
class GameView(context: Context?, gameReference: DatabaseReference, ball: String, racket: String) : View(context),
    SensorEventListener2 {

    private val ballBitmap: Bitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeStream(context?.assets?.open(ball)),
        200,
        200,
        false
    )

    private val racketBitmap: Bitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeStream(context?.assets?.open(racket)),
        200,
        200,
        false
    )

    /** ball radius */
    var ball: Ball = Ball()

    /** ball painter: shader with radial gradient, clamp tile mode */
    private var ballPainter = Paint().apply {
       shader = BitmapShader(ballBitmap, Shader.TileMode.MIRROR, Shader.TileMode.REPEAT)
        //shader = RadialGradient(-130.5f, -300.5f, 60f, Color.WHITE, Color.rgb(255, 153, 0), Shader.TileMode.CLAMP)
    }

    var ballPainter2 = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 5f
    }

    /** player painter: shader with radial gradient, clamp tile mode */
    var playerPainter1 = Paint().apply {
        shader = BitmapShader(racketBitmap, Shader.TileMode.MIRROR, Shader.TileMode.REPEAT)
        //color = Color.RED
    }


    var buttonPainter = Paint().apply {
       color=Color.rgb(248, 95, 106)
    }

    /** players */
    private var currentPlayer = PlayerInfo()
    private var adversaryPlayer = PlayerInfo()
    private var playerSpeed = 100

    /** local matrix (to update shader position wrt ball movements) */
    var m = Matrix()

    /** timestamp: for animation */
    var currenTime: Long = System.currentTimeMillis()

    private val gameRef: DatabaseReference = gameReference

    /** game variables */
    private var game: Game = Game()
    private var canCollide: Boolean = true
    private var lastCollision: Long = System.currentTimeMillis()

    private var ready: Boolean = false

    private var buttonBox = Box()
    private var debugBox = Box()

    private var textPainter = Paint().apply {
        isAntiAlias = true
        textSize = 16 * resources.displayMetrics.density
        color = Color.WHITE
    }


    private var textPainterGoal = Paint().apply {
        isAntiAlias = true
        textSize = 35 * resources.displayMetrics.density
        color = Color.rgb(94, 2, 2)
        typeface = Typeface.DEFAULT_BOLD
        //typeface = resources.getFont(R.font.asap_medium_italic)
    }

    init {
        /** sensor initialization */
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // accelerometer
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        // game listener: create listeners for changes in the database
        val gameListener = object : ValueEventListener {

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "game listener on cancelled $error")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get game object and use the values to update the UI
                game = dataSnapshot.getValue<Game>()!!

                game::class.members.forEach { it ->
                    if (it.visibility == KVisibility.PUBLIC) {
                        if (it.name == adversaryPlayer.label) {
                            val adversaryPlayerRef = it.call(game) as Player?
                            adversaryPlayer.position = -playerSpeed * adversaryPlayerRef?.position!!
                        } else if (it.name == currentPlayer.label) {
                            val currentPlayerRef = it.call(game) as Player?
                            if (currentPlayer.score != currentPlayerRef?.score) {
                                currentPlayer.score = currentPlayerRef?.score!!
                                Log.i(
                                    TAG,
                                    "GOAL!!! ${currentPlayer.label} - ${currentPlayer.score} : ${adversaryPlayer.label} - ${adversaryPlayer.score}"
                                )
                            }
                        }
                    }
                }
            }
        }

        gameRef.addValueEventListener(gameListener)
    }

    override fun onDraw(canvas: Canvas?) {
        if (ready) {
            super.onDraw(canvas)
            // draw debug button (if debug)
            //if(BuildConfig.DEBUG){
            //    drawDebugButton(canvas)
            //}
            // draw current player
            drawPlayer(canvas, true)
            // draw adversary
            drawPlayer(canvas, false)
            // draw the ball
            canvas?.drawCircle(ball.center.x, ball.center.y, ball.radius, ballPainter)
            //canvas?.drawCircle(ball.center.x, ball.center.y, ball.radius, ballPainter2)
            // update shader coordinates by applying translation to local matrix
            if (!game.gameOver) {
                Log.i(TAG, "width $height")
                val dt = getDeltaTime()
                if (game.hasStarted) {
                    // update ball position
                    updateBall(dt)
                    // boundary check : either one of left/right (for x) or top/bottom (for y) constraints must be satisfied
                    checkBoundaries()
                } else if (ball.radius == Constants.DEFAULT_BALL_RADIUS) {
                    ball.radius = (width / Constants.BALL_RADIUS).toFloat()

                    ball.velocity = Coordinates(
                        (width / Constants.BALL_VELOCITY_X).toFloat(),
                        (height / Constants.BALL_VELOCITY_Y).toFloat()
                    )
                    playerSpeed = width / Constants.PLAYER_SPEED
                } else if (adversaryPlayer.score > 0 || currentPlayer.score > 0) {
                    goal(canvas)
                }
                // invalidate view and re-run onDraw
                if (canPlayerTouch()) {
                    drawReadyButton(canvas)
                }
                updateShader()
                invalidate()
            }
        }
    }

    fun setLabels(currentLabel: String, adversaryLabel: String) {
        currentPlayer.label = currentLabel
        adversaryPlayer.label = adversaryLabel
    }

    fun setReady() {
        ready = true;
        invalidate();
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // if !game has started
        if (canPlayerTouch() &&
            event?.y!! >= buttonBox.top && event?.y!! <= buttonBox.bottom &&
            event?.x!! >= buttonBox.left && event?.x!! <= buttonBox.right
        ) {
            Log.d("game", "START/STOP")
            currenTime = System.currentTimeMillis()
            gameRef.child("hasStarted").setValue(!game.hasStarted)
        }
        if (BuildConfig.DEBUG &&
            event?.y!! >= debugBox.top && event?.y!! <= debugBox.bottom &&
            event?.x!! >= debugBox.left && event?.x!! <= debugBox.right
        ) {
            gameOver()
        }
        return true
    }

    private fun canPlayerTouch(): Boolean {
        return !game.hasStarted && game.playerTurn == currentPlayer.label
    }

    private fun getDeltaTime(): Long {
        val nowTime = System.currentTimeMillis()
        val dt = nowTime - currenTime
        currenTime = nowTime
        if (!canCollide && (currenTime - lastCollision > 500)) {
            canCollide = true
        }
        return dt
    }

    private fun drawPlayer(canvas: Canvas?, isCurrentPlayer: Boolean) {
        var top = 0
        var bottom = 0
        var left = width / 2 - width / Constants.PLAYER_WIDTH_OFFSET
        var right = width / 2 + width / Constants.PLAYER_WIDTH_OFFSET
        // set top-bottom according to player position (p1 is up on the screen, p2 stays down)
        if (isCurrentPlayer) {
            top = height - height / Constants.PLAYER_HEIGHT_OFFSET
            bottom = height
            left -= (currentPlayer.position / 2).toInt()
            right -= (currentPlayer.position / 2).toInt()
        } else {
            bottom = height / Constants.PLAYER_HEIGHT_OFFSET
            left -= (adversaryPlayer.position / 2).toInt()
            right -= (adversaryPlayer.position / 2).toInt()
        }
        // check screen boundaries (when player goes beyond screen size, resize the player rectangle
        if (left <= 0) {
            left = 0
            right = width / (Constants.PLAYER_WIDTH_OFFSET / 2)
        } else if (right >= width) {
            left = width - width / (Constants.PLAYER_WIDTH_OFFSET / 2)
            right = width
        }
        /** store players box */
        if (isCurrentPlayer) {
            currentPlayer.box = Box(top, bottom, left, right)
        } else {
            adversaryPlayer.box = Box(top, bottom, left, right)
        }
        /** set ball position according to user */
        if (!game.hasStarted) {
            if (currentPlayer.label == game.playerTurn && isCurrentPlayer) {
                // if current player is player 1, draw the ball to him
                ball.center = Coordinates(
                    (left + width / Constants.PLAYER_WIDTH_OFFSET).toFloat(),
                    height - 2 * ball.radius - Constants.COLLISION_OFFSET
                )
                // adjust speed to match the player position
                if (ball.velocity.y > 0) {
                    ball.velocity.y *= -1
                    ball.velocity.x *= -1
                }
            } else if (adversaryPlayer.label == game.playerTurn && !isCurrentPlayer) {
                // otherwise draw the ball to adversary
                ball.center = Coordinates(
                    (left + width / Constants.PLAYER_WIDTH_OFFSET).toFloat(),
                    2 * ball.radius + Constants.COLLISION_OFFSET
                )
            }
        }
        canvas?.drawRect(Rect(left, top, right, bottom), ballPainter2)
        canvas?.drawRect(Rect(left, top, right, bottom), playerPainter1)

    }

    private fun drawReadyButton(canvas: Canvas?) {
        var top = height / 2 - Constants.PLAYER_HEIGHT_OFFSET * 2
        var bottom = height / 2 + Constants.PLAYER_HEIGHT_OFFSET * 2
        var left = width / 2 - width / Constants.PLAYER_WIDTH_OFFSET
        var right = width / 2 + width / Constants.PLAYER_WIDTH_OFFSET
        buttonBox = Box(top, bottom, left, right)
        canvas?.drawRect(Rect(left, top, right, bottom), buttonPainter)
        val readyText = context.getString(R.string.button_play_start)
        val textWidth = textPainter.measureText(readyText)
        canvas?.drawText(
            readyText,
            (width / 2).toFloat()-36 ,
            (height / 2).toFloat()+15,
            textPainter
        )
    }

    private fun drawDebugButton(canvas: Canvas?) {
        var top = height / 4 - Constants.PLAYER_HEIGHT_OFFSET * 2
        var bottom = height / 4 + Constants.PLAYER_HEIGHT_OFFSET * 2
        var left = width / 2 - width / Constants.PLAYER_WIDTH_OFFSET
        var right = width / 2 + width / Constants.PLAYER_WIDTH_OFFSET
        debugBox = Box(top, bottom, left, right)
        canvas?.drawRect(Rect(left, top, right, bottom), buttonPainter)
        val readyText = "EXIT"
        val textWidth = textPainter.measureText(readyText)
        canvas?.drawText(
            readyText,
            (width / 2).toFloat() - textWidth/2,
            (height / 4).toFloat()+15,
            textPainter
        )
    }


    private fun goal(canvas: Canvas?) {
        var scorePlayer = ""
        when (game.playerTurn) {
            currentPlayer.label -> {
                scorePlayer = adversaryPlayer.label
            }
            adversaryPlayer.label -> {
                scorePlayer = currentPlayer.label
            }
        }

        val text = "GOAL!!!"
        val textWidth = textPainterGoal.measureText(text)


        canvas?.drawText(
            text,
            (width / 2).toFloat() -200,
            (height / 2).toFloat()-300,
            textPainterGoal
        )
    }

    private fun updateShader() {
        // light offsets: translate the inner white dynamically
        val offX = 5 * ball.center.x / width - 1
        val offY = 5 * ball.center.y / height - 1
        m.setTranslate(ball.center.x + ball.radius * offX, ball.center.y + ball.radius * offY)
        ballPainter.shader.setLocalMatrix(m)
    }

    private fun updateBall(dt: Long) {
        ball.center.x += ball.velocity.x * dt / Constants.DEFAULT_BALL_TIME
        ball.center.y += ball.velocity.y * dt / Constants.DEFAULT_BALL_TIME
    }

    private fun checkBoundaries() {
        // when ball hits the border of the screens
        if (canCollide) {
            if (ball.center.x >= width - ball.radius - Constants.COLLISION_OFFSET
                || ball.center.x <= ball.radius + Constants.COLLISION_OFFSET
            ) {
                Log.i(TAG, "BORDER COLLISION ${ball.center.y}")
                ball.velocity.x *= -1f
                canCollide = false
                lastCollision = System.currentTimeMillis()
            }
            // player collision check
            if ((ball.center.x + ball.radius).toInt() >= currentPlayer.box.left &&
                (ball.center.x - ball.radius).toInt() <= currentPlayer.box.right && ball.center.y.toInt() >= currentPlayer.box.top - Constants.COLLISION_OFFSET
            ) {
                // current player collision
                Log.i(TAG, "CURRENT PLAYER COLLISION ${ball.center.y} ${currentPlayer.box.top}")
                ball.velocity.y *= -1f
                canCollide = false
                lastCollision = System.currentTimeMillis()
            } else if (((ball.center.x + ball.radius).toInt() >= adversaryPlayer.box.left &&
                        (ball.center.x - ball.radius).toInt() <= adversaryPlayer.box.right &&
                        ball.center.y.toInt() <= adversaryPlayer.box.bottom) ||
                (ball.center.y <= ball.radius)
            ) {
                // adversary player collision
                Log.i(TAG, "ADVERSARY PLAYER COLLISION")
                ball.velocity.y *= -1f
                canCollide = false
                lastCollision = System.currentTimeMillis()
            } else if (ball.center.y > height - ball.radius) {
                // adversary has scored a point
                canCollide = false
                lastCollision = System.currentTimeMillis()
                adversaryPlayer.score++ // increase player score
                gameRef.child(adversaryPlayer.label).child("score").setValue(adversaryPlayer.score)
                Log.i(
                    TAG,
                    "GOAL!!! ${currentPlayer.label} - ${currentPlayer.score} : ${adversaryPlayer.label} - ${adversaryPlayer.score}"
                )
                if (adversaryPlayer.score == game.maxScore) {
                    // if score has reached max, call game over
                    gameOver()
                } else {
                    // score is not yet max, restart the match and pass the ball to adversary
                    gameRef.child("hasStarted").setValue(false)
                    gameRef.child("playerTurn").setValue(adversaryPlayer.label)
                }

            }
        }
    }

    private fun gameOver(){
        Log.i(TAG, "GAME OVER")
        game.gameOver = true
        gameRef.child("gameOver").setValue(game.gameOver)
    }

    private fun map(input: Float, toMin: Int, toMax: Int, fromMin: Int, fromMax: Int): Float {
        return (input - fromMin) / (fromMax - fromMin) * (toMax - toMin) + toMin
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.i(TAG, "onAccuracyChanged: Not yet implemented")
    }

    override fun onFlushCompleted(p0: Sensor?) {
        Log.i(TAG, "onFlushCompleted: Not yet implemented")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            currentPlayer.position = event.values[0].toDouble()
            gameRef.child(currentPlayer.label).child("position").setValue(currentPlayer.position)
            currentPlayer.position *= playerSpeed
        }
    }

    companion object {
        private const val TAG = "GameView"
    }
}