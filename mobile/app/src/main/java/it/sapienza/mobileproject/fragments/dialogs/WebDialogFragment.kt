package it.sapienza.mobileproject.fragments.dialogs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.gson.Gson
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.PongViewModel
import it.sapienza.mobileproject.entities.room.AppDatabase
import it.sapienza.mobileproject.entities.room.RoomViewModel
import it.sapienza.mobileproject.entities.room.RoomViewModelFactory
import it.sapienza.mobileproject.fragments.WebInterface
import it.sapienza.mobileproject.singletons.VolleySingleton
import org.json.JSONObject

open abstract class WebDialogFragment : DialogFragment(),
    WebInterface {

    private lateinit var volley: VolleySingleton

    protected val pongViewModel: PongViewModel by activityViewModels()

    protected lateinit var roomViewModel: RoomViewModel

    protected lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        val roomViewModelFactory = RoomViewModelFactory(database.pongNotificationDao(), requireActivity().application)
        roomViewModel = ViewModelProvider(
            this, roomViewModelFactory).get(RoomViewModel::class.java)
    }
    override fun onStart() {
        super.onStart()
        volley = context?.let { VolleySingleton.getInstance(it) }!!
    }

    private val responseListener = Response.Listener<JSONObject> { response ->
        onResponseCallback(response)
    }

    private val errorListener = Response.ErrorListener { error ->
        // return  false
        onErrorCallback(error)
    }

    protected fun postRequest(path: String, jsonRequest: String){
        request(Request.Method.POST,
            path,
            jsonRequest
        )
    }

    protected fun putRequest(path: String, jsonRequest: String){
        request(Request.Method.PUT,
            path,
            jsonRequest
        )
    }

    protected fun deleteRequest(path: String, jsonRequest: String){
        request(Request.Method.DELETE,
            path,
            jsonRequest
        )
    }

    protected fun getRequest(path: String, jsonRequest: String){
        request(Request.Method.GET,
            path,
            jsonRequest
        )
    }

    private fun request(method: Int, path: String, jsonRequest: String){
        volley.volleyRequest(method,
            path,
            jsonRequest,
            responseListener,
            errorListener
        )
    }

    override fun onResponseCallback(response: JSONObject) {
        Log.d(TAG, "received ${response.toString()}")
    }

    override fun onErrorCallback(error: VolleyError) {
        Log.d(TAG, "received error ${Gson().toJson(error)}")
    }

    protected abstract fun setupView(view: View)

    protected fun setTopBottomView(view: View, title: String, subtitle: String){
        view.findViewById<TextView>(R.id.dialog_title).text = arguments?.getString(
            title
        )
        view.findViewById<TextView>(R.id.dialog_subtitle).text = arguments?.getString(
            subtitle
        )
        setupClickListeners(view)
    }

    protected abstract fun setupClickListeners(view: View)

    companion object{
        private const val TAG = "WebDialogFragment"

    }
}