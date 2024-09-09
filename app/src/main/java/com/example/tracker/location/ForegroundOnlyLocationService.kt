package com.example.tracker.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.IOException

class ForegroundOnlyLocationService : Service() {

    // For Android 12 or higher, foreground services cannot be started while app is running in the
    // background. For Android 11 or lower, this flag is needed so that the service can be
    // switched to the foreground when the app transitions into background. For details, see onBind.
    private var serviceRunningInForeground = false

    // For Android 11 or lower, this flag is used for detecting whether the app experiences
    // configuration changes, as opposed to transitioning into background. If it is a configuration
    // change, there is no need to continue the service as a foreground service. For details, see
    // onBind.
    private var configurationChange = false

    // A status bar notification is required for foreground service to make users aware that
    // the app is performing a task in the foreground and consuming system resources, as per
    // Android documentation.
    private lateinit var notificationManager: NotificationManager

    // Binds to an activity so communication can happen.
    private val localBinder = LocalBinder()

    // Google Play services location APIs. Device must support Google Play services to work
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // For preventing device from going into doze mode/sleep mode; can be removed for certain devices.
    private lateinit var wakeLock: PowerManager.WakeLock

    // Firebase Firebase Firestore and Authentication
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    /*
    In onCreate, we initialize the Google Firebase APIs
     */
    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        Log.d(TAG, "onCreate()")

        // Initialize Google Firebase services
        firestore = Firebase.firestore
        auth = Firebase.auth

        // Initialize a wakelock to prevent the service from stopping when the device goes into
        // doze mode.
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::MyWakelockTag"
        )
        // Suppress wakelock timeout
        wakeLock.acquire()

        // Initialize the notification manager so we can launch the service as a foreground service.
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // The location provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // The LocationRequest object for requesting location updates.
        // TODO: Introduce settings to tweak these values
        locationRequest = LocationRequest.Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setIntervalMillis(10000)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateAgeMillis(0)
            .setMaxUpdates(Integer.MAX_VALUE)
            .setMinUpdateDistanceMeters(0.0F)
            .build()

        // The LocationCallback object for handling location updates.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(
                    TAG,
                    "onLocationResult() ${locationResult.lastLocation?.time!!} $locationResult"
                )

                if (locationResult.lastLocation != null && auth.currentUser?.uid != null) {
                    // For details about each field, see LocationSnapshot.kt.
                    val dbEntry = LocationSnapshot(
                        // TODO: Fix an ID.
                        id = null,

                        recordTimeISOString = getDateTimeTzStringFromUnixEpoch(System.currentTimeMillis()),
                        recordTimeUnixEpoch = System.currentTimeMillis(),
                        snapshotTimeISOString = getDateTimeTzStringFromUnixEpoch(locationResult.lastLocation?.time!!),
                        snapshotTimeUnixEpoch = locationResult.lastLocation?.time!!,

                        latitude = locationResult.lastLocation?.latitude,
                        longitude = locationResult.lastLocation?.longitude,
                        accuracy = locationResult.lastLocation?.accuracy,
                        hasAccuracy = locationResult.lastLocation?.hasAccuracy() ?: false,

                        speed = locationResult.lastLocation?.speed,
                        hasSpeed = locationResult.lastLocation?.hasSpeed() ?: false,
                        speedAccuracyMetersPerSecond = locationResult.lastLocation?.speedAccuracyMetersPerSecond,
                        hasSpeedAccuracy = locationResult.lastLocation?.hasSpeedAccuracy() ?: false,

                        altitude = locationResult.lastLocation?.altitude,
                        hasAltitude = locationResult.lastLocation?.hasAltitude() ?: false,
                        verticalAccuracyMeters = locationResult.lastLocation?.verticalAccuracyMeters,
                        hasVerticalAccuracy = locationResult.lastLocation?.hasVerticalAccuracy()
                            ?: false,
                    )


                    val collectionName = getDateStringFromUnixEpoch(
                        locationResult.lastLocation?.time!!
                    )

                    val documentTimestamp = getDateTimeStringFromUnixEpoch(
                        locationResult.lastLocation?.time!!
                    )

                    firestore
                        .collection("snapshots") // The "snapshots" root collection.
                        .document(auth.currentUser!!.uid) // user ID
                        // Device for the user, defaults to "personal".
                        .collection("personal")
                        // A document named in date format, e.g. "2024-01-01", contains a
                        // a subcollection of timestamps, and fields of metadata
                        .document(collectionName)
                        // A subcollection containing all timestamps
                        .collection("timestamps")
                        // A document representing a LocationSnapshot object.
                        .document(documentTimestamp) //
                        .set(dbEntry)
                        .addOnSuccessListener { _ ->
                            Log.d(TAG, "firebase.set() success")
//                            writeLogToInternalStorage(
//                                "${dbEntry.snapshotTimeISOString}: Firebase write: $dbEntry"
//                            )
                            writeLogToInternalStorage(
                                "${dbEntry.snapshotTimeISOString}: [${dbEntry.latitude}, ${dbEntry.longitude}],  alt ${dbEntry.altitude}m, ${dbEntry.speed}m/s, "
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "firebase.set() failed")
                            Log.e(TAG, e.printStackTrace().toString())
                            writeLogToInternalStorage(
                                "${dbEntry.snapshotTimeISOString}: Firebase write failed: ${e.message}"
                            )
                        }

                }

                val intent = Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
                intent.putExtra(EXTRA_LOCATION, locationResult.lastLocation)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        }

        Log.d(TAG, "ForegroundOnlyLocationService launching")
        startForeground(NOTIFICATION_ID, generateNotification())
        serviceRunningInForeground = true
        Log.d(TAG, "ForegroundOnlyLocationService started")
    }

    // Log a message.
    fun writeLogToInternalStorage(logMessage: String) {
        val fileName = "logfile.txt"
        try {
            // Open file output stream to write to file
            openFileOutput(fileName, Context.MODE_APPEND).use { fos ->
                fos.write(logMessage.toByteArray())
                fos.write("\n".toByteArray())  // Add a new line after each log entry
                Log.d(TAG, "writeLogToInternalStorage() success")
            }
        } catch (e: IOException) {
            Log.e(TAG, "writeLogToInternalStorage() failed")
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        // If the service is killed, re-create it when possible.
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind()")

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // The following code is no longer needed since for Android 12 or higher, foreground
        // services cannot be started while app is running in the background. For Android 11 or
        // lower, this flag is needed so that the service can be switched to the foreground when
        // the app transitions into background.
        //
        // The current approach is simply launch the service as a foreground service from the get
        // go in the MainActivity, without having to consider whether it is necessary to switch
        // this service to foreground (when activity/app goes to background) or background (when
        // the user opens the app and it comes back to the foreground).

        /*
        // MainActivity (client) comes into foreground and binds to service, so the service can
        // become a background service.
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceRunningInForeground = false
        configurationChange = false
        */

        // Note: no need to call super.onBind, since it's default is simply returning null.
        // Check out the onBind method in Binder class for more details.

        return localBinder
    }

    // For details on why onRebind, onUnbind and onTaskRemoved are no longer needed, see the
    // the comments in onBind.
    // Note: These methods may be required to update FirebaseAuth and FirebaseFirestore.
    /*
    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceRunningInForeground = false
        configurationChange = false

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(currentLocation)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(currentLocation)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }
    }
    */

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    /**
     * Start the location tracking service.
     */
    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(
            Intent(
                applicationContext,
                ForegroundOnlyLocationService::class.java
            )
        )

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Unauthenticated; Service not started.")
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            return
        } else {
            Log.d(TAG, "Authenticated; Service started.")
            try {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
            } catch (unlikely: SecurityException) {
                SharedPreferenceUtil.saveLocationTrackingPref(this, false)
                Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
            }
        }
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    stopSelf()
                } else {
                    Log.e(TAG, "Failed to remove Location Callback.")
                }
            }
            removeTask.addOnFailureListener { task ->
                Log.e(TAG, "Failed to remove Location Callback.", task)
            }
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }


    private fun generateNotification(): Notification {
        Log.d(TAG, "generateNotification()")

        // Android 8.0 (API level 26) or above requires all notifications to be assigned to a
        // channel.
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(notificationChannel)

        // Title and text.
        val mainNotificationText =
            getDateTimeTzStringFromUnixEpoch(System.currentTimeMillis()) + ": Enabled"
        val titleText = getString(R.string.app_name)
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // Clicking on the notification resumes the Activity.
        val launchActivityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Note: notification channel ID is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setContentIntent(activityPendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Class used for the client Binder. Since this service runs in the same process as its
     * clients, we don't need to deal with inter-process communication (IPC).
     */
    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val TAG = "ForegroundOnlyLocationService"

        private const val PACKAGE_NAME = "com.example.android.tracker"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "Tracker"
    }
}