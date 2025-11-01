package com.finix.mpossampleapplication.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.finix.mpos.models.Currency
import com.finix.mpos.models.EnvEnum
import com.finix.mpos.models.MerchantData
import com.finix.mpos.models.SplitTransfer
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class ConfigPrefs @Inject constructor() {
    companion object {
        private val PREF_NAME = "merchant_data"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_MERCHANT_ID = "merchant_id"
        private const val KEY_MERCHANT_MID = "merchant_mid"
        private const val KEY_API_USERNAME = "username"
        private const val KEY_API_PASSWORD = "password"
        private const val KEY_SPLIT_MERCHANTS = "split_merchants"
        private const val KEY_ENVIRONMENT = "environment"
        private const val KEY_TAGS = "tags"

        const val D135 = "D135"
        const val CONFIG_FILE_NAME = "merchant_config.json"
    }

    private fun key(key: String, env: EnvEnum) = "${key}_${env.name}"

    fun saveConfigurations(context: Context, data: MerchantData) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(key(KEY_DEVICE_ID, data.env), data.deviceId)
            putString(key(KEY_MERCHANT_ID, data.env), data.merchantId)
            putString(key(KEY_MERCHANT_MID, data.env), data.mid)
            putString(key(KEY_API_USERNAME, data.env), data.userId)
            putString(key(KEY_API_PASSWORD, data.env), data.password)
            putString(KEY_ENVIRONMENT, data.env.name)
            apply()
        }
    }


    fun loadConfigurations(context: Context, env: EnvEnum): MerchantData {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val deviceId = prefs.getString(key(KEY_DEVICE_ID, env), "")

        // If no saved config → load from file, then save to prefs
        if (deviceId.isNullOrEmpty()) {
            initConfigurationWithFile(context, env)
        }

        return MerchantData(
            deviceId = prefs.getString(key(KEY_DEVICE_ID, env), "") ?: "",
            merchantId = prefs.getString(key(KEY_MERCHANT_ID, env), "") ?: "",
            mid = prefs.getString(key(KEY_MERCHANT_MID, env), "") ?: "",
            env = env,
            userId = prefs.getString(key(KEY_API_USERNAME, env), "") ?: "",
            password = prefs.getString(key(KEY_API_PASSWORD, env), "") ?: "",
            currency = Currency.USD
        )
    }


    fun initConfigurationWithFile(context: Context, env: EnvEnum) {
        try {
            val inputStream = context.assets.open(CONFIG_FILE_NAME)
            val json = inputStream.bufferedReader().use { it.readText() }

            if (json.isBlank()) {
                Log.e("Configuration Init", "Configuration file is empty")
                return
            }

            val jsonObject = Gson().fromJson(json, JsonObject::class.java)

            val envData = jsonObject.getAsJsonObject(env.name)
            if (envData == null) {
                Log.e("Configuration Init", "No configuration found for env: ${env.name}")
                return
            }

            val parsedData = Gson().fromJson(envData, MerchantData::class.java)

            // Build MerchantData with env and currency assigned
            val merchantData = MerchantData(
                deviceId = parsedData.deviceId,
                merchantId = parsedData.merchantId,
                mid = parsedData.mid,
                userId = parsedData.userId,
                password = parsedData.password,
                currency = Currency.USD,
                env = env
            )

            val validation = ConfigValidator.validateMerchantConfig(merchantData)

            if(validation.isValid) {
                saveConfigurations(context, merchantData)
                Log.d("Configuration Init", "Loaded and saved config for $env: $merchantData")
            } else {
                Log.d("Configuration Init", "Failed to initialize config file for invalid data")
            }

        } catch (e: Exception) {
            Log.e("Configuration Init", "Failed to initialize configuration for $env", e)
        }
    }


    fun saveSplitMerchants(context: Context, env: EnvEnum, merchants: List<SplitTransfer>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            val gson = Gson()
            val json = gson.toJson(merchants)
            putString(key(KEY_SPLIT_MERCHANTS, env), json)
        }
    }

    fun loadSplitMerchants(context: Context, env: EnvEnum): List<SplitTransfer> {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(key(KEY_SPLIT_MERCHANTS, env), null)

        if (json.isNullOrEmpty()) return emptyList()

        val gson = Gson()
        val type = object : TypeToken<List<SplitTransfer>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearSplitMerchants(context: Context, env: EnvEnum) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(key(KEY_SPLIT_MERCHANTS, env)) }
    }

    fun saveTags(context: Context, env: EnvEnum, tags: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(key(KEY_TAGS, env), tags)
            apply()
        }
    }

    fun loadTags(context: Context, env: EnvEnum): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key(KEY_TAGS, env), "") ?: ""
    }

    fun loadCurrentEnvironment(context: Context): EnvEnum {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val envName = prefs.getString(KEY_ENVIRONMENT, null)
        return try {
            EnvEnum.valueOf(envName ?: EnvEnum.PROD.name)
        } catch (e: IllegalArgumentException) {
            EnvEnum.PROD
        }
    }
}