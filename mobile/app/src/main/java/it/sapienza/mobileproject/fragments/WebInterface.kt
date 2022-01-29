package it.sapienza.mobileproject.fragments

import com.android.volley.VolleyError
import org.json.JSONObject

interface WebInterface {

    fun onResponseCallback(response: JSONObject)

    fun onErrorCallback(error: VolleyError)
}