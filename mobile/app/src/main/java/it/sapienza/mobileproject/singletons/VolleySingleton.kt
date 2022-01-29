package it.sapienza.mobileproject.singletons

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import it.sapienza.mobileproject.BuildConfig
import it.sapienza.mobileproject.entities.dto.AuthRequest
import org.json.JSONObject


class VolleySingleton constructor(context: Context) {

    private lateinit var token : String
    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also {
                    INSTANCE = it
                }
            }
    }

    private val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    private fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    fun setToken(newToken: String){
        token = newToken
    }

    fun volleyRequest(
        method: Int,
        path: String,
        postData: String,
        responseListener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener
    ) {
        val url = BuildConfig.BASE_URL + path
        val jsonObjectRequest = AuthRequest(
            method,
            url,
            JSONObject(postData),
            responseListener,
            errorListener,
            token
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            0,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        addToRequestQueue(jsonObjectRequest)
    }
}
