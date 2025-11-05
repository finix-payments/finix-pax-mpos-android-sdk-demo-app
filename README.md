# Finix PAX MPOS Android SDK Demo App

## Overview

### Documents for Java/Android developers

This guide is a quick start guide for adding **FINIX PAX MPOS SDK** to Java/Android applications.

### Get started

[Integrate your App with POSLink](#integrate-mpos-sdk)

[Sample code](#sample-app)

#### Integrate MPOS SDK

##### Installation

Add the mPOS SDK dependency to your gradle file. See [Maven Central](https://central.sonatype.com/artifact/com.finix/pax-mpos-sdk-android) or [MVN Repository](https://mvnrepository.com/artifact/com.finix/pax-mpos-sdk-android) for the latest version.

`com.finix:pax-mpos-sdk-android:2.1.1`

##### Initialization

Add permission to AndroidManifest to interact with mPOS device via Bluetooth

```xml
   <!-- For network communication -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- For bluetooth communication -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /><!-- TargetSdkVersion greater than or equal to 31 -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

To interact with the MPOS device, it first needs to be initialized. This can be done by initializing
the class `MPOSFinix`

```
class MPOSFinix(private val context: Context, private val merchantData: MerchantData)

context: Android application context
merchantData: Specific values which help identify the merchant

class MerchantData (
    val merchantId: String,     //Merchant Id from Finix starting with MUxxxx
    val mid: String,            //Finix mid (GUID representation)
    val deviceId: String,       //Device ID - device registers with Finix
    val currency: String = "USD",
    val env: EnvEnum = EnvEnum.SB,   // SB = sandbox or  PROD = production
    val userId : String, // Role Partner UserId
    val password: String //  Role partner password
)

Example : val mpos = MPOSFinix(
  context,
  MerchantData(
        merchantId = "MUrc",
        mid = "3f4-b8f-40",
        deviceId = "DVvyDYD",
        env = EnvEnum.SB,
        userId = "US9jv",
        password = "8c5-046-40c"
  )
)
```

Once initialized, connect to the device. When the device is ready for pairing, it will show an
orange light and green light. Once connected, the orange light will disappear and only the green
light will stay on.

```
fun connect(deviceName: String, deviceAddress: String, callback: MPOSConnectionCallback)

deviceName : bluetooth name of the device 
deviceAddress : bluetooth address of the device 
callback: exposes functions which are invoked to propagate success, error, progress

interface MPOSConnectionCallback {
    fun onSuccess() // Called if the connection is successfully established
    fun onError(errorMessage: String) // Called if the connection fails for some reason
    fun onProcessing(currentStepMessage: String) // Provides status messages of what's currently happening
}
```

##### Start Transaction

To start a transaction, the function `startTransaction()` must be called. This will prep the mPOS
device to accept card input (swipe, tap, insert). A blue status light will be shown. Once any action
is performed with the card the device would show a red light to indicate, the card has been read and
to remove it.

```
fun startTransaction(
     amount: Long,
     transactionType: TransactionType,
     transactionCallback: MPOSTransactionCallback,
     configureEMVResponseCallback: MPOSEMVProcessingCallback,
     splitTransfers: List<SplitTransfer>? = null,
     tags: Map<String, String>? = null
 )
amount : amount in cents for $10.10 this value would be 1010
transactionType: SALE, AUTH, REFUND (one of these values should be passed)
transactionCallback : Callback interface that provides updates on the transaction status or the TransactionResult
configureEMVResponseCallback: Exposes functions which are invoked to with status of authorizing a card insert
splitTransfers: Optional list of split merchant details to be passed if split transfers are enabled
tags: A list of key–value pairs for attaching additional metadata to the transaction

interface MPOSTransactionCallback {
    fun onSuccess(result: TransactionResult?) // Called if the transaction is successfully processed
    fun onError(errorMessage: String) // Called if the transaction fails for any reason
    fun onProcessing(currentStepMessage: String) // Provides status messages of what's currently happening
}

interface MPOSEMVProcessingCallback {
    fun onError(errorMessage: String) // Called if the authorization of chip insert fails for any reason
    fun onProcessing(currentStepMessage: String) // Provides status messages of what's currently happening
}
```

##### Referenced Refund

A referenced refund, refunds the amount specified (refund amount) of a particular transaction (transaction id)
to the card on file

```
fun startRefund(
    transactionId: String,
    refundAmount: Long,
    refundCallback: MPOSRefundCallback
)

transactionId : The transaction id specified correlating to a previous transaction. The ID is returned as part of the TransactionResult
in the above start transaction
refundAmount : amount in cents for $10.10 this value would be 1010
refundCallback : Exposes functions which are invoked with status of the refund or the Refund Result
```

#### Sample App

Please see the sample app to see how to interact with the SDK. The App works as follows:

1. Pair the mPOS device (D135): Ensure the D135 device is paired with your phone/tablet via Bluetooth.
2. Launch the app: Open the application on your device. 
3. Scan for devices: Tap `Scan for Devices` to search for currently paired Bluetooth devices.
4. Select the mPOS device: A dialog will appear listing paired D135 devices. Select your device to start the connection process.
A loading spinner will appear, and you can monitor connection progress through the on-screen logs.
5. Configure merchant data: Open the Configuration menu, enter the merchant details, and tap Save. 
6. Configure optional data: Open the Others menu, enter the tag and split transfer details, and tap Save.
7. Initiate a transaction: Enter an amount in the TextField (default: `3.14`) and choose one of the following actions: `Sale, Auth, Refund`.

Note: The app requires the addition of retrofit as a dependency due to proguard issues with the AAR

