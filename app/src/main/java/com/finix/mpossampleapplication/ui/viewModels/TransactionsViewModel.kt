package com.finix.mpossampleapplication.ui.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finix.mpos.models.EnvEnum
import com.finix.mpos.models.MerchantData
import com.finix.mpos.models.SplitTransfer
import com.finix.mpos.models.TransactionResult
import com.finix.mpos.models.TransactionType
import com.finix.mpos.sdk.MPOSConnectionCallback
import com.finix.mpos.sdk.MPOSEMVProcessingCallback
import com.finix.mpos.sdk.MPOSFinix
import com.finix.mpos.sdk.MPOSTransactionCallback
import com.finix.mpossampleapplication.utils.ConfigPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configPrefs: ConfigPrefs
) : ViewModel() {
    private lateinit var mpos: MPOSFinix

    private val _deviceName = MutableLiveData("")
    val deviceName: LiveData<String?> = _deviceName

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _transactionStatus = MutableLiveData("")
    var transactionStatus: LiveData<String> = _transactionStatus

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    var logText by mutableStateOf("")
        private set

    private val _merchantData = MutableStateFlow<MerchantData>(
        loadConfigurations(configPrefs.loadCurrentEnvironment(context))
    )
    val merchantData: StateFlow<MerchantData> = _merchantData.asStateFlow()

    private val _splitMerchants = MutableStateFlow<List<SplitTransfer>>(
        configPrefs.loadSplitMerchants(context = context, env = merchantData.value.env)
    )
    var splitMerchants = _splitMerchants


    private val _tags = MutableStateFlow<String>(
        configPrefs.loadTags(context, merchantData.value.env)
    )
    val tags: StateFlow<String> = _tags

    fun saveTags(tags: String) {
        _tags.value = tags
        configPrefs.saveTags(context, merchantData.value.env, tags)
    }

    fun initializeDevice(context: Context) {
        mpos = MPOSFinix(context, merchantData.value)
    }

    fun saveMerchantData(updatedMerchantData: MerchantData) {
        _merchantData.value = updatedMerchantData
        configPrefs.saveConfigurations(context, updatedMerchantData)
    }

    fun saveSplitData(updatedSplit: List<SplitTransfer>) {
        _splitMerchants.value = updatedSplit
        configPrefs.saveSplitMerchants(context, merchantData.value.env, updatedSplit)
    }

    fun clearSplitData() {
        _splitMerchants.value = emptyList<SplitTransfer>()
        configPrefs.clearSplitMerchants(context, merchantData.value.env)
    }

    fun transact(amount: String, transactionType: TransactionType) {
        appendLog("\nStart New Transaction\n")
        setLoading(true)


        kotlin.runCatching {
            val amountInCents = (amount.toDouble() * 100).toLong()

            mpos.startTransaction(
                amountInCents,
                transactionType,
                transactionCallback(),
                emvCallback(),
                _splitMerchants.value.ifEmpty { null },
                getTagsMap(tags.value)
            )
        }.onFailure {
            appendLog("Transaction Error -> ${it.message}\n")
            setLoading(false)
            showStatus(transactionType.toString()+ "Failed!")
        }
    }

    fun connectToTheDevice(deviceName: String, deviceAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            setLoading(true)
            appendLog("\nStart Connection to Device\n")

            if (mpos.isConnected()) {
                appendLog("Device Already Connected\n")
                _isConnected.postValue(true)
                setLoading(false)
                return@launch
            }

            runCatching {
                mpos.connect(deviceName, deviceAddress, object : MPOSConnectionCallback {
                    override fun onSuccess() {
                        appendLog("Device Connected\n")
                        _deviceName.value = deviceName
                        _isConnected.postValue(true)
                    }

                    override fun onError(errorMessage: String) {
                        appendLog("Device Connection Error: $errorMessage\n")
                        _isConnected.postValue(false)
                    }

                    override fun onProcessing(currentStepMessage: String) {
                        appendLog("Device Connecting: $currentStepMessage\n")
                    }
                })
            }.onFailure {
                appendLog("Device Failed to Connect: ${it.message}\n")
                _isConnected.postValue(false)
            }

            setLoading(false)
        }
    }

    fun resetFiles() {
        viewModelScope.launch {
            setLoading(true)
            appendLog("Reset Files\n")
            withContext(Dispatchers.IO) {
                mpos.resetFilesToDevice()
            }
            appendLog("Reset Files Complete\n")
            setLoading(false)
        }
    }

    fun resetDevice() {
        viewModelScope.launch {
            setLoading(true)
            appendLog("Reset Device\n")

            withContext(Dispatchers.IO) {
                mpos.resetDevice()
            }

            appendLog("Reset Device Complete\n")
            setLoading(false)
            _isConnected.postValue(false)
        }
    }

    fun cancelTransaction() {
        logText += "Cancel Transaction \n"
        mpos.cancelTransaction()
    }


    fun clearLogs() {
        logText = ""
    }

    fun destroy() {
        appendLog(
            if (mpos.isConnected()) {
                mpos.finishTransaction()
                mpos.disconnect()
                _isConnected.postValue(false)
                "Device Disconnected\n"
            } else {
                "Device Not Connected\n"
            }
        )
    }

    private fun appendLog(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        logText += "[$timestamp] $message\n"
    }

    private fun setLoading(value: Boolean) {
        _isLoading.postValue(value)
    }

    fun showStatus(stats: String) {
        _transactionStatus.value = stats
    }

    fun endStatus() {
        _transactionStatus.value = ""
    }

    private fun transactionCallback(): MPOSTransactionCallback = object : MPOSTransactionCallback {
        override fun onSuccess(result: TransactionResult?) {
            appendLog("Transaction Success\n")
            setLoading(false)
        }

        override fun onError(errorMessage: String) {
            appendLog("Transaction Error -> $errorMessage\n")
            setLoading(false)
        }

        override fun onProcessing(currentStepMessage: String) {
            appendLog("Transaction Status -> $currentStepMessage\n")
        }
    }

    private fun emvCallback(): MPOSEMVProcessingCallback = object : MPOSEMVProcessingCallback {
        override fun onError(errorMessage: String) {
            appendLog("EMV Processing Error -> $errorMessage\n")
        }

        override fun onProcessing(currentStepMessage: String) {
            appendLog("EMV Processing Status -> $currentStepMessage\n")
        }
    }

    fun loadConfigurations(env: EnvEnum): MerchantData {
        return configPrefs.loadConfigurations(context = context, env)
    }

    fun isValidSplitMerchants(merchants: List<SplitTransfer>): Boolean {
        merchants.forEach {
            if(it.merchantId.isEmpty() || it.amount<=0) {
                return false
            }
        }
        return true
    }

    fun getTagsMap(inputTag: String): Map<String, String>? {
        return inputTag
            .takeIf { it.isNotBlank() && isValidKeyValueFormat(it) }
            ?.split(",")
            ?.map { it.trim() }?.associate {
                val (key, value) = it.split(":").map(String::trim)
                key to value
            }
    }

    fun isValidKeyValueFormat(input: String): Boolean {
        return input.split(",").all {
            it.contains(":") && it.split(":").size == 2
        }
    }
}

fun isValidKeyValueFormat(input: String): Boolean {
    return input.split(",").all {
        it.contains(":") && it.split(":").size == 2
    }
}

fun MerchantData.copyWith(
    deviceId: String = this.deviceId,
    merchantId: String = this.merchantId,
    mid: String = this.mid,
    userId: String = this.userId,
    password: String = this.password,
    env: EnvEnum = this.env
): MerchantData {
    return MerchantData(
        deviceId = deviceId,
        merchantId = merchantId,
        mid = mid,
        userId = userId,
        password = password,
        env = env
    )
}
