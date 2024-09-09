package com.example.tracker.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

private val TAG = "HomeViewModel"

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "null"
    }
    val text: LiveData<String> = _text

    fun setText(q: String) {
        Log.d(TAG, "setText()")

        _text.value = q
    }
}