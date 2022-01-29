package it.sapienza.mobileproject

import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import it.sapienza.mobileproject.entities.PongViewModel
import it.sapienza.mobileproject.entities.dto.PongUser
import it.sapienza.mobileproject.entities.dto.TokenRequest
import it.sapienza.mobileproject.entities.room.AppDatabase
import it.sapienza.mobileproject.entities.room.RoomViewModel
import it.sapienza.mobileproject.entities.room.RoomViewModelFactory
import it.sapienza.mobileproject.entities.room.entity.PongNotification
import it.sapienza.mobileproject.fragments.WebInterface
import it.sapienza.mobileproject.singletons.VolleySingleton
import org.json.JSONObject


class MainActivity : AppCompatActivity(), AppBarConfiguration.OnNavigateUpListener, WebInterface {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var drawerLayout: DrawerLayout

    private var user: FirebaseUser? = null

    private lateinit var notificationToken: String

    private lateinit var auth: FirebaseAuth

    private lateinit var navController: NavController

    private lateinit var volley: VolleySingleton

    private val pongViewModel: PongViewModel by viewModels()

    private lateinit var gameLauncher: ActivityResultLauncher<Intent>

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext /* Activity context */)
        // init game intent response
        gameLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d(TAG, "game ended")
                if (result.resultCode == Activity.RESULT_OK) {
                    // update user
                    sendNotificationToken()
                }
            }

        // Handle possible data accompanying notification message.
        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        // [START handle_data_extras]
        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.d(TAG, "intent extras: Key: $key Value: $value")
            }
        }
        // [END handle_data_extras]

        volley = this?.let { VolleySingleton.getInstance(it) }!!
        setContentView(R.layout.activity_main)
        // Initialize Firebase Auth
        auth = Firebase.auth
        // Check if user is signed in (non-null) and update UI accordingly.

        // Check if user is signed in (non-null) and update UI accordingly.
        user = auth.currentUser

        // start navcontroller and fragment
        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return
        navController = host.navController
        appBarConfiguration = AppBarConfiguration(navController.graph) //configure nav controller
        setupNavigation(navController) //setup navigation

        // hear for event changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val dest: String = try {
                resources.getResourceName(destination.id)
            } catch (e: Resources.NotFoundException) {
                destination.id.toString()
            }
            Log.d(TAG, "Navigated to $dest")
        }

        // user check
        if (user == null) {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        user = auth.currentUser
                        getAuthenticationToken()
                        pongViewModel.setUser(user!!)
                        Log.d(TAG, "signInAnonymously:success ${user.toString()}")

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInAnonymously:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else if (!user?.isAnonymous!!) {
            // user is not anonymous
            setupActionBar()
            pongViewModel.setUser(user!!)
        } else if (user?.isAnonymous!!) {
            pongViewModel.setUser(user!!)
        }

        // animateBackground()
        getAuthenticationToken()

        logUser()

        pongViewModel.gameInfoUpdated.observe(this, Observer { gameInfo ->
            val auth = Firebase.auth
            auth.signInWithCustomToken(gameInfo.token).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCustomToken:success")
                    user = auth.currentUser
                    pongViewModel.setUser(auth.currentUser!!)
                    val intent = Intent(this, GameActivity::class.java).apply {

                        val table = sharedPreferences.getString(getString(R.string.tables_key), "")
                        val ball = sharedPreferences.getString(getString(R.string.balls_key), "")
                        val racket = sharedPreferences.getString(getString(R.string.rackets_key), "")
                        Log.d(TAG, "shared pref table: $table ball: $ball racket: $racket" )
                        putExtra(AlarmClock.EXTRA_MESSAGE, gameInfo)
                        putExtra(Constants.INTENT_KEY_TABLE, table)
                        putExtra(Constants.INTENT_KEY_BALL, ball)
                        putExtra(Constants.INTENT_KEY_RACKET, racket)
                    }
                    gameLauncher.launch(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCustomToken:failure", task.exception)
                    Toast.makeText(
                        this, "Error while  launching game.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun animateBackground() {
        val backGround = findViewById<ImageView>(R.id.background)
        val backGroundTwo = findViewById<ImageView>(R.id.background_two)
        val animator: ValueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.duration = 5000L
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val width: Int = backGround.width
            val translationX = width * progress
            backGround.translationX = translationX
            backGroundTwo.translationX = translationX - width
        }
        animator.start()
    }

    private fun getAuthenticationToken() {
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            val newToken = task.result?.token!!
            Log.d(TAG, "get auth token. token is $newToken")
            volley.setToken(newToken)
            initNotifications()
        }
    }

    private fun setupNavigation(navController: NavController) {
        val sideNavView = findViewById<NavigationView>(R.id.nav_view)
        sideNavView?.setupWithNavController(navController)
        drawerLayout = findViewById(R.id.drawer_layout)
        //fragments load from here
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.friendsFragment,
                R.id.statsFragment,
                R.id.shopFragment,
                R.id.profileFragment
            ),
            drawerLayout
        )
    }

    private fun setupActionBar() {
        setupActionBarWithNavController(navController, appBarConfiguration)

        val settingsButton = findViewById<Button>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val currentDestination = navController.currentDestination?.id
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            if (currentDestination != R.id.settingsFragment) {
                navController.popBackStack(R.id.homeFragment, false)
                navController.navigate(R.id.action_homeFragment_to_settingsFragment)
            }
        }
        val logOutButton = findViewById<Button>(R.id.logout_button)
        logOutButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            navController.popBackStack(R.id.homeFragment, false)
            auth.signOut()
            recreate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val retValue = super.onCreateOptionsMenu(menu)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        if (navigationView == null) {
            //android needs to know what menu I need
            menuInflater.inflate(R.menu.menu, menu)
            return true
        }
        return retValue
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //I need to open the drawer onClick
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (item!!.itemId) {
            android.R.id.home ->
                drawerLayout.openDrawer(GravityCompat.START)
        }
        return item.onNavDestinationSelected(findNavController(R.id.nav_host_fragment))
                || super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun initNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        // [START subscribe_topics]
        Firebase.messaging.subscribeToTopic(getString(R.string.default_notification_channel_id))
            .addOnCompleteListener { task ->
                var msg = "subscribed to channel weather"
                if (!task.isSuccessful) {
                    msg = "error subscribing to channel weather"
                }
                Log.d(TAG, msg)
            }
        // [END subscribe_topics]
        // Get token
        // [START log_reg_token]
        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            notificationToken = task.result!!

            if (this::notificationToken.isInitialized) {
                // Log and toast
                Log.d(TAG, "firebaseMessaing: notification token $notificationToken")
                sendNotificationToken()
            }

        })
        // OPEN BROADCAST MANAGER TO RECEIVE NOTIFICATION
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter("intentKey"));

    }

    private fun sendNotificationToken() {
        Log.d(TAG, "sending notification token $notificationToken to backend")
        volley.volleyRequest(
            Request.Method.PUT,
            BuildConfig.USER,
            Gson().toJson(TokenRequest(notificationToken)),
            responseListener,
            errorListener
        )
    }

    // BROADCAST RECEIVER; TO RECEIVE DATA FROM WORKER LOCATED IN FCM PACKAGE
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Get extra data included in the Intent
            val message = intent.getStringExtra("key")
            Log.d(TAG, "broadcastReceiver: RECEIVED $message")
            val notification = Gson().fromJson<PongNotification>(message, PongNotification::class.java)
            when(notification.type){
                Constants.REQUEST_TYPE_GAME ->
                    pongViewModel.hasGameNotifications(true)
                Constants.REQUEST_TYPE_FRIEND ->
                    pongViewModel.hasFriendNotifications(true)
                Constants.REQUEST_TYPE_ACCEPT_FRIEND ->
                    pongViewModel.setUpdateFriends(true)

            }
        }
    }

    /** volley interface */
    private val responseListener = Response.Listener<JSONObject> { response ->
        onResponseCallback(response)
    }

    private val errorListener = Response.ErrorListener { error ->
        onErrorCallback(error)
    }

    fun reload(){
        user = auth.currentUser
        finish()
        startActivity(intent)
    }


    override fun onResponseCallback(response: JSONObject) {
        if(response.length()>0){
            // backend has returned a new user
            Log.d(TAG, "rest response ${response.toString()} ${response.length()>0}")
            val pongUser = Gson().fromJson<PongUser>(response.toString(), PongUser::class.java)
            pongViewModel.setPongUser(pongUser)
            if(pongUser.friendsDataList!=null) {
                pongViewModel.setPongFriends(pongUser.friendsDataList)
            }
            if(pongUser.statistics!=null){
                pongViewModel.setStatistics(stats = pongUser.statistics)
            }
            user?.reload()!!.addOnCompleteListener { _ ->
                logUser()
            }
        }
    }

    private fun logUser(){
        Log.d(
            TAG,
            "firebaseUser: \n-id:${user?.uid}\n-displayName:${user?.displayName}\n-picture:${user?.photoUrl}\n-anonymous: ${user?.isAnonymous}"
        )
    }

    override fun onErrorCallback(error: VolleyError) {
        Log.d(TAG, "rest error ${Gson().toJson(error)}")
    }

    companion object {
        private const val TAG = "MainActivity"
    }

}