package it.sapienza.mobileproject.fragments.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.R


class RegisterFragment : Fragment() {

    private lateinit var authViewModel: AuthViewModel

    private lateinit var auth: FirebaseAuth

    private lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title=getString(R.string.sign_up)
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this.requireActivity(), gso)

        authViewModel = ViewModelProvider(this,
            AuthViewModelFactory()
        )
            .get(AuthViewModel::class.java)

        val usernameEditText = view.findViewById<EditText>(R.id.username)
        val passwordEditText = view.findViewById<EditText>(R.id.password)
        val passwordConfirmText = view.findViewById<EditText>(R.id.password_confirm)
        val termsCheckBox: CheckBox = view.findViewById<CheckBox>(R.id.terms_check)
        val termsTextView: TextView = view.findViewById<TextView>(R.id.terms_check_text)
        setTermsText(termsTextView)
        val registerButton = view.findViewById<Button>(R.id.register_button)
        val googleRegisterButton = view.findViewById<Button>(R.id.register_google_button)
        val loadingProgressBar = view.findViewById<ProgressBar>(R.id.loading)

        authViewModel.authFormState.observe(viewLifecycleOwner,
            Observer { authFormState ->
                if (authFormState == null) {
                    return@Observer
                }
                registerButton.isEnabled = authFormState.isDataValid
                authFormState.usernameError?.let {
                    usernameEditText.error = getString(it)
                }
                authFormState.passwordError?.let {
                    passwordEditText.error = getString(it)
                }
                authFormState.passwordConfirmError?.let {
                    passwordConfirmText.error = getString(it)
                }
            })

        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                authViewModel.registerDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    passwordConfirmText.text.toString(),
                    termsCheckBox.isChecked
                )
            }
        }
        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)
        passwordConfirmText.addTextChangedListener(afterTextChangedListener)
        termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            authViewModel.registerDataChanged(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                passwordConfirmText.text.toString(),
                isChecked
            )
        }

        registerButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            register(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                null
            )
        }

        googleRegisterButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            googleSignIn()
        }
    }

    private fun register(email: String?, password: String?, idToken: String?) {

        var credential = if(idToken!=null){
            GoogleAuthProvider.getCredential(idToken, null)
        } else{
            EmailAuthProvider.getCredential(email!!, password!!)
        }
        Log.d(TAG, "registering with credential ${Gson().toJson(credential)} to current user ${Gson().toJson(auth.currentUser)}")
        auth.currentUser!!.linkWithCredential(credential).addOnCompleteListener(this.requireActivity()) { task ->
                val loadingProgressBar = view?.findViewById<ProgressBar>(R.id.loading)
                loadingProgressBar?.visibility = View.GONE
                if (task.isSuccessful) {
                    Log.d(TAG, "linkWithCredential:success")
                    val user = auth.currentUser
                    val intent = Intent()
                    intent.putExtra(Constants.AUTH_MESSAGE, true)
                    activity?.setResult(Activity.RESULT_OK, intent)
                    activity?.finish()
                } else {
                    Log.w(TAG, "linkWithCredential:failure", task.exception)
                    showLoginFailed(task.exception?.localizedMessage)
                }
            }
    }

    private fun showLoginFailed(errorString: String?) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    private fun setTermsText(termsTextView: TextView) {
        val click: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Log.d("prova", "terms agreement")
            }
        }
        val termsLink = getString(R.string.terms_link)
        val termsText = getString(R.string.terms_text, termsLink)
        val spannableStringBuilder = SpannableStringBuilder(termsText)
        spannableStringBuilder.setSpan(
            click,
            termsText.indexOf(termsLink),
            termsText.indexOf(termsLink) + termsLink.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        termsTextView.text = spannableStringBuilder
        termsTextView.movementMethod = LinkMovementMethod.getInstance();
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                register(null, null, account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                val loadingProgressBar = view?.findViewById<ProgressBar>(R.id.loading)
                loadingProgressBar?.visibility = View.GONE
            }
        }
    }


    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent,
            RC_SIGN_IN
        )
    }

    companion object {
        private const val TAG = "RegisterFragment"
        private const val RC_SIGN_IN = 7
    }
}