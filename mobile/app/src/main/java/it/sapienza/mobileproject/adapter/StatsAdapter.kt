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

class StatsAdapter(private val dataSet: List<PongFriend>) :
    RecyclerView.Adapter<StatsAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.text_player_rank)
        val userName: TextView = view.findViewById(R.id.text_player_stats)
        val wins: TextView = view.findViewById(R.id.text_win_stats)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.adapter_stats, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // update user name
        viewHolder.rank.text = "${(position + 1)}. "
        viewHolder.userName.text = dataSet[position].nickname
        viewHolder.wins.text = dataSet[position].statistics.win.toString()
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    companion object{
        private const val TAG = "StatsAdapter"
    }
}
