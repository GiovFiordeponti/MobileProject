package it.sapienza.mobileproject.fragments

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import com.google.gson.Gson
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.adapter.FriendAdapter
import it.sapienza.mobileproject.adapter.NotificationAdapter
import it.sapienza.mobileproject.entities.dto.FriendRequest
import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification
import it.sapienza.mobileproject.fragments.dialogs.ImageDialogFragment
import it.sapienza.mobileproject.fragments.dialogs.NotificationDialogFragment
import it.sapienza.mobileproject.fragments.dialogs.ProfileFriendDialog
import it.sapienza.mobileproject.fragments.dialogs.RequestDialogFragment
import kotlinx.coroutines.launch
import org.json.JSONObject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendsFragment : WebFragment() {
    private lateinit var friendAdapter: FriendAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pongViewModel.selectedPongFriends.observe(this, Observer { backendFriends ->
            // first, update recycler view
            Log.d(TAG, "receiving friends ${Gson().toJson(backendFriends)}")
            this.updateFriends(backendFriends, this.requireView())
            //finally, update database
            roomViewModel.insertFriends(backendFriends)
        })
        pongViewModel.selectedUpdatedFriends.observe(this, Observer { _ ->
            roomViewModel.friends.observe(viewLifecycleOwner, androidx.lifecycle.Observer { friends ->
                updateFriends(friends, requireView())
            })
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friends, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set add friend button listener
        view.findViewById<Button>(R.id.button_add_friend).setOnClickListener { _ ->
            addFriend()
        }
        // set adapter
        friendAdapter = FriendAdapter(ArrayList<PongFriend>()){ friend, action -> onClickNotifications(friend, action)}
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.friendRecyclerView)
        val llm = LinearLayoutManager(requireContext())
        llm.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = llm
        recyclerView.adapter = friendAdapter
    }


    private fun onClickNotifications(friend: PongFriend, action: String){
        Log.d(TAG, "received from recycler view friend ${friend.nickname} with action $action")
        when(action){
            Constants.ACTION_PLAY ->
                postRequest(BuildConfig.REQUEST, Gson().toJson(FriendRequest("", friend)))
            Constants.ACTION_PROFILE ->
                ProfileFriendDialog.newInstance(
                    "",
                    "",
                    Gson().toJson(friend)
                ).show(requireActivity().supportFragmentManager, TAG)
        }
    }

    private fun updateFriends(friendList: List<PongFriend>, view: View){
        // update view
        Log.d(TAG, "updating friends list")
        if(friendList.isNotEmpty()) {
            // if user has friends, show recycler view
            val recyclerView = view.findViewById<RecyclerView>(R.id.friendRecyclerView)
            friendAdapter =
                FriendAdapter(friendList) { friend, action -> onClickNotifications(friend, action) }
            recyclerView.swapAdapter(friendAdapter, false)
            view.findViewById<NestedScrollView>(R.id.scroll_view_friends).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.textNoFriends).visibility = View.GONE
        }
        else{
            // else show friends list
            view.findViewById<TextView>(R.id.textNoFriends).visibility = View.VISIBLE
        }
        view.findViewById<ProgressBar>(R.id.progress).visibility = View.GONE

    }

    override fun onStart() {
        super.onStart()

        roomViewModel.friends.observe(viewLifecycleOwner, androidx.lifecycle.Observer { friends ->
            updateFriends(friends, requireView())
        })
    }

    private fun addFriend(){
        RequestDialogFragment.newInstance(
            getString(R.string.dialog_requests_send_title),
            getString(R.string.dialog_requests_send_subtitle)
        ).show(requireActivity().supportFragmentManager, TAG)
    }

    override fun onResponseCallback(response: JSONObject) {
        super.onResponseCallback(response)
        Toast.makeText(requireContext(), getString(R.string.request_game_sent), Toast.LENGTH_SHORT).show()
    }

    override fun onErrorCallback(error: VolleyError) {
        super.onErrorCallback(error)
        Toast.makeText(requireContext(), getString(R.string.request_game_error), Toast.LENGTH_SHORT).show()
    }


    companion object {
        private const val TAG = "FriendsFragment"
    }
}