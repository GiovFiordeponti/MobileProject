package it.sapienza.mobileproject.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.gson.Gson
import it.sapienza.mobileproject.*
import it.sapienza.mobileproject.fragments.dialogs.NicknameDialogFragment
import it.sapienza.mobileproject.fragments.dialogs.NotificationDialogFragment
import it.sapienza.mobileproject.fragments.dialogs.PlayDialogFragment
import it.sapienza.mobileproject.fragments.dialogs.PlayFriendsDialogFragment


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : WebFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var authLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init auth intent response
        authLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // There are no request codes
                    val data: Intent? = result.data
                    val isLogged = data?.getBooleanExtra(Constants.AUTH_MESSAGE, false)
                    if (isLogged!!) {
                        //(activity as MainActivity).setupActionBar()
                        (activity as MainActivity).reload()
                        // setAuthUI(requireView())
                    }
                }
            }


        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        pongViewModel.selectedUser.observe(this, Observer { user ->
            // Perform an action with the latest item data
            Log.d(TAG, "Received user ${user.email}")
            currentUser = user
            if (!user.isAnonymous!!) {
                // if user is logged (i.e. not anonymous), update ui
                setAuthUI(requireView())
            } else {
                setHomeUI(requireView())
            }
        })

        pongViewModel.homeNicknameUpdated.observe(this, Observer { updated ->
            // Perform an action with the latest item data
            Log.d(TAG, "Nickname updated: $updated")
            if (updated) {
                currentUser?.reload()!!.addOnCompleteListener { _ ->
                    Log.d(TAG, "now user is: ${currentUser!!.displayName}")
                    Toast.makeText(
                        requireActivity().applicationContext,
                        getString(R.string.msg_username_updated) + " ${currentUser!!.displayName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        pongViewModel.homeFriendNotifications.observe(this, Observer { update ->
            if (update) {
                setNotificationCount()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        if (currentUser != null && !currentUser?.isAnonymous!!) {
            // if user is logged (set auth ui)
            setAuthUI(view)
        } else {
            // otherwise set normal ui
            setHomeUI(view)
        }
        return view
    }

    private fun setHomeUI(view: View) {
        val simpleLayout = view.findViewById<LinearLayout>(R.id.simple_layout)
        val authLayout = view.findViewById<LinearLayout>(R.id.auth_layout)
        authLayout.visibility = View.GONE
        simpleLayout.visibility = View.VISIBLE
        val logButton = view.findViewById<Button>(R.id.login_fragment_button)
        logButton.setOnClickListener {
            goToAuthActivity(it)
        }
        val signButton = view.findViewById<Button>(R.id.register_fragment_button)
        signButton.setOnClickListener {
            goToAuthActivity(it)
        }
        val playButton = view.findViewById<Button>(R.id.play_button)
        playButton.setOnClickListener {
            play(false)
        }
        val notificationButton = view.findViewById<Button>(R.id.button_notification)
        notificationButton.setOnClickListener {
            openNotificationDialog(false)
        }
        val requestsButton = view.findViewById<Button>(R.id.button_requests)
        requestsButton.setOnClickListener {
            openNotificationDialog(true)
        }
    }

    private fun setAuthUI(view: View) {
        val simpleLayout = view.findViewById<LinearLayout>(R.id.simple_layout)
        val authLayout = view.findViewById<LinearLayout>(R.id.auth_layout)
        val welcomeText = view.findViewById<TextView>(R.id.welcome_back)
        simpleLayout.visibility = View.GONE
        val sb = StringBuilder()
        val displayName = if (currentUser?.displayName != null) {
            currentUser?.displayName
        } else currentUser?.email
        sb.append(welcomeText.text).append(" ").append(displayName)
        authLayout.visibility = View.VISIBLE
        val inviteButton = view.findViewById<Button>(R.id.button_invite_friends)
        inviteButton.setOnClickListener { _ ->
            inviteFriends()
        }
        val playButton = view.findViewById<Button>(R.id.play_button_random)
        playButton.setOnClickListener { _ ->
            play(false)
        }
        val playButtonFriends = view.findViewById<Button>(R.id.play_friends_button)
        playButtonFriends.setOnClickListener { _ ->
            play(true)
        }
        setNotificationCount()
    }

    private fun goToAuthActivity(view: View) {
        var fragmentAction = 0
        when (view.id) {
            R.id.register_fragment_button -> {
                fragmentAction = R.id.action_loginFragment_to_registerFragment
            }
        }

        val intent = Intent(activity, AuthActivity::class.java).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, fragmentAction)
        }
        authLauncher.launch(intent)
    }

    private fun play(withFriends: Boolean) {
        if (currentUser?.displayName != null
            && currentUser?.displayName!!.length >= Constants.LENGTH_MIN_NICKNAME) {
            //val intent = Intent(activity, DemoActivity::class.java).apply {
            //    putExtra(AlarmClock.EXTRA_MESSAGE, "login")
            //}
            //startActivity(intent)
            Log.d(TAG, "opening play dialog")
            if (withFriends) {
                PlayFriendsDialogFragment.newInstance(
                    getString(R.string.dialog_play_friends_title),
                    ""
                ).show(requireActivity().supportFragmentManager, TAG)
            } else {
                PlayDialogFragment.newInstance(
                    getString(R.string.dialog_play_title),
                    getString(R.string.dialog_play_subtitle)
                ).show(requireActivity().supportFragmentManager, TAG)
            }
        } else {
            Log.d(TAG, "opening fragment dialog")
            NicknameDialogFragment.newInstance(
                getString(R.string.dialog_nickname_title),
                getString(R.string.dialog_nickname_subtitle)
            ).show(requireActivity().supportFragmentManager, "dialog")
        }

    }

    private fun inviteFriends() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.invite_friends_intent) + " " + getString(R.string.app_url)
            )
            type = "text/plain"
        }
        // Always use string resources for UI text.
        // This says something like "Share this photo with"
        val title = resources.getString(R.string.chooser_title)
        // Create intent to show chooser
        val chooser = Intent.createChooser(sendIntent, title)

        // Verify the intent will resolve to at least one activity
        if (sendIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(chooser)
        }

    }

    private fun setNotificationCount() {
        roomViewModel.friendNotifications.observe(viewLifecycleOwner, Observer { notifications ->
            Log.d(TAG, "there are ${notifications.size} friend notifications")
            val friendsButton = requireView().findViewById<Button>(R.id.button_requests)
            friendsButton.text = notifications.size.toString()
        })

        roomViewModel.gameNotifications.observe(viewLifecycleOwner, Observer { notifications ->
            Log.d(TAG, "there are ${notifications.size} game notifications")
            val notificationButton = requireView().findViewById<Button>(R.id.button_notification)
            notificationButton.text = notifications.size.toString()
        })
    }

    private fun openNotificationDialog(isRequest: Boolean) {
        Log.d(TAG, "opening ${if (isRequest) "friend" else "game"} notification dialog")
        NotificationDialogFragment.newInstance(
            if (isRequest) getString(R.string.dialog_requests_title) else getString(R.string.dialog_game_title),
            getString(R.string.dialog_requests_subtitle),
            isRequest
        ).show(requireActivity().supportFragmentManager, TAG)
    }


    companion object {
        private const val TAG = "HomeFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}