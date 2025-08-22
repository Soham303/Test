package com.example.trialpaymentapp

import android.app.Application
import com.example.trialpaymentapp.data.AppDatabase

class PaymentApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    companion object {
        lateinit var instance: PaymentApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}