package com.example.tracker.location

import android.content.Context
import androidx.core.content.edit
import com.example.tracker.R
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Returns a date format of "yyyy-MM-dd" from a Unix Epoch Time format.
 */
fun getDateStringFromUnixEpoch(s: Long): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val netDate = Date(s)
        return sdf.format(netDate)
    } catch (e: Exception) {
        return e.toString()
    }
}

/**
 * Returns a datetime format of "yyyy-MM-dd HH:mm:ss" from a Unix Epoch Time format.
 */
fun getDateTimeStringFromUnixEpoch(s: Long): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val netDate = Date(s)
        return sdf.format(netDate)
    } catch (e: Exception) {
        return e.toString()
    }
}

/**
 * Returns a datetime format of "yyyy-MM-dd HH:mm:ss.SSSS zzzz" from a Unix Epoch Time format.
 */
fun getDateTimeTzStringFromUnixEpoch(s: Long): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS zzzz")
        val netDate = Date(s)
        return sdf.format(netDate)
    } catch (e: Exception) {
        return e.toString()
    }
}


/**
 * Provides access to SharedPreferences for location to Activities and Services.
 */
internal object SharedPreferenceUtil {

    private const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The [Context].
     */
    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
            .getBoolean(KEY_FOREGROUND_ENABLED, false)

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }
}