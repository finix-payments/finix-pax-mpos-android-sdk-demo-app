package com.finix.mpossampleapplication.ui.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finix.mpos.models.Currency
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
import kotlinx.coroutines.flow.collectLatest
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

    private val _connectedDeviceName = MutableLiveData("")
    val connectedDeviceName: LiveData<String?> = _connectedDeviceName

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _transactionStatus = MutableLiveData("")
    var transactionStatus: LiveData<String> = _transactionStatus

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    var logText by mutableStateOf("")
        private set

    private val _merchantData = MutableStateFlow(
        MerchantData(
            env = EnvEnum.PROD,
            merchantId = "",
            mid = "",
            deviceId = "",
            currency = Currency.USD,
            userId = "",
            password = ""
        )
    )

    val merchantData: StateFlow<MerchantData> = _merchantData.asStateFlow()

    private val _splitMerchants = MutableStateFlow<List<SplitTransfer>>(emptyList())
    var splitMerchants = _splitMerchants

    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags

    fun saveTags(tags: String) {
        _tags.value = tags
        configPrefs.saveTags(context, merchantData.value.env, tags)
    }

    init {
        try {
            viewModelScope.launch {
                _merchantData.collectLatest { data ->
                    mpos = MPOSFinix(context, data)
                }
            }

            val env = configPrefs.loadCurrentEnvironment(context)
            _merchantData.value = loadConfigurations(env)
            _splitMerchants.value = configPrefs.loadSplitMerchants(context, env)
            _tags.value = configPrefs.loadTags(context, env)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        _splitMerchants.value = emptyList()
        configPrefs.clearSplitMerchants(context, merchantData.value.env)
    }

    fun transact(amount: String, transactionType: TransactionType) {
        appendLog("\nStart New Transaction\n")
        setLoading(true)

        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val amountInCents = (amount.toDouble() * 100).toLong()

                mpos.startTransaction(
                    amountInCents,
                    transactionType,
                    transactionCallback(transactionType),
                    emvCallback(),
                    _splitMerchants.value.ifEmpty { null },
                    getTagsMap(tags.value)
                )
            }.onFailure {
                appendLog("Transaction Error -> ${it.message}\n")
                setLoading(false)
                showStatus("$transactionType Failed!")
            }
        }
    }

    fun connectToTheDevice(deviceName: String, deviceAddress: String) {
        setLoading(true)
        appendLog("\nStart Connection to Device\n")

        if (mpos.isConnected()) {
            if (deviceName == connectedDeviceName.value) {
                appendLog("Device Already Connected\n")
                setLoading(false)
                _isConnected.postValue(true)
                _connectedDeviceName.postValue(deviceName)
                return
            } else {
                appendLog("Disconnecting device ${connectedDeviceName.value}\n")
                disconnect()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                mpos.connect(deviceName, deviceAddress, object : MPOSConnectionCallback {
                    override fun onSuccess() {
                        appendLog("Device Connected\n")
                        _connectedDeviceName.postValue(deviceName)
                        setLoading(false)
                        _isConnected.postValue(true)
                    }

                    override fun onError(errorMessage: String) {
                        appendLog("Device Connection Error: $errorMessage\n")
                        setLoading(false)
                        _isConnected.postValue(false)
                    }

                    override fun onProcessing(currentStepMessage: String) {
                        appendLog("Device Connecting: $currentStepMessage\n")
                    }
                })
            }.onFailure {
                appendLog("Device Failed to Connect: ${it.message}\n")
                _isConnected.postValue(false)
                setLoading(false)
            }
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

    fun sendDebugData() {
        setLoading(true)
        appendLog("Sending debug data...\n")

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mpos.sendDebugReport()
                setLoading(false)
                appendLog("Sent\n")
            }
        }
    }

    fun cancelTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            logText += "Cancel Transaction \n"
            mpos.cancelTransaction()
        }
    }

    fun clearLogs() {
        logText = ""
    }

    fun destroy() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (mpos.isConnected()) {
                    disconnect()
                    appendLog("Device Disconnected\n")
                } else {
                    appendLog("Device Not Connected\n")
                }
            } catch (e: Exception) {
                appendLog("Error disconnecting device: ${e.message}\n")
            }
        }
    }

    fun disconnect() {
        mpos.finishTransaction()
        mpos.disconnect()
        _isConnected.postValue(false)
        _connectedDeviceName.postValue("")
    }

    private fun appendLog(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        logText += "[$timestamp] $message\n"
    }

    private fun setLoading(value: Boolean) {
        _isLoading.postValue(value)
    }

    fun showStatus(stats: String) {
        _transactionStatus.postValue(stats)
    }

    fun endStatus() {
        _transactionStatus.postValue("")
    }

    private fun transactionCallback(transactionType: TransactionType): MPOSTransactionCallback =
        object : MPOSTransactionCallback {
            override fun onSuccess(result: TransactionResult?) {
                result?.id
                    ?.takeIf { it.isNotBlank() }
                    ?.let { appendLog("${transactionName(transactionType)} id: $it\n") }
                appendLog("✅ Transaction Success \n")

                setLoading(false)
                showStatus(transactionName(transactionType) + " Complete")
            }

            override fun onError(errorMessage: String) {
                appendLog("❌ Transaction Error -> $errorMessage\n")
                setLoading(false)
                showStatus(transactionName(transactionType) + " Failed")
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

    fun loadEnvironments(): List<EnvEnum> {
        return configPrefs.loadEnvironments(context)
    }

    fun transactionName(transactionType: TransactionType): String = when (transactionType) {
        TransactionType.SALE -> "Sale"
        TransactionType.AUTHORIZATION -> "Authorization"
        TransactionType.REFUND -> "Refund"
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
