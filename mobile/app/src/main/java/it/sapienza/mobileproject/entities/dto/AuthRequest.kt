package it.sapienza.mobileproject.entities.dto

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject


class AuthRequest : JsonObjectRequest {

    private var userToken: String = ""

    constructor(
        method: Int,
        url: String?,
        requestBody: JSONObject,
        listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener?,
        token: String
    ) : super(method, url, requestBody, listener, errorListener) {
        // Check if user is signed in (non-null) and update UI accordingly.
        userToken = token
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject>? {
        return if (response != null && response.data.isEmpty()) Response.success(
            JSONObject(),
            null
        ) else super.parseNetworkResponse(response)
    }

    @Throws(AuthFailureError::class)
    override fun getHeaders(): Map<String, String> {
        val headers: MutableMap<String, String> = HashMap()
        headers["Content-Type"] = "application/json";
        headers["Authorization"] = "Bearer $userToken"
        return headers
    }

}