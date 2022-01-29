package it.sapienza.mobileproject

import android.content.Context
import android.os.Bundle
import android.provider.AlarmClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        val fragmentId = intent.getIntExtra(AlarmClock.EXTRA_MESSAGE, 0)

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.auth_host_fragment) as NavHostFragment? ?: return
        val navController = host.navController
        AppBarConfiguration(navController.graph) //configure nav controller
        if(fragmentId!=0) {
            navController.navigate(fragmentId)
        }
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    companion object{
        private const val TAG = "AuthActivity"
    }
}