package it.sapienza.mobileproject.fragments.dialogs

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.android.volley.VolleyError
import com.google.gson.Gson
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.PongViewModel
import it.sapienza.mobileproject.entities.dto.NicknameRequest
import it.sapienza.mobileproject.entities.dto.NicknameResponse
import org.json.JSONObject

class NicknameDialogFragment : WebDialogFragment() {

    private val viewModel: PongViewModel by activityViewModels()

    companion object {

        const val TAG = "PongDialogFragment"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"

        fun newInstance(title: String, subTitle: String): NicknameDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_SUBTITLE, subTitle)
            val fragment =
                NicknameDialogFragment()
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
        return inflater.inflate(R.layout.dialog_nickname, container, false)
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
        setTopBottomView(view,
            KEY_TITLE,
            KEY_SUBTITLE
        )
        view.findViewById<Button>(R.id.btnPositive).isEnabled = false
        view.findViewById<EditText>(R.id.username).addTextChangedListener { nickname ->
            Log.d(TAG, "text is $nickname")
            if (nickname!!.length >= 6) {
                postRequest(
                    BuildConfig.USER_CHECK,
                    Gson().toJson(NicknameRequest(nickname.toString(), true))
                )
            } else {
                view.findViewById<Button>(R.id.btnPositive).isEnabled = false
            }
        }

    }

    override fun setupClickListeners(view: View) {
        view.findViewById<Button>(R.id.btnPositive).setOnClickListener {
            val nickname = view.findViewById<EditText>(R.id.username).text.toString()
            postRequest(
                BuildConfig.USER_CHECK,
                Gson().toJson(NicknameRequest(nickname.toString(), false))
            )
        }
        view.findViewById<Button>(R.id.btnNegative).setOnClickListener {
            viewModel.setNickNameUpdated(false)
            dismiss()
        }
    }

    override fun onResponseCallback(response: JSONObject) {
        super.onResponseCallback(response)
        val responseObj =
            Gson().fromJson<NicknameResponse>(response.toString(), NicknameResponse::class.java)
        if (responseObj.check) {
            requireView().findViewById<Button>(R.id.btnPositive).isEnabled = responseObj.result
        } else {
            if (responseObj.result) {
                viewModel.setNickNameUpdated(responseObj.result)
                dismiss()
            }
        }
    }

    override fun onErrorCallback(error: VolleyError) {
        super.onErrorCallback(error)
    }


}