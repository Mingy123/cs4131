package com.example.cryptochat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class AppInfo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_info)
        supportActionBar?.title = "About App"
    }
}