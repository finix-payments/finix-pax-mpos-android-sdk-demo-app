package com.finix.mpossampleapplication.ui.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finix.mpos.models.MerchantData
import com.finix.mpos.models.TransactionResult
import com.finix.mpos.sdk.MPOSConnectionCallback
import com.finix.mpos.sdk.MPOSEMVProcessingCallback
import com.finix.mpos.sdk.MPOSFinix
import com.finix.mpos.sdk.MPOSTransactionCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionsViewModel : ViewModel() {

    private lateinit var mpos: MPOSFinix
    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _isConnected: MutableLiveData<Boolean> = MutableLiveData()
    val isConnected: LiveData<Boolean>
        get() = _isConnected

    private val merchantData = MerchantData(
        merchantId = "[MERCHANT_ID]", // Update tou tor merchant ID
        mid = "[MID]", // Update to your mid
        deviceId = "[DEVICE_ID]", // Update to you device ID
        env = "qa", // Possible values are (qa, sandbox, production)
        userId = "[USER_ID]", // Update to your user ID
        password = "[PASSWORD]" //Update to your password
    )

    var logText by mutableStateOf("Log Output \n")
        private set

    fun transact(amount: String, transaction: String) {
        logText += "\nStart New Transaction\n\n"
        _isLoading.postValue(true)
        try {
            mpos.startTransaction(
                (amount.toDouble() * 100).toLong(),
                transaction.lowercase(), //Possible values are sale, auth and refund
                object : MPOSTransactionCallback {
                    override fun onSuccess(result: TransactionResult?) {
                        _isLoading.postValue(false)
                        logText += "Transaction Success\n"
                    }

                    override fun onError(errorMessage: String) {
                        _isLoading.postValue(false)
                        logText += "Transaction Error -> $errorMessage\n"
                    }

                    override fun onProcessing(currentStepMessage: String) {
                        logText += "Transaction Status -> $currentStepMessage\n"
                    }

                },
                object : MPOSEMVProcessingCallback {
                    override fun onError(errorMessage: String) {
                        logText += "EMV Processing Error -> $errorMessage\n"
                    }

                    override fun onProcessing(currentStepMessage: String) {
                        logText += "EMV Processing Status -> $currentStepMessage\n"
                    }

                }
            )
        } catch (e: Exception) {
            _isLoading.postValue(false)
            logText += "Transaction Error -> ${e.message}\n"
        }
    }

    fun cancelTransaction() {
        logText += "Cancel Transaction \n"
        mpos.cancelTransaction()
    }

    fun resetFiles() {
        _isLoading.postValue(true)
        logText += "Reset files \n"
        mpos.resetFilesToDevice()
        _isLoading.postValue(false)
    }

    fun resetDevice() {
        _isLoading.postValue(true)
        logText += "Reset Device \n"
        mpos.resetDevice()
        _isLoading.postValue(false)
    }

    fun initializeDevice(context: Context) {
        mpos = MPOSFinix(context, merchantData)
    }

    fun connectToTheDevice(deviceName: String, deviceAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            logText += "\nStart Connection to Device\n\n"
            try {
                if (!mpos.isConnected()) {
                    mpos.connect(
                        deviceName,
                        deviceAddress,
                        object : MPOSConnectionCallback {
                            override fun onSuccess() {
                                logText += "Device Connected\n"
                                _isConnected.postValue(true)
                            }

                            override fun onError(errorMessage: String) {
                                logText += "Device Connection Error: $errorMessage\n"
                                _isConnected.postValue(false)
                            }

                            override fun onProcessing(currentStepMessage: String) {
                                logText += "Device Connecting: $currentStepMessage\n"
                            }
                        }
                    )
                } else {
                    logText += "Device Already Connected\n"
                    _isConnected.postValue(true)
                }
            } catch (e: Exception) {
                _isConnected.postValue(false)
                logText += "Device Failed to Connect ${e.message}\n"
            }
            _isLoading.postValue(false)
        }
    }

    fun clearLogs() {
        logText = ""
    }

    fun destroy() {
        logText += if (mpos.isConnected()) {
            mpos.finishTransaction()
            mpos.disconnect()
            _isConnected.postValue(false)
            "Device Disconnected\n"
        } else {
            "Device Not Connected\n"
        }
    }
}
