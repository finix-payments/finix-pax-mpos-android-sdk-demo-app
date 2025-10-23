package com.finix.mpossampleapplication.utils

import com.finix.mpos.models.MerchantData
import com.finix.mpos.models.SplitTransfer
import com.finix.mpossampleapplication.ui.viewModels.isValidKeyValueFormat
import com.finix.mpossampleapplication.utils.ConfigValidator.MERCHANT_ID_REGEX

object SplitTransferValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val errors: Map<String, String> = emptyMap()
    )

    fun validateMerchantConfig(
        splitTransfer: SplitTransfer
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (splitTransfer.merchantId.isBlank()) errors["merchantId"] = "Merchant Id is missing"
        else if (!splitTransfer.merchantId.matches(MERCHANT_ID_REGEX))
            errors["merchantId"] = "Merchant ID is not valid"

        if (splitTransfer.amount<=0) errors["amount"] = "Amount must greater then 0"

        splitTransfer.tags?.entries?.let {
            if (!isValidKeyValueFormat(it.joinToString { "${it.key}:${it.value}" })) errors["tags"] = "Invalid format. Use key:value, key2:value2"
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}