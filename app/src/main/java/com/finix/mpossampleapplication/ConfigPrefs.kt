package com.finix.mpossampleapplication

import android.content.Context
import com.finix.mpos.models.EnvEnum
import com.finix.mpos.models.MerchantData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import com.finix.mpos.models.SplitTransfer

object ConfigPrefs {
    private const val PREF_NAME = "merchant_data"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_MERCHANT_ID = "merchant_id"
    private const val KEY_MERCHANT_MID = "merchant_mid"
    private const val KEY_API_USERNAME = "username"
    private const val KEY_API_PASSWORD = "password"
    private const val KEY_SPLIT_MERCHANTS = "split_merchants"

    private const val KEY_ENVIRONMENT = "environment"
    private const val KEY_TAGS = "tags"

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
        return MerchantData(
            deviceId = prefs.getString(key(KEY_DEVICE_ID, env), "") ?: "",
            merchantId = prefs.getString(key(KEY_MERCHANT_ID, env), "") ?: "",
            mid = prefs.getString(key(KEY_MERCHANT_MID, env), "") ?: "",
            env = env,
            userId = prefs.getString(key(KEY_API_USERNAME, env), "") ?: "",
            password = prefs.getString(key(KEY_API_PASSWORD, env), "") ?: ""
        )
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

    fun loadEnvironment(context: Context): EnvEnum {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val envName = prefs.getString(KEY_ENVIRONMENT, null)
        return try {
            EnvEnum.valueOf(envName ?: EnvEnum.PROD.name)
        } catch (e: IllegalArgumentException) {
            EnvEnum.PROD
        }
    }
}