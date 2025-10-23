package com.finix.mpossampleapplication.ui.views

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finix.mpos.models.SplitTransfer
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel
import com.finix.mpossampleapplication.ui.viewModels.copyWith
import com.finix.mpossampleapplication.utils.ConfigValidator
import com.finix.mpossampleapplication.utils.SplitTransferValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSheet(
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasInitialized by remember { mutableStateOf(false) }

    val observedTags by viewModel.tags.collectAsState()
    var tags by remember { mutableStateOf("") }

    val observedSplits by viewModel.splitMerchants.collectAsState()
    val splitMerchants = remember { mutableStateListOf<SplitTransfer>() }
    var isSplitChecked by remember { mutableStateOf(false) }


    var splitValidationErrors by remember { mutableStateOf<Map<Int, Map<String,String>>>(emptyMap()) }


    LaunchedEffect(observedTags, observedSplits) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onDismiss.invoke() }) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Others",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        var hasErrors = false

                        viewModel.saveTags(tags)

                        if (isSplitChecked) {
                            val allErrors = mutableMapOf<Int, Map<String, String>>()
                            splitMerchants.forEachIndexed { index, row ->
                                val validation = SplitTransferValidator.validateMerchantConfig(row)
                                if (!validation.isValid) {
                                    allErrors[index] = validation.errors
                                    hasErrors = true
                                }
                            }

                            if (!hasErrors) {
                                viewModel.saveSplitData(splitMerchants)
                                onDismiss()
                            } else {
                                splitValidationErrors = allErrors
                            }
                        } else {
                            viewModel.clearSplitData()
                            onDismiss()
                        }
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
                    if (tags.isNotEmpty() && viewModel.isValidKeyValueFormat(tags)) {
                        Text("Invalid format. Use key:value, key2:value2")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text(text = "Split Transfer")
                Spacer(modifier = Modifier.width(5.dp))
                Checkbox(
                    checked = isSplitChecked,
                    onCheckedChange =
                        {
                            isSplitChecked = it
                            splitMerchants.clear()
                            splitMerchants.add(SplitTransfer(merchantId = viewModel.merchantData.value.merchantId, 0))
                        },
                )
            }

            if (isSplitChecked) {
                splitMerchants.forEachIndexed { index, row ->
                    SplitMerchantRow(
                        index = index,
                        merchantData = row,
                        isLastItem = index == splitMerchants.lastIndex,
                        onAddClick = {
                            if(viewModel.isValidSplitMerchants(splitMerchants)) {
                                splitMerchants.add(SplitTransfer("", 0))
                            } else {
                                Toast.makeText(context, "ID or Amount missing for some merchants!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onChange = { i, updatedRow ->
                            splitMerchants[i] = updatedRow
                            splitValidationErrors = splitValidationErrors - i // Clear only this row's errors
                        },
                        onDelete = { i ->
                            if(splitMerchants.size == 1) {
                                splitMerchants.clear()
                                isSplitChecked = false
                            } else if (splitMerchants.size > 1) {
                                splitMerchants.removeAt(i)
                            }
                        },
                        validationErrors = splitValidationErrors[index] ?: emptyMap()
                    )
                }
            }
        }
    }
}

@Composable
fun SplitMerchantRow(
    index: Int,
    merchantData: SplitTransfer,
    isLastItem: Boolean,
    onAddClick: () -> Unit,
    onChange: (Int, SplitTransfer) -> Unit,
    onDelete: (Int) -> Unit,
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

            if (index > 0) {
                IconButton(
                    onClick = { onDelete(index) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Row")
                }
            }
        }

        if(isLastItem) {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Row")
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
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


