package it.sapienza.mobileproject.fragments.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import com.google.gson.Gson
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.adapter.GameFriendAdapter
import it.sapienza.mobileproject.entities.dto.NotificationResponse
import it.sapienza.mobileproject.entities.dto.SimpleResponse
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.room.entity.PongNotification
import org.json.JSONObject


class PlayFriendsDialogFragment : WebDialogFragment() {

    companion object {

        const val TAG = "PlayFriendsDialFragm"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"

        private lateinit var gameFriendAdapter: GameFriendAdapter

        private var selectedNotification: PongNotification? = null

        fun newInstance(title: String, subTitle: String): PlayFriendsDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_SUBTITLE, subTitle)
            val fragment =
                PlayFriendsDialogFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /** The system calls this to get the DialogFragment's layout, regardless
    of whether it's being displayed as a dialog or an embedded fragment. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.dialog_play_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameFriendAdapter =
            GameFriendAdapter(ArrayList<PongNotification>()) { notification ->
                onClickNotifications(
                    notification
                )
            }
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.playFriendsRecyclerView)
        val llm = LinearLayoutManager(requireContext())
        llm.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = llm
        recyclerView.adapter = gameFriendAdapter
        setupView(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        init()
    }

    private fun init() {
        roomViewModel.acceptedGameNotifications.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { notifications ->
                // get element from db
                Log.d(TAG, "there are ${notifications.size} friend notifications")
                setNotificationView(requireView(), notifications)
            })
    }

    private fun setNotificationView(view: View, notifications: List<PongNotification>) {
        val gameCount = view.findViewById<TextView>(R.id.dialog_subtitle)
        gameCount.text =
            String.format(getString(R.string.dialog_play_friends_subtitle), notifications.size)
        // update view
        val recyclerView = view.findViewById<RecyclerView>(R.id.playFriendsRecyclerView)
        gameFriendAdapter = GameFriendAdapter(notifications) { notification ->
            onClickNotifications(
                notification
            )
        }
        recyclerView.swapAdapter(gameFriendAdapter, false)
        view.findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
        view.findViewById<NestedScrollView>(R.id.scroll_view_play_friends).visibility = View.VISIBLE
    }

    private fun onClickNotifications(notification: PongNotification) {
        Log.d(TAG, "play friend fragment callback with game info ${Gson().toJson(notification.gameInfo!!)}")
        pongViewModel.setGameInfo(notification.gameInfo!!)
        roomViewModel.deleteNotification(notification)
        dismiss()

    }

    override fun setupView(view: View) {
        setTopBottomView(
            view,
            KEY_TITLE,
            KEY_SUBTITLE
        )
        view.findViewById<Button>(R.id.btnPositive).visibility = View.GONE
    }

    override fun setupClickListeners(view: View) {
        view.findViewById<Button>(R.id.btnNegative).setOnClickListener {
            dismiss()//deleteRequest()//deleteRequest()
        }
    }

    override fun onResponseCallback(response: JSONObject) {
        super.onResponseCallback(response)
        val simpleResponse = Gson().fromJson(response.toString(), SimpleResponse::class.java)
        if (simpleResponse.result) {
            // delete notification from db and update view
            roomViewModel.deleteNotification(selectedNotification!!).apply {
                init()
            }
        } else {
            // show error toast
            showErrorToast()
        }
        selectedNotification = null
    }

    override fun onErrorCallback(error: VolleyError) {
        super.onErrorCallback(error)
        selectedNotification = null
        showErrorToast()
    }

    private fun showErrorToast() {
        Toast.makeText(
            requireContext(),
            getString(R.string.error_request_accept),
            Toast.LENGTH_SHORT
        )
            .show()
    }

}