package it.sapienza.mobileproject.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import it.sapienza.mobileproject.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}