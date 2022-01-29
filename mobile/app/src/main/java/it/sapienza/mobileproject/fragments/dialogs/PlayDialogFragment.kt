package it.sapienza.mobileproject.fragments.dialogs

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import com.android.volley.VolleyError
import com.google.gson.Gson
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.dto.PlayRequest
import it.sapienza.mobileproject.entities.dto.PlayResponse
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule

class PlayDialogFragment : WebDialogFragment() {

    private lateinit var findPlayTimerTask: TimerTask

    companion object {

        const val TAG = "PlayDialogFragment"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"

        fun newInstance(title: String, subTitle: String): PlayDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_SUBTITLE, subTitle)
            val fragment =
                PlayDialogFragment()
            fragment.arguments = args
            return fragment
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
        return inflater.inflate(R.layout.dialog_play, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
    }

    private fun findGame(retry: Boolean){
        Log.d(TAG, "findGame: "+(if(retry) "looking for a game" else "adding user  to lobby"))
        val request = Gson().toJson(PlayRequest(BuildConfig.DEBUG, null, true))
        if(retry) {
            postRequest(BuildConfig.GAME, request)
        }
        else{
            putRequest(BuildConfig.GAME, request)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        findGame(false)
    }

    override fun setupView(view: View) {
        //view.background.alpha = 15
        setTopBottomView(view,
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

    private fun deleteRequest(){
        // user is exiting dialog. game has been deleted
        if(this::findPlayTimerTask.isInitialized){ // delete scheduler
            findPlayTimerTask.cancel()
        }
        deleteRequest(BuildConfig.GAME, "{}")
    }

    override fun onResponseCallback(response: JSONObject) {
        super.onResponseCallback(response)
        val playResponse = Gson().fromJson<PlayResponse>(response.toString(), PlayResponse::class.java)
        if(playResponse.delete){
            // game has been delete, dismiss dialog
            dismiss()
        }
        else{
            if(playResponse.created) {
                // game has been created, start timertask
                findPlayTimerTask = Timer("SettingUp", false).schedule(5000, 5000) {
                    findGame(true)
                }
            }
            else{
                // TODO : MANAGE LOBBY
                if(playResponse.result && playResponse.info!=null){
                    pongViewModel.setGameInfo(playResponse.info!!)
                    dismiss()
                }
            }
        }
    }

    override fun onErrorCallback(error: VolleyError) {
        super.onErrorCallback(error)
    }

    override fun onDestroy() {
        Log.d(TAG, "destroyed")
        deleteRequest()
        super.onDestroy()
    }


}