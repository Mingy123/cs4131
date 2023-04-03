package com.example.cryptochat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.RequestQueue

class MainViewModel : ViewModel() {
    private val requestQueue = MutableLiveData<RequestQueue>()

    fun setRequestQueue(queue: RequestQueue) {
        requestQueue.value = queue
    }
    fun getRequestQueue(): RequestQueue? = requestQueue.value
}