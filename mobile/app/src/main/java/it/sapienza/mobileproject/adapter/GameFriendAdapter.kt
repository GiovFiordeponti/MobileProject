package it.sapienza.mobileproject.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.room.entity.PongNotification

class GameFriendAdapter(
    private val dataSet: List<PongNotification>,
    private val onClick: (PongNotification) -> Unit
) :
    RecyclerView.Adapter<GameFriendAdapter.ViewHolder>() {

    private val storage = FirebaseStorage.getInstance()
    private val picasso: Picasso = Picasso.get()

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textNotificationInfo)
        val profilePic: ImageView = view.findViewById(R.id.image_friend_profile)
        val playButton: Button = view.findViewById<Button>(R.id.button_accept_play)

        init {
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.adapter_play_friends, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // user picture
        val pathReference =
            storage.reference.child("${dataSet[position].user!!.id}/profile_picture.png")
        pathReference.downloadUrl.addOnCompleteListener {
            if (BuildConfig.DEBUG) {
                picasso.isLoggingEnabled = true
            }
            Log.d(TAG, "firebaseStorage: received ${it.result.toString()}")
            picasso.load(it.result)
                .into(viewHolder.profilePic)
        }

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.textView.text = dataSet[position].user!!.nickname
        // click listeners
        viewHolder.playButton.setOnClickListener { _ ->
            onClick(dataSet[position])
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    companion object {
        private const val TAG = "NotificationAdapter"
    }
}
