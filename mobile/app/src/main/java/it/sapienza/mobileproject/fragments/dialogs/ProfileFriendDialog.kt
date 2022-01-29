package it.sapienza.mobileproject.fragments.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.adapter.NotificationAdapter
import it.sapienza.mobileproject.entities.dto.NotificationResponse
import it.sapienza.mobileproject.entities.dto.SimpleResponse
import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification
import it.sapienza.mobileproject.formatFriendNumber
import it.sapienza.mobileproject.fragments.ProfileFragment
import org.json.JSONObject


class ProfileFriendDialog : WebDialogFragment()  {

    private lateinit var friend : PongFriend

    private lateinit var picasso: Picasso
    private lateinit var storage: FirebaseStorage

    companion object {

        const val TAG = "NotifyDialogFragment"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"
        private const val KEY_FRIEND_USER = "KEY_FRIEND_USER"

        fun newInstance(title: String, subTitle: String, userJson: String): ProfileFriendDialog {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_SUBTITLE, subTitle)
            args.putString(KEY_FRIEND_USER, userJson)
            val fragment =
                ProfileFriendDialog()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init storage and picasso
        storage = FirebaseStorage.getInstance()
        picasso = Picasso
            .Builder(requireContext())
            .build()

        friend = Gson().fromJson(arguments?.getString(KEY_FRIEND_USER), PongFriend::class.java)

        val pathReference =
            storage.reference.child("${friend!!.id}/profile_picture.png")
        pathReference.downloadUrl.addOnCompleteListener {
            picasso = Picasso
                .Builder(requireContext())
                .build()

            if (BuildConfig.DEBUG) {
                picasso.isLoggingEnabled = true
            }
            Log.d(TAG, "firebaseStorage: received ${it.result.toString()}")
            setProfileInfo(requireView(), it.result.toString())
        }
    }

    /** The system calls this to get the DialogFragment's layout, regardless
    of whether it's being displayed as a dialog or an embedded fragment. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.dialog_friend_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun setupView(view: View) {
        setTopBottomView(view,
            KEY_TITLE,
            KEY_SUBTITLE
        )
        view.findViewById<Button>(R.id.btnPositive).visibility = View.GONE
        view.findViewById<Button>(R.id.btnNegative).text = getString(R.string.close)
    }

    override fun setupClickListeners(view: View) {
        view.findViewById<Button>(R.id.btnNegative).setOnClickListener {
            dismiss()//deleteRequest()//deleteRequest()
        }
    }

    override fun onResponseCallback(response: JSONObject) {
        super.onResponseCallback(response)
    }

    override fun onErrorCallback(error: VolleyError) {
        super.onErrorCallback(error)
        showErrorToast()
    }

    private fun setProfileInfo(view: View, imageUri: String) {
        picasso.load(imageUri)
            .into(view.findViewById<ImageView>(R.id.profile_image))
        val nickname = view.findViewById<TextView>(R.id.text_profile_nickname)
        val email = view.findViewById<TextView>(R.id.text_profile_email)
        nickname.text = friend.nickname
        email.text = friend.email
        view.findViewById<TextView>(R.id.text_friend_code).text = formatFriendNumber(friend.friendCode)
        view.findViewById<TextView>(R.id.win_numbers).text = friend.statistics.win.toString()
        view.findViewById<TextView>(R.id.lose_numbers).text = friend.statistics.losses.toString()

    }

    private fun showErrorToast(){
        Toast.makeText(requireContext(), getString(R.string.error_request_accept), Toast.LENGTH_SHORT).show()
    }

}