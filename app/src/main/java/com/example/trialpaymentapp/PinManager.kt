package com.example.trialpaymentapp

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

class PinManager(context: Context) {

    companion object {
        private const val PIN_FILE_NAME = "app_pin_prefs"
        private const val PIN_KEY = "user_app_pin"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            PIN_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: GeneralSecurityException) {
        throw IOException("Failed to create EncryptedSharedPreferences", e)
    } catch (e: IOException) {
        throw IOException("Failed to create EncryptedSharedPreferences", e)
    }

    fun setPin(pin: String) {
        with(sharedPreferences.edit()) {
            putString(PIN_KEY, pin)
            apply()
        }
    }

    fun getPin(): String? {
        return sharedPreferences.getString(PIN_KEY, null)
    }

    fun isPinSet(): Boolean {
        return getPin() != null
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedPin = getPin()
        return storedPin != null && storedPin == enteredPin
    }

    fun clearPin() {
        with(sharedPreferences.edit()) {
            remove(PIN_KEY)
            apply()
        }
    }
}
