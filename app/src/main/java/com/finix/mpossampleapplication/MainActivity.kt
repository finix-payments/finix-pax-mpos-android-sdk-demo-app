package com.finix.mpossampleapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.finix.mpossampleapplication.ui.theme.MPOSSampleApplicationTheme
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel
import com.finix.mpossampleapplication.ui.views.Loader
import com.finix.mpossampleapplication.ui.views.NumberInputButton

class MainActivity : ComponentActivity() {

    private var viewModel = TransactionsViewModel()
    private val _permissionsGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    private val permissionsGranted: LiveData<Boolean>
        get() = _permissionsGranted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetooth()
        setContent {
            MPOSSampleApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoading by viewModel.isLoading.observeAsState(false)
                    val permissionsAccepted by permissionsGranted.observeAsState()
                    val isConnected by viewModel.isConnected.observeAsState(false)

                    NumberInputButton(viewModel, permissionsAccepted ?: true, isConnected)
                    if (isLoading) {
                        Loader(
                            viewModel,
                            isConnected
                        )
                    }
                }
            }
        }
        viewModel.initializeDevice(this)
    }

    override fun onDestroy() {
        viewModel.destroy()
        super.onDestroy()
    }

    private fun requestBluetooth() {
        // check android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                _permissionsGranted.postValue(true)
            } else {
                _permissionsGranted.postValue(false)
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.contains(false)) {
                _permissionsGranted.postValue(false)
            } else {
                _permissionsGranted.postValue(true)
            }
        }
}
