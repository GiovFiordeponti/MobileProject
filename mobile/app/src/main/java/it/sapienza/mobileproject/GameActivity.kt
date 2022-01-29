package it.sapienza.mobileproject

import android.app.Activity
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.sapienza.mobileproject.entities.dto.GameDeleteRequest
import it.sapienza.mobileproject.entities.dto.TokenRequest
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.game.Game
import it.sapienza.mobileproject.entities.game.Player
import it.sapienza.mobileproject.fragments.WebInterface
import it.sapienza.mobileproject.singletons.VolleySingleton
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.activity_game_over.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.include_progress_bar.*
import org.json.JSONObject
import kotlin.reflect.KVisibility

class GameActivity : AppCompatActivity(), WebInterface {

    private var gameView: GameView? = null

    private lateinit var gameInfo: GameInfo

    private lateinit var player: Player

    private lateinit var adversaryLabel: String

    private lateinit var adversaryPlayer: Player

    private lateinit var gameRef: DatabaseReference

    private lateinit var volley: VolleySingleton

    private lateinit var table: String

    private val gameListener = object : ValueEventListener {

        override fun onCancelled(error: DatabaseError) {
            Log.i("TODO", "game listener on cancelled $error")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Get game object and use the values to update the UI
            val game = dataSnapshot.getValue<Game>()!!
            var readyCount = 0
            if (game.gameOver) {
                game::class.members.forEach { it ->
                    if (it.visibility == KVisibility.PUBLIC) {
                        if (Constants.PLAYERS.contains(it.name)) {
                            // for each player, check if its ready
                            val playerRef = it.call(game) as Player?
                            // update players
                            if (it.name == gameInfo.playerRole) player =
                                playerRef!! else adversaryPlayer = playerRef!!
                        }
                    }
                }
                gameOver()
            } else if (!game.ready) {
                game::class.members.forEach { it ->
                    if (it.visibility == KVisibility.PUBLIC) {
                        if (Constants.PLAYERS.contains(it.name)) {
                            // for each player, check if its ready
                            val playerRef = it.call(game) as Player?
                            Log.d(
                                "TAG",
                                "onDataChange: PLAYER ${it.name} READY ${playerRef!!.ready}"
                            )
                            if (playerRef!!.ready) {
                                readyCount++ // update count
                            }
                        }
                    }
                }
                // if both players are ready, launch the game
                if (readyCount == 2) {
                    launchGame()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get volley instance
        volley = this?.let { VolleySingleton.getInstance(it) }!!
        // Get the Intent that started this activity and extract the string
        gameInfo = intent.getParcelableExtra<GameInfo>(EXTRA_MESSAGE)
        val ball = intent.getStringExtra(Constants.INTENT_KEY_BALL)
        val racket = intent.getStringExtra(Constants.INTENT_KEY_RACKET)
        table = intent.getStringExtra(Constants.INTENT_KEY_TABLE)
        // game listener: create listeners for changes in the database
        gameRef = Firebase.database.reference.child("games").child(gameInfo.matchId)
        gameRef.addValueEventListener(gameListener)
        gameView = GameView(this.applicationContext, gameRef, ball, racket)
        // set player labels
        adversaryLabel = Constants.PLAYERS.filterIndexed { _, s ->
            s != gameInfo.playerRole
        }.first()
        gameView!!.setLabels(
            gameInfo.playerRole,
            adversaryLabel
        )
        initSkins()
        // set content view
        setContentView(R.layout.activity_game)
        initUI()
    }

    private fun initSkins(){

    }
    private fun initUI() {
        gameRef.child(gameInfo.playerRole).get().addOnCompleteListener {
            player = it.result?.getValue<Player>()!!
            my_nickname.text = player.nickname
        }
        gameRef.child(adversaryLabel).get().addOnCompleteListener {
            adversaryPlayer = it.result?.getValue<Player>()!!
            enemy_nickname.text = adversaryPlayer.nickname
            enemy_code.text = formatFriendNumber(adversaryPlayer.friendCode)
        }
        button_copy_friend_code.setOnClickListener { _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label",adversaryPlayer.friendCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                applicationContext,
                String.format(getString(R.string.friend_text_copied), adversaryPlayer.nickname),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun gameReady(view: View) {

        when (view.id) {
            R.id.ready -> {
                Log.i("game", "ready ${view.id}")
                // Show progress overlay (with animation):
                if(progress_overlay!=null) {
                    animateView(progress_overlay, View.VISIBLE, 0.4f, 200)
                }
            }
        }
        gameRef.child(gameInfo.playerRole).child("ready").setValue(true)
        gameView!!.setReady()
    }

    //Set Background
    fun launchGame() {
        if(progress_overlay!=null) {
            animateView(progress_overlay, View.GONE, 0f, 200)
        }
        gameRef.child("ready").setValue(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            when(table){
                getString(R.string.default_table_file) ->
                    gameView?.background = getDrawable(R.drawable.default_table)
                getString(R.string.table_1_file) ->
                    gameView?.background = getDrawable(R.drawable.soccer_field)
                getString(R.string.table_2_file) ->
                    gameView?.background = getDrawable(R.drawable.basket_field)
                getString(R.string.table_3_file) ->
                    gameView?.background = getDrawable(R.drawable.space_field)
            }

        }
        setContentView(gameView)
    }

    fun gameOver() {
        // set content view
        gameRef.removeEventListener(gameListener)
        Log.d(TAG, "GAME OVER ${Gson().toJson(player)} ${Gson().toJson(adversaryPlayer)}")
        setContentView(R.layout.activity_game_over)
        // set label
        if(player.score == Constants.MAX_SCORE){
            text_game_over.text = getString(R.string.win_label)
        }
        else if(adversaryPlayer.score == Constants.MAX_SCORE){
            text_game_over.text = getString(R.string.lose_label)
        }
        else{
            text_game_over.text = getString(R.string.cancel_label)
        }

        button_game_over.setOnClickListener {
            if(progress_overlay!=null) {
                animateView(progress_overlay, View.GONE, 0f, 200)
            }
            volley.volleyRequest(
                Request.Method.POST,
                BuildConfig.GAME_FINISH,
                Gson().toJson(GameDeleteRequest(gameInfo,
                    player.score == Constants.MAX_SCORE,
                    player.score != Constants.MAX_SCORE && adversaryPlayer.score  != player.score)),
                responseListener,
                errorListener
            )

        }

        button_share_victory.setOnClickListener {
            shareResult()
        }
    }

    private fun shareResult() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                String.format(
                    getString(R.string.share_result),
                    player.nickname,
                    adversaryPlayer.nickname,
                    player.score,
                    adversaryPlayer.score)
            )
            type = "text/plain"
        }
        // Always use string resources for UI text.
        // This says something like "Share this photo with"
        val title = resources.getString(R.string.chooser_title)
        // Create intent to show chooser
        val chooser = Intent.createChooser(sendIntent, title)

        // Verify the intent will resolve to at least one activity
        if (sendIntent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        }

    }

    override fun onBackPressed() {
        val textView = TextView(this)
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(getString(R.string.game_warning))
            builder.setMessage(getString(R.string.game_warning_msg))
            //setContentView(textView)
            builder.apply {
                setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { _, _ ->
                        // User clicked OK button
                        gameRef.child("gameOver").setValue(true)
                        super.onBackPressed()
                    })
                setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        // User cancelled the dialog
                    })
            }
            // Set other dialog properties

            // Create the AlertDialog
            builder.create()
        }
        alertDialog?.show()

    }

    companion object {
        private const val TAG = "GameActivity"
    }

    override fun onResponseCallback(response: JSONObject) {
        Log.d(TAG,"Received response ${response.toString()}")
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onErrorCallback(error: VolleyError) {
        Log.d(TAG,"Not yet implemented")
    }

    /** volley interface */
    private val responseListener = Response.Listener<JSONObject> { response ->
        onResponseCallback(response)
    }

    private val errorListener = Response.ErrorListener { error ->
        onErrorCallback(error)
    }

}