package com.finix.mpossampleapplication.ui.views

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.text.isDigitsOnly
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel

@Composable
fun NumberInputButton(
    viewModel: TransactionsViewModel,
    permissionsAccepted: Boolean,
    isConnected: Boolean
) {
    var text by remember { mutableStateOf("3.14") }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!permissionsAccepted) {
            Text(text = "Bluetooth permission not granted, cannot select device. Please go to app settings and enable permission")
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.padding(horizontal = 5.dp),
            enabled = permissionsAccepted
        ) {
            Text("Scan for Devices")
        }

        Button(
            onClick = { viewModel.destroy() },
            modifier = Modifier.padding(horizontal = 5.dp),
            enabled = permissionsAccepted
        ) {
            Text("Disconnect current device")
        }

        Spacer(modifier = Modifier.height(8.dp))

//        Button(
//            onClick = { viewModel.resetDevice() },
//            modifier = Modifier.padding(horizontal = 5.dp),
//            enabled = isConnected
//        ) {
//            Text("Reset device")
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))

//        Button(
//            onClick = { viewModel.resetFiles() },
//            modifier = Modifier.padding(horizontal = 5.dp),
//            enabled = isConnected
//        ) {
//            Text("Reset files")
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = text,
            onValueChange = { if (it.isDigitsOnly()) text = it },
            enabled = isConnected
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            arrayOf("Sale", "Auth", "Refund").forEach {
                Button(
                    onClick = { viewModel.transact(text, it) },
                    modifier = Modifier.padding(horizontal = 5.dp),
                    enabled = isConnected
                ) {
                    Text(it)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.padding(horizontal = 5.dp),
            ) {
                Text("Clear Logs")
            }
        }

        if (showDialog && permissionsAccepted) {
            BluetoothDeviceListDialog(viewModel, onDismiss = { showDialog = false })
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "LOGS")

        Text(
            text = viewModel.logText,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .align(Alignment.Start)
        )
    }
}

@Composable
fun Loader(
    viewModel: TransactionsViewModel,
    isConnected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .pointerInput(Unit) {
                detectTapGestures { }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            if (isConnected) {
                Button(
                    onClick = { viewModel.cancelTransaction() },
                    modifier = Modifier.padding(horizontal = 5.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceListDialog(
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit
) {
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    val devices by remember { mutableStateOf(pairedDevices?.toList() ?: emptyList()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select your mPOS device")
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    items(devices) { device ->
                        BluetoothDeviceItem(device.name, device.address) {
                            viewModel.connectToTheDevice(device.name, device.address)
                            onDismiss.invoke()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceItem(deviceName: String, deviceAddress: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column {
            Text(text = deviceName)
            Text(text = deviceAddress)
        }
    }
}
