package it.sapienza.mobileproject.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.PongViewModel
import it.sapienza.mobileproject.entities.dto.PongUser
import it.sapienza.mobileproject.formatFriendNumber
import it.sapienza.mobileproject.fragments.dialogs.ImageDialogFragment


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val pongViewModel: PongViewModel by activityViewModels()
    private var currentUser: FirebaseUser? = null
    private lateinit var currentPongUser: PongUser

    private lateinit var picasso: Picasso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        val storage = FirebaseStorage.getInstance()

        pongViewModel.friendSelectedUser.observe(this, Observer { user ->
            // Perform an action with the latest item data
            Log.d(TAG, "Received user ${user.email}")
            currentUser = user
            setFirebaseInfo(requireView())
            val pathReference =
                storage.reference.child("${currentUser!!.uid}/profile_picture.png")
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

        })

        pongViewModel.selectedPongUser.observe(this, Observer { pongUser ->
            Log.d(TAG, "Received pong user ${Gson().toJson(pongUser)}")
            currentPongUser = pongUser
            setPongInfo(requireView())
        })

        pongViewModel.selectedPictureUpdated.observe(this, Observer { pictureUpdated ->
            Log.d(TAG, "Received updated picture with uri $pictureUpdated")
            Toast.makeText(requireContext(),
                getString(if(pictureUpdated!=null) R.string.toast_picture_updated else R.string.toast_picture_error),
                Toast.LENGTH_SHORT).show()
            if (this::picasso.isInitialized && pictureUpdated!=null) {
                picasso.load(pictureUpdated)
                    .into(requireView().findViewById<ImageView>(R.id.profile_image))
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    private fun setFirebaseInfo(view: View) {
        val nickname = view.findViewById<TextView>(R.id.text_profile_nickname)
        val email = view.findViewById<TextView>(R.id.text_profile_email)
        nickname.text = if (currentUser!!.displayName.isNullOrBlank()) ""
        else currentUser!!.displayName!!
        email.text = currentUser!!.email!!
    }

    private fun setProfileInfo(view: View, imageUri: String) {

        picasso.load(imageUri)
            .into(view.findViewById<ImageView>(R.id.profile_image))

        // callback to change image
        view.findViewById<Button>(R.id.button_change_photo).setOnClickListener {
            ImageDialogFragment.newInstance(
                getString(R.string.dialog_camera_title),
                getString(R.string.dialog_camera_subtitle),
                currentUser!!.uid
            ).show(requireActivity().supportFragmentManager, TAG)
        }
    }

    private fun setPongInfo(view: View) {
        val friendCode = view.findViewById<TextView>(R.id.text_friend_code)
        friendCode.text = formatFriendNumber(currentPongUser.friendCode)
        // listeners
        val shareButton = view.findViewById<Button>(R.id.share_button)
        shareButton.setOnClickListener { _ ->
            inviteFriends()
        }
    }

    private fun inviteFriends() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_code_intent) + " " + currentPongUser.friendCode
            )
            type = "text/plain"
        }
        // Always use string resources for UI text.
        // This says something like "Share this photo with"
        val title = resources.getString(R.string.intent_profile_title)
        // Create intent to show chooser
        val chooser = Intent.createChooser(sendIntent, title)

        // Verify the intent will resolve to at least one activity
        if (sendIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(chooser)
        }
    }

    companion object {
        private const val TAG = "ProfileFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}