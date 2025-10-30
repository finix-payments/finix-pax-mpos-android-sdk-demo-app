package com.finix.mpossampleapplication.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.finix.mpos.models.SplitTransfer
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel
import com.finix.mpossampleapplication.utils.SplitTransferValidator
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSheet(
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasInitialized by remember { mutableStateOf(false) }

    val observedTags by viewModel.tags.collectAsState()
    val observedSplits by viewModel.splitMerchants.collectAsState()

    var tags by remember { mutableStateOf("") }
    val splitMerchants = remember { mutableStateListOf<SplitTransfer>() }

    var isSplitChecked by remember { mutableStateOf(false) }

    var showAddEditSplitForm by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (!hasInitialized) {
            tags = observedTags
            splitMerchants.clear()
            if (observedSplits.isNotEmpty()) {
                splitMerchants.addAll(observedSplits.map { it.copy() })
                isSplitChecked = true
            } else {
                splitMerchants.add(SplitTransfer(viewModel.merchantData.value.merchantId, 0))
                isSplitChecked = false
            }
            hasInitialized = true
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text("Others", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        viewModel.saveTags(tags)
                        if (isSplitChecked && splitMerchants.isNotEmpty()) {
                            viewModel.saveSplitData(splitMerchants)
                        } else {
                            viewModel.clearSplitData()
                        }
                        onDismiss()
                    }
                ) {
                    Text("Save")
                }
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags") },
                placeholder = { Text("Key:value") },
                isError = !viewModel.isValidKeyValueFormat(tags) && tags.isNotEmpty(),
                supportingText = {
                    if (tags.isNotEmpty() && !viewModel.isValidKeyValueFormat(tags)) {
                        Text("Invalid format. Use key:value, key2:value2")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Split toggle ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text("Split Transfer")
                Spacer(Modifier.width(5.dp))
                Checkbox(
                    checked = isSplitChecked,
                    onCheckedChange = { checked ->
                        isSplitChecked = checked
                        if (!checked) splitMerchants.clear()
                        else if (splitMerchants.isEmpty()) {
                            splitMerchants.add(
                                SplitTransfer(viewModel.merchantData.value.merchantId, 0)
                            )
                        }
                    }
                )
            }

            if (isSplitChecked) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .padding(vertical = 8.dp)
                ) {
                    itemsIndexed(splitMerchants) { index, row ->
                        SplitMerchantItem(
                            index = index,
                            merchantData = row,
                            onDelete = { i ->
                                if (splitMerchants.size == 1) {
                                    splitMerchants.clear()
                                    isSplitChecked = false
                                } else {
                                    splitMerchants.removeAt(i)
                                }
                            },
                            onClick = {
                                selectedIndex = index
                                showAddEditSplitForm = true
                            }
                        )
                    }

                    item {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedIndex = splitMerchants.size
                                showAddEditSplitForm = true
                            }
                        ) {
                            Text("+ Add Split Transfer")
                        }
                    }
                }
            }
        }
    }

    if (showAddEditSplitForm) {
        splitMerchantDialog(
            index = selectedIndex,
            showDialog = true,
            initialMerchant = if(selectedIndex == splitMerchants.size) SplitTransfer("", 0) else splitMerchants.get(selectedIndex),
            onDismiss = { showAddEditSplitForm = false },
            onSave = { updatedMerchant ->
                if (selectedIndex < splitMerchants.size) {
                    splitMerchants[selectedIndex] = updatedMerchant
                } else {
                    splitMerchants.add(updatedMerchant)
                }
                showAddEditSplitForm = false
            }
        )
    }
}


@Composable
fun splitMerchantDialog(
    index: Int,
    showDialog: Boolean,
    initialMerchant: SplitTransfer,
    onDismiss: () -> Unit,
    onSave: (SplitTransfer) -> Unit
) {
    if (showDialog) {
        var editableMerchant by remember { mutableStateOf(initialMerchant) }
        var validationErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Split Merchant") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SplitMerchantForm(
                        index = index,
                        merchantData = editableMerchant,
                        isLastItem = false,
                        onChange = { _, updated -> editableMerchant = updated },
                        validationErrors = validationErrors
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = SplitTransferValidator.validateMerchantConfig(editableMerchant)
                        if (result.isValid) {
                            onSave(editableMerchant)
                            onDismiss
                        } else {
                            validationErrors = result.errors
                        }
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SplitMerchantForm(
    index: Int,
    merchantData: SplitTransfer,
    isLastItem: Boolean,
    onChange: (Int, SplitTransfer) -> Unit,
    validationErrors: Map<String, String> = emptyMap()
) {
    var tagsInput by rememberSaveable { mutableStateOf(
        merchantData.tags.orEmpty().entries.joinToString(", ") { "${it.key}: ${it.value}" }
    ) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        OutlinedTextField(
            value = merchantData.merchantId,
            onValueChange = { onChange(index, merchantData.copy(merchantId = it)) },
            label = { Text("Merchant ID") },
            enabled = index > 0,
            isError = validationErrors.containsKey("merchantId") ,
            supportingText = {
                validationErrors["merchantId"]?.let { Text(it, color = Color.Red) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            AmountInputField(
                modifier = Modifier.weight(1f),
                label = "Amount",
                valueInCents = merchantData.amount,
                onValueChangeInCents = { newAmount ->
                    onChange(index, merchantData.copy(amount = newAmount ?: 0L))
                },
                validationErrors = validationErrors
            )

            AmountInputField(
                modifier = Modifier.weight(1f),
                label = "Fee",
                valueInCents = merchantData.fee,
                onValueChangeInCents = { newFee ->
                    onChange(index, merchantData.copy(fee = newFee))
                },
                validationErrors = validationErrors
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { input ->
                    tagsInput = input
                    val map = input.split(",")
                        .mapNotNull { entry ->
                            val parts = entry.split(":").map { it.trim() }
                            if (parts.size == 2 && parts[0].isNotEmpty()) parts[0] to parts[1] else null
                        }.toMap()
                    onChange(index, merchantData.copy(tags = map))
                },
                label = { Text("Tags") },
                placeholder = { Text("key: value") },
                isError = validationErrors.containsKey("tags"),
                supportingText = {
                    validationErrors["tags"]?.let { Text(it, color = Color.Red) }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SplitMerchantItem(
    index: Int,
    merchantData: SplitTransfer,
    onDelete: (Int) -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Merchant ${index + 1}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = TextDecoration.Underline
                ))

            if (index > 0) {
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.padding(start = 3.dp).height(20.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Row")
                }
            }
        }
        Column() {
            Text(text = "Merchant ID: ${merchantData.merchantId}")
            val decimalFormat = DecimalFormat("#,##0.00")

            Text(
                text = buildString {
                    val amountValue = (merchantData.amount.takeIf { it > 0 } ?: 0) / 100.0
                    append("Amount: $${decimalFormat.format(amountValue)}")

                    val feeValue = (merchantData.fee ?: 0L) / 100.0
                    if (feeValue > 0) {
                        append(", Fee: $${decimalFormat.format(feeValue)}")
                    }
                }
            )

            if(merchantData.tags?.isNotEmpty() == true) {
                Text(text = "Tags: ${merchantData.tags!!.entries.toString()}")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Split Transfer") },
            text = { Text("Are you sure you want to delete this Merchant?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(index)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AmountInputField(
    modifier: Modifier = Modifier,
    label: String,
    valueInCents: Long?,
    onValueChangeInCents: (Long?) -> Unit,
    validationErrors: Map<String, String> = emptyMap()
) {
    var amountInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(key1 = valueInCents) {
        if ((amountInput.isEmpty() || amountInput == "0.0") && valueInCents != null && valueInCents != 0L) {
            amountInput = "%.2f".format(valueInCents / 100.0)
        }
    }

    OutlinedTextField(
        value = amountInput,
        onValueChange = { input ->
            val filtered = input.filterIndexed { idx, c ->
                c.isDigit() || (c == '.' && input.indexOf('.') == idx)
            }

            amountInput = filtered

            val decimalValue = filtered.toDoubleOrNull()
            val cents = decimalValue?.let { (it * 100).toLong() }

            onValueChangeInCents(cents)
        },
        placeholder = { Text("0.0") },
        prefix = { Text("$") },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal
        ),
        isError = validationErrors.containsKey(label.lowercase()) ,
        supportingText = {
            validationErrors[label.lowercase()]?.let { Text(it, color = Color.Red) }
        },
        modifier = modifier
    )
}


