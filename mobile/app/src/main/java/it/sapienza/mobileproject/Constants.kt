package it.sapienza.mobileproject

object Constants{
    /** GAME CONSTANTS */
    val PLAYERS = arrayOf("player1", "player2")
    const val MAX_SCORE: Int = 3
    const val DEFAULT_BALL_RADIUS: Float = 10f
    const val BALL_RADIUS: Int = 20
    const val BALL_VELOCITY_X: Int = 2
    const val BALL_VELOCITY_Y: Int = 4
    const val PLAYER_SPEED: Int = 8
    const val PLAYER_WIDTH_OFFSET: Int = 8
    const val PLAYER_HEIGHT_OFFSET: Int = 30
    const val DEFAULT_BALL_VELOCITY: Float = 700f
    const val DEFAULT_BALL_CENTER: Float = 0f
    const val DEFAULT_BALL_TIME: Int = 1000
    const val DEFAULT_PLAYER: String = "player1"
    const val COLLISION_OFFSET: Float = 10f
    /** INTENTS */
    const val AUTH_MESSAGE = "AUTH"
    const val USER_KEY = "USER_KEY"
    /** DATABASE */
    const val DATABASE_NAME = "database_name"
    /** ACTIONS */
    const val ACTION_PLAY = "ACTION_PLAY"
    const val ACTION_PROFILE = "ACTION_PROFILE"

    /** INPUT SIZE */
    const val LENGTH_MAX_FRIEND_CODE = 12
    const val LENGTH_MIN_NICKNAME = 6

    /** NOTIFICATIONS */
    const val REQUEST_TYPE_FRIEND = "friend"
    const val REQUEST_TYPE_GAME = "game"
    const val REQUEST_TYPE_ACCEPT_FRIEND = "friend_accept"
    const val REQUEST_TYPE_ACCEPT_GAME = "game_accept"

    /** INTENT GAME KEYS */
    const val INTENT_KEY_TABLE = "INTENT_KEY_TABLE"
    const val INTENT_KEY_RACKET = "INTENT_KEY_RACKET"
    const val INTENT_KEY_BALL = "INTENT_KEY_BALL"
}