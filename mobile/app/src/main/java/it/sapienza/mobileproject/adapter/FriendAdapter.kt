package it.sapienza.mobileproject.adapter

import android.media.Image
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
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification
import it.sapienza.mobileproject.fragments.ProfileFragment

class FriendAdapter(private val dataSet: List<PongFriend>, private val onClick: (PongFriend, String) -> Unit) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    private val storage = FirebaseStorage.getInstance()
    private val picasso: Picasso = Picasso.get()
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.text_friend_name)
        val profilePic: ImageView = view.findViewById(R.id.image_friend_profile)
        val playButton: Button = view.findViewById<Button>(R.id.button_friend_play)
        val profileButton: Button = view.findViewById<Button>(R.id.button_friend_profile)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.adapter_friends, viewGroup, false)


        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        // first: set profile pic
        val pathReference =
            storage.reference.child("${dataSet[position].id}/profile_picture.png")
        pathReference.downloadUrl.addOnCompleteListener {
            if (BuildConfig.DEBUG) {
                picasso.isLoggingEnabled = true
            }
            Log.d(TAG, "firebaseStorage: received ${it.result.toString()}")
            picasso.load(it.result)
                .into(viewHolder.profilePic)
        }
        // update user name
        viewHolder.textView.text = dataSet[position].nickname
        // button listeners
        viewHolder.playButton.setOnClickListener { _ ->
            onClick(dataSet[position], Constants.ACTION_PLAY)
        }
        viewHolder.profileButton.setOnClickListener { _ ->
            onClick(dataSet[position], Constants.ACTION_PROFILE)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    companion object{
        private const val TAG = "FriendAdapter"
    }
}
