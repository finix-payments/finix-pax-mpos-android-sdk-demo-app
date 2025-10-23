package com.finix.mpossampleapplication.ui.views

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.finix.mpos.models.TransactionType
import com.finix.mpossampleapplication.R
import com.finix.mpossampleapplication.ui.theme.MPOSSampleApplicationTheme
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel
import com.finix.mpossampleapplication.utils.ConfigPrefs
import kotlinx.coroutines.delay


@Composable
fun MainViews(
    viewModel: TransactionsViewModel,
    permissionsAccepted: Boolean,
    isConnected: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var configurationSheet by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var otherSheet by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("3.14") }
    val cardColor = if(isSystemInDarkTheme()) Color.DarkGray else Color.LightGray

    Scaffold(
        topBar = {
            AppBar(
                viewModel,
                menuExpanded,
                onConfigurationSheetChange = { configurationSheet = it },
                onOtherSheetChange = { otherSheet = it },
                onMenuExpandedChange = { menuExpanded = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 15.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                if (!permissionsAccepted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Bluetooth permission not granted, cannot select device. Please go to app settings and enable permission",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("DEVICE")
                DeviceSection(
                    viewModel,
                    cardColor,
                    permissionsAccepted,
                    isConnected,
                    onScanClick = { showBottomSheet = true },
                    onDisconnectClick = { showDisconnectDialog = true }
                )

                if (showDisconnectDialog) {
                    AlertDialog(
                        onDismissRequest = { showDisconnectDialog = false },
                        title = { Text("Disconnect Device?") },
                        text = { Text("This device will be unpaired and you'll need to scan for it again to reconnect.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.destroy()
                                showDisconnectDialog = false
                            }) {
                                Text("Disconnect")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDisconnectDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if(isConnected) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Text("TRANSACTION")
                    TransactionSection(
                        viewModel,
                        cardColor,
                        amount,
                        onAmountChange = { amount = it },
                        onTransactionClick = { type ->
                            viewModel.transact(amount, type)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text("LOGS")
                LogSection(
                    cardColor,
                    logs = viewModel.logText,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if(showBottomSheet) {
            BluetoothDeviceSheet(
                viewModel = viewModel,
                onDismiss = {showBottomSheet = false})
        } else if (configurationSheet) {
            ConfigurationSheet(
                viewModel = viewModel,
                onDismiss = { configurationSheet = false }
            )
        } else if(otherSheet) {
            OtherSheet(
                viewModel = viewModel,
                onDismiss = { otherSheet = false }
            )

        }
    }
}

@Composable
fun Progress(
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Text(
                        text = "Cancel"
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionStatus(
    viewModel: TransactionsViewModel,
    status: String
) {
    LaunchedEffect(Unit) {
        delay(3000)
        viewModel.endStatus()
    }

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
            Button(
                onClick = { viewModel.endStatus() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                shape = RoundedCornerShape(7.dp)
            ) {
                Text(
                    text = status
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    viewModel: TransactionsViewModel,
    menuExpanded: Boolean,
    onConfigurationSheetChange: (Boolean) -> Unit,
    onOtherSheetChange: (Boolean) -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Finix + PAX D135")
            }
        },
        actions = {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) }
            ) {
                val menuItems = listOf("Configurations", "Reset Device", "Reset Files", "Others")
                menuItems.forEach { menuItem ->
                    DropdownMenuItem(
                        onClick =
                            {
                                onMenuExpandedChange(false)
                                when (menuItem) {
                                    "Configurations" -> {
                                        onConfigurationSheetChange(true)
                                    }
                                    "Others" -> {
                                        onOtherSheetChange(true)
                                    }
                                    "Reset Device"   -> viewModel.resetDevice()
                                    "Reset Files"    -> viewModel.resetFiles()
                                }
                            },
                        text = { Text(menuItem) }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun DeviceSection(
    viewModel: TransactionsViewModel,
    cardColor: Color,
    permissionsAccepted: Boolean,
    isConnected: Boolean,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    if(isConnected) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(cardColor)
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    viewModel.deviceName.value ?: "",
                    modifier = Modifier.weight(2f).padding(start = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(.5.dp)
                        .background(Color.Gray)
                )
                TextButton(
                    onClick = onDisconnectClick,
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape
                ) {
                    Text("Disconnect")
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(cardColor)
                .padding(12.dp)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                shape = RoundedCornerShape(7.dp),
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.light_blue),
                    contentColor = Color.White
                ),
                enabled = permissionsAccepted
            ) {
                Text("Scan for device")
            }
        }
    }
}

@Composable
fun TransactionSection(
    viewModel:TransactionsViewModel,
    cardColor: Color,
    amount: String,
    onAmountChange: (String) -> Unit,
    onTransactionClick: (TransactionType) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(cardColor)
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amount:",
                    modifier = Modifier.padding(end = 8.dp)
                )

                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .border(1.dp, Color.Gray, RectangleShape),
                    singleLine = true,
                    prefix = {
                        Text("$")
                    },
                    placeholder = {Text("0")},
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }

            OtherConfig(viewModel)

            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val transactionTypes: List<Pair<String, TransactionType>> = listOf(
                    "Sale" to TransactionType.SALE,
                    "Auth" to TransactionType.AUTHORIZATION,
                    "Refund" to TransactionType.REFUND
                )

                transactionTypes.forEach {(label, type) ->
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onTransactionClick(type) },
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun OtherConfig(viewModel: TransactionsViewModel) {
    val tagValue by viewModel.tags.collectAsState()
    val splits by viewModel.splitMerchants.collectAsState()

    Column {
        if(tagValue.isNotEmpty()) {
            Text("\nTags: " + tagValue)
        }

        if(splits.isNotEmpty()) {
            splits.forEachIndexed { inxex, it ->
                if(it.merchantId.isNotEmpty() && it.amount>0) {
                    if(inxex == 0) {
                        Text("Split Merchants:")
                    }
                    val amountFormatted = String.format("%.2f", it.amount / 100.0)
                    val feeFormatted = it.fee?.takeIf { it > 0 }?.let { fee ->
                        ", Fee: ${String.format("%.2f", fee / 100.0)}"
                    } ?: ""

                    Text("Merchant: ${it.merchantId}, Amount: $amountFormatted$feeFormatted")
                }
            }
        }
    }
}

@Composable
fun LogSection(
    cardColor: Color,
    logs: String,
    onClearLogs: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(cardColor)
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Text(
            text = logs,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        )
    }

    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 30.dp, top = 5.dp, bottom = 15.dp)
            .height(45.dp),
        onClick = onClearLogs,
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.primary
        )
    ) {
        Text("Clear")
    }
}

@Preview(showBackground = true)
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun HomeViewsPreview() {
    MPOSSampleApplicationTheme {
        MainViews(TransactionsViewModel(context = LocalContext.current, ConfigPrefs()), true, true)
    }
}
