package com.finix.mpossampleapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.finix.mpossampleapplication.ui.theme.MPOSSampleApplicationTheme
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel
import com.finix.mpossampleapplication.ui.views.MainViews
import com.finix.mpossampleapplication.ui.views.Progress
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TransactionsViewModel by viewModels()

    private val _permissionsGranted = MutableStateFlow(false)
    private val permissionsGranted = _permissionsGranted.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                _permissionsGranted.collect { granted ->
                    if (granted) {
                        viewModel.initializeDevice(this@MainActivity)
                    }
                }
            }
        }

        setContent {
            MPOSSampleApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoading by viewModel.isLoading.observeAsState(false)
                    val permissionsAccepted by permissionsGranted.collectAsState()
                    val isConnected by viewModel.isConnected.observeAsState(false)

                    MainViews(viewModel = viewModel, permissionsAccepted ?: true, isConnected)
                    if(isLoading) {
                        Progress(viewModel, isConnected)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.destroy()
        super.onDestroy()
    }

    private fun requestBluetoothPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    )
                )
            }
            else -> {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestEnableBluetooth.launch(enableBtIntent)
            }
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            _permissionsGranted.value = (result.resultCode == RESULT_OK)
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            _permissionsGranted.value = permissions.values.all { it }
        }
}
