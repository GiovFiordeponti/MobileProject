package it.sapienza.mobileproject.fragments.dialogs

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.PongViewModel
import it.sapienza.mobileproject.entities.dto.FriendRequest
import it.sapienza.mobileproject.entities.dto.NicknameRequest
import it.sapienza.mobileproject.entities.dto.NicknameResponse
import it.sapienza.mobileproject.entities.dto.SimpleResponse
import org.json.JSONObject
import java.net.HttpURLConnection

class RequestDialogFragment : WebDialogFragment() {

    private val viewModel: PongViewModel by activityViewModels()

    companion object {

        const val TAG = "PongDialogFragment"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"

        fun newInstance(title: String, subTitle: String): RequestDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_SUBTITLE, subTitle)
            val fragment =
                RequestDialogFragment()
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
        return inflater.inflate(R.layout.dialog_request, container, false)
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
        //view.background.alpha = 15
        setTopBottomView(
            view,
            KEY_TITLE,
            KEY_SUBTITLE
        )
        view.findViewById<Button>(R.id.btnPositive).isEnabled = false
        view.findViewById<EditText>(R.id.edit_text_friend_code).addTextChangedListener { nickname ->
            // update button state depending on text size
            view.findViewById<Button>(R.id.btnPositive)
                .isEnabled = nickname!!.length == Constants.LENGTH_MAX_FRIEND_CODE
        }

    }

    override fun setupClickListeners(view: View) {
        view.findViewById<Button>(R.id.btnPositive).setOnClickListener {
            val friendCode = view.findViewById<EditText>(R.id.edit_text_friend_code).text.toString()
            if (friendCode.length == Constants.LENGTH_MAX_FRIEND_CODE) {
                postRequest(
                    BuildConfig.REQUEST,
                    Gson().toJson(
                        FriendRequest(friendCode, null)
                    )
                )
            }
        }
        view.findViewById<Button>(R.id.btnNegative).setOnClickListener {
            dismiss()
        }
    }

    override fun onResponseCallback(response: JSONObject) {
        super.onResponseCallback(response)
        val responseObj =
            Gson().fromJson<SimpleResponse>(response.toString(), SimpleResponse::class.java)
        if (responseObj.result) {
            Toast.makeText(requireContext(), getString(R.string.request_sent), Toast.LENGTH_SHORT).show()
            dismiss()
        } else {
            Toast.makeText(requireContext(), getString(R.string.error_request), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onErrorCallback(error: VolleyError) {
        super.onErrorCallback(error)

        when(error.networkResponse.statusCode){
            HttpURLConnection.HTTP_NOT_FOUND -> {
                // friend code not found
                requireView().findViewById<EditText>(R.id.edit_text_friend_code).error = getString(R.string.error_request_not_found)
            }
            HttpURLConnection.HTTP_CONFLICT -> {
                // friend code already sent
                requireView().findViewById<EditText>(R.id.edit_text_friend_code).error = getString(R.string.error_request_conflict)
            }
        }
    }


}