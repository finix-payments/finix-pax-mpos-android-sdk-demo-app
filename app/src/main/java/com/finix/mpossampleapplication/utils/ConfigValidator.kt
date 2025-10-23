package com.finix.mpossampleapplication.utils

import com.finix.mpos.models.MerchantData

object ConfigValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val errors: Map<String, String> = emptyMap()
    )

    val MERCHANT_ID_REGEX = Regex("^MU[a-zA-Z0-9]*")
    private val DEVICE_ID_REGEX = Regex("^DV[a-zA-Z0-9]*")

    fun validateMerchantConfig(
        merchantData: MerchantData
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (merchantData.merchantId.isBlank()) errors["merchantId"] = "Merchant Id is missing"
        else if (!merchantData.merchantId.matches(MERCHANT_ID_REGEX))
            errors["merchantId"] = "Merchant ID is not valid"

        if (merchantData.deviceId.isBlank()) errors["deviceId"] = "Required"
        else if (!merchantData.deviceId.matches(DEVICE_ID_REGEX))
            errors["deviceId"] = "Device ID is not valid"

        if (merchantData.mid.isBlank()) errors["mid"] = "MID is missing"

        if (merchantData.userId.isBlank()) errors["userId"] = "User ID is missing"
        else if (merchantData.password.length < 8) errors["password"] = "Password must be at least 8 characters"

        return ValidationResult(errors.isEmpty(), errors)
    }
}