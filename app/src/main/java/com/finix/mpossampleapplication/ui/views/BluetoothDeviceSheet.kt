package com.finix.mpossampleapplication.ui.views

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceSheet(
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val devices by remember { mutableStateOf(pairedDevices?.toList() ?: emptyList()) }

        Text(
            "COMPATIBLE DEVICES",
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(devices.filter { it.name.startsWith("D135") }) { device ->
                BluetoothDeviceItem(device.name, device.address) {
                    viewModel.connectToTheDevice(device.name, device.address)
                    onDismiss.invoke()
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color.DarkGray,
                    thickness = 0.5.dp
                )
            }
        }

        Text(
            "Ensure your PAX device is turn on",
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 20.dp))

        LaunchedEffect(Unit) {
            if (!sheetState.isVisible) {
                sheetState.show()
            }
        }
    }
}

@Composable
fun BluetoothDeviceItem(deviceName: String, deviceAddress: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 15.dp, vertical = 5.dp)
    ) {
        Text(text = deviceName)
        Text(text = deviceAddress)
    }
}
