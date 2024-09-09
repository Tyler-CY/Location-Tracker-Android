package com.example.tracker

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.util.Log
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tracker.databinding.ActivityMainBinding
import com.example.tracker.location.ForegroundOnlyLocationService
import com.example.tracker.location.SharedPreferenceUtil
import com.example.tracker.ui.home.HomeFragment
import com.example.tracker.ui.home.HomeViewModel
import com.google.android.material.navigation.NavigationView
import java.io.FileNotFoundException

private const val TAG = "MainActivity"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    HomeFragment.HomeFragmentCallback {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences

    private val viewModel: HomeViewModel by viewModels()

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected()")
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "onServiceDisconnected()")
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        /**
         * Menu UI setup
         */
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        /**
         * Location Service setup
         */
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()
        Log.d(TAG, "foregroundOnlyBroadcastReceiver assigned")
        Log.d(TAG, foregroundOnlyBroadcastReceiver.toString())
        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
//        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
////        serviceIntent.setAction(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
//        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
//        serviceIntent.setAction(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")

        super.onResume()

        Log.d(TAG, "foregroundOnlyBroadcastReceiver")
        Log.d(TAG, foregroundOnlyBroadcastReceiver.toString())

        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
        )

        viewModel.setText(readLogFromInternalStorage())
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")

//        Log.d(TAG, "foregroundOnlyBroadcastReceiver")
//        Log.d(TAG, foregroundOnlyBroadcastReceiver.toString())
//
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(
//            foregroundOnlyBroadcastReceiver
//        )

//        if (foregroundOnlyLocationServiceBound) {
//            unbindService(foregroundOnlyServiceConnection)
//            foregroundOnlyLocationServiceBound = false
//        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()

        Log.d(TAG, "foregroundOnlyBroadcastReceiver")
        Log.d(TAG, foregroundOnlyBroadcastReceiver.toString())

        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
    }

    override fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        foregroundOnlyLocationService?.subscribeToLocationUpdates()
    }

    override fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
    }

    override fun foregroundPermissionApproved(): Boolean {
        Log.d(TAG, "foregroundPermissionApproved()")

        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun requestForegroundPermissions() {
        Log.d(TAG, "requestForegroundPermissions()")

        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun ignoreBatteryOptimizationPermissionApproved(): Boolean {
        Log.d(TAG, "ignoreBatteryOptimizationPermissionApproved()")

        val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(this.packageName)
    }

    override fun requestIgnoreBatteryOptimizationPermission() {
        Log.d(TAG, "requestIgnoreBatteryOptimizationPermission()")

        val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        val uri = Uri.fromParts("package", this.packageName, null)
        intent.data = uri
        this.startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult()")

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()

                else -> {
                    SharedPreferenceUtil.saveLocationTrackingPref(applicationContext, false)
                }
            }
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, p1: String?) {
    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive()")
            viewModel.setText(readLogFromInternalStorage())
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun readLogFromInternalStorage(): String {
        val fileName = "logfile.txt"
        return try {
            openFileInput(fileName).bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            "File not found"
        }
    }
}