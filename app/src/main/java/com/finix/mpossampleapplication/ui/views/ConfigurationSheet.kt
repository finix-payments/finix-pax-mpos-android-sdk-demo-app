package com.finix.mpossampleapplication.ui.views

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.finix.mpos.models.EnvEnum
import com.finix.mpos.models.MerchantData
import com.finix.mpossampleapplication.R
import com.finix.mpossampleapplication.ui.viewModels.TransactionsViewModel
import com.finix.mpossampleapplication.ui.viewModels.copyWith
import com.finix.mpossampleapplication.utils.ConfigValidator

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ConfigurationSheet(
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val environments = listOf(EnvEnum.PROD, EnvEnum.SB)
    val envDisplayNames = mapOf(
        EnvEnum.PROD to "Production",
        EnvEnum.SB to "Sandbox"
    )

    val merchantData by viewModel.merchantData.collectAsState()
    var selectedEnvironment by remember { mutableStateOf(merchantData.env) }

    val merchantDataMap = remember {
        mutableStateMapOf<EnvEnum, MerchantData>().apply {
            environments.forEach { env ->
                this[env] = viewModel.loadConfigurations(env)
            }
        }
    }

    val currentEditedData = merchantDataMap[selectedEnvironment] ?: merchantData

    var validationErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {

        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp)) {
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
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        merchantDataMap[selectedEnvironment]?.let { data ->
                            val validation = ConfigValidator.validateMerchantConfig(data)

                            if (validation.isValid) {
                                viewModel.saveMerchantData(data.copyWith(env = selectedEnvironment))
                                onDismiss()
                            } else {
                                validationErrors = validation.errors
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            }

            Text("ENVIRONMENT")
            Spacer(modifier = Modifier.height(3.dp))

            Column {
                TabRow(
                    selectedTabIndex = environments.indexOf(selectedEnvironment),
                    modifier = Modifier
                        .fillMaxWidth(),
                    containerColor = Color.LightGray
                ) {
                    environments.forEachIndexed { index, env ->
                        Tab(
                            selected = selectedEnvironment ==  env,
                            onClick = {
                                selectedEnvironment = env
                            },
                            text = { Text(envDisplayNames[env] ?: env.name)  }
                        )
                    }
                }
            }

            MerchantDataForm(
                merchantData = currentEditedData,
                onChange = { updated ->
                    merchantDataMap[selectedEnvironment] = updated
                    validationErrors = emptyMap()
                },
                validationErrors = validationErrors
            )

            LaunchedEffect(Unit) {
                if (!sheetState.isVisible) {
                    sheetState.show()
                }
            }
        }
    }
}


@Composable
fun MerchantDataForm(
    merchantData: MerchantData,
    onChange: (MerchantData) -> Unit,
    validationErrors: Map<String, String> = emptyMap()
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        Spacer(modifier = Modifier.height(16.dp))
        Text("DEVICE")
        OutlinedTextField(
            value = merchantData.deviceId,
            onValueChange = { onChange(merchantData.copyWith(deviceId = it)) },
            label = { Text("ID") },
            isError = validationErrors.containsKey("deviceId"),
            supportingText = {
                validationErrors["deviceId"]?.let { Text(it, color = Color.Red) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("MERCHANT")
        OutlinedTextField(
            value = merchantData.merchantId,
            onValueChange = { onChange(merchantData.copyWith(merchantId = it)) },
            label = { Text("ID") },
            isError = validationErrors.containsKey("merchantId"),
            supportingText = {
                validationErrors["merchantId"]?.let { Text(it, color = Color.Red) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = merchantData.mid,
            onValueChange = { onChange(merchantData.copyWith(mid = it)) },
            label = { Text("MID") },
            isError = validationErrors.containsKey("mid"),
            supportingText = {
                validationErrors["mid"]?.let { Text(it, color = Color.Red) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("API KEY")
        OutlinedTextField(
            value = merchantData.userId,
            onValueChange = { onChange(merchantData.copyWith(userId = it)) },
            label = { Text("Username") },
            isError = validationErrors.containsKey("userId"),
            supportingText = {
                validationErrors["userId"]?.let { Text(it, color = Color.Red) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = merchantData.password,
            onValueChange = { onChange(merchantData.copyWith(password = it)) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            isError = validationErrors.containsKey("password"),
            supportingText = {
                validationErrors["password"]?.let { Text(it, color = Color.Red) }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    val iconRes =
                        if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
    }
}