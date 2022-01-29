package it.sapienza.mobileproject.fragments.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.R

class LoginFragment : Fragment() {

    private lateinit var authViewModel: AuthViewModel

    private lateinit var auth: FirebaseAuth

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (activity as AppCompatActivity).supportActionBar?.title=getString(R.string.log_in)
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        val signUpText = view.findViewById<Button>(R.id.sign_up_text)
        signUpText.setOnClickListener {
            goToSignUp(it)
        }
        return view
    }

    private fun goToSignUp(view: View){
        Log.i(TAG, "sign up")
        Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment)
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
        val loginButton = view.findViewById<Button>(R.id.login_fragment_button)
        val googleButton = view.findViewById<Button>(R.id.login_google_button)
        val loadingProgressBar = view.findViewById<ProgressBar>(R.id.loading)

        authViewModel.authFormState.observe(viewLifecycleOwner,
            Observer { loginFormState ->
                if (loginFormState == null) {
                    return@Observer
                }
                loginButton.isEnabled = loginFormState.isDataValid
                loginFormState.usernameError?.let {
                    usernameEditText.error = getString(it)
                }
                loginFormState.passwordError?.let {
                    passwordEditText.error = getString(it)
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
                authViewModel.loginDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
        }
        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    null
                )
            }
            false
        }

        loginButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            login(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                null
            )
        }
        googleButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            googleSignIn()
        }
    }

    private fun login(email: String?, password: String?, idToken: String?) {

        Log.d(TAG, "firebaseUser: ${auth?.currentUser}")
        if(auth?.currentUser!=null && auth?.currentUser?.isAnonymous!!){
            // delete anonymous user if any
            auth.currentUser?.delete()
        }

        val credential = if(idToken!=null){
            GoogleAuthProvider.getCredential(idToken, null)
        } else{
            EmailAuthProvider.getCredential(email!!, password!!)
        }

        auth.signInWithCredential(credential).addOnCompleteListener(this.requireActivity()) { task ->
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                login(null, null, account.idToken!!)
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
        private const val TAG = "LoginFragment"
        private const val RC_SIGN_IN = 7
    }
}