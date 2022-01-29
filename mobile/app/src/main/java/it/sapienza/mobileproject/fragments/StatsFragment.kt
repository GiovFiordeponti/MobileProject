package it.sapienza.mobileproject.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.adapter.FriendAdapter
import it.sapienza.mobileproject.adapter.StatsAdapter
import it.sapienza.mobileproject.entities.dto.PongStatistics
import it.sapienza.mobileproject.entities.room.entity.PongFriend

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [StatsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StatsFragment : WebFragment() {

    private lateinit var statsAdapter: StatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pongViewModel.selectedStatistics.observe(this, Observer {
            setStatistics(it, requireView())
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set adapter
        statsAdapter = StatsAdapter(ArrayList<PongFriend>())
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.statsRecyclerView)
        val llm = LinearLayoutManager(requireContext())
        llm.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = llm
        recyclerView.adapter = statsAdapter
        roomViewModel.leaderBoardFriends.observe(viewLifecycleOwner, androidx.lifecycle.Observer { friends ->
            updateFriends(friends, view)
        })
    }

    private fun setStatistics(stats: PongStatistics, view: View) {
        view.findViewById<TextView>(R.id.win_numbers).text = stats.win.toString()
        view.findViewById<TextView>(R.id.lose_numbers).text = stats.losses.toString()
    }

    private fun updateFriends(friendList: List<PongFriend>, view: View){
        // update view
        Log.d(TAG, "updating stats list with friends ${Gson().toJson(friendList)}")
        if(friendList.isNotEmpty()) {
            // if user has friends, show recycler view
            val recyclerView = view.findViewById<RecyclerView>(R.id.statsRecyclerView)
            statsAdapter = StatsAdapter(friendList)
            recyclerView.swapAdapter(statsAdapter, false)
            view.findViewById<NestedScrollView>(R.id.scroll_view_friends).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.textNoFriends).visibility = View.GONE
        }
        else{
            // else show friends list
            view.findViewById<TextView>(R.id.textNoFriends).visibility = View.VISIBLE
        }
        view.findViewById<ProgressBar>(R.id.progress).visibility = View.GONE

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment StatsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StatsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        private const val TAG = "StatsFragment"
    }
}