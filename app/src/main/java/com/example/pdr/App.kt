package com.example.pdr

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize application-wide components here
        // e.g., dependency injection, logging, analytics
    }
}