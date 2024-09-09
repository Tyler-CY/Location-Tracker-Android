package com.example.tracker.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tracker.databinding.FragmentHomeBinding
import com.example.tracker.location.SharedPreferenceUtil

private const val TAG = "HomeFragment"
private const val LOCATION_SERVICE_SWITCH_STATE = "LOCATION_SERVICE_SWITCH_STATE"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    interface HomeFragmentCallback {
        fun subscribeToLocationUpdates()
        fun unsubscribeToLocationUpdates()
        fun foregroundPermissionApproved(): Boolean
        fun requestForegroundPermissions()
        fun ignoreBatteryOptimizationPermissionApproved(): Boolean
        fun requestIgnoreBatteryOptimizationPermission()
    }

    private lateinit var homeFragmentCallback: HomeFragmentCallback

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttach()")
        super.onAttach(context)

        try {
            homeFragmentCallback = context as HomeFragmentCallback
        } catch (_: ClassCastException) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView()")

        super.onCreateView(inflater, container, savedInstanceState)

        // Handles bindings
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize textView
        val textView: TextView = binding.textHome
        textView.movementMethod = ScrollingMovementMethod()
        homeViewModel.text.observe(viewLifecycleOwner) {
            Log.d(TAG, "observe()")
            textView.text = homeViewModel.text.value

            // Auto-scroll the text to the bottom
            // TODO: Prevent auto scroll when the user manually scrolls up to check past logs.
            if (textView.layout != null) {
                val scrollAmount =
                    textView.layout.getLineTop(textView.lineCount) - textView.height
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    textView.scrollTo(0, scrollAmount)
                else
                    textView.scrollTo(0, 0)
            }
        }

        // Initialize switch
        binding.switch2.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                homeFragmentCallback.unsubscribeToLocationUpdates()
            } else {
                if (!homeFragmentCallback.ignoreBatteryOptimizationPermissionApproved()) {
                    homeFragmentCallback.requestIgnoreBatteryOptimizationPermission()
                }

                if (homeFragmentCallback.foregroundPermissionApproved()) {
                    homeFragmentCallback.subscribeToLocationUpdates()
                } else {
                    homeFragmentCallback.requestForegroundPermissions()
                }
            }
        }
        try {
            binding.switch2.isChecked =
                SharedPreferenceUtil.getLocationTrackingPref(requireContext())
        } catch (_: IllegalStateException) {

        }

//        val prefs: SharedPreferences? = activity?.getSharedPreferences("my_prefs", 0)
//        if (prefs != null) {
//            if (prefs.contains(LOCATION_SERVICE_SWITCH_STATE)) {
//                binding.switch2.isChecked = prefs.getBoolean(LOCATION_SERVICE_SWITCH_STATE, false)
//            }
//        }
//        if (savedInstanceState != null) {
//            binding.switch2.isChecked = savedInstanceState.getBoolean(LOCATION_SERVICE_SWITCH_STATE)
//        }

        return root
    }

    override fun onResume() {
        super.onResume()

        try {
            binding.switch2.isChecked =
                SharedPreferenceUtil.getLocationTrackingPref(requireContext())
        } catch (_: IllegalStateException) {

        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()

        val prefs: SharedPreferences? = activity?.getSharedPreferences("my_prefs", 0)
        if (prefs != null) {
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(LOCATION_SERVICE_SWITCH_STATE, binding.switch2.isChecked)
            editor.apply()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView()")


        super.onDestroyView()
        _binding = null
    }

//    override fun onSaveInstanceState(state: Bundle) {
//        super.onSaveInstanceState(state)
//        state.putBoolean(LOCATION_SERVICE_SWITCH_STATE, binding.switch2.isChecked)
//    }
}