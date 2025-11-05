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

`com.finix:pax-mpos-sdk-android:3.3.4`

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
    // Merchant Id from Finix starting with MUxxxx
    val merchantId: String,
    
    // Finix mid (GUID representation)
    val mid: String,
    
    // Device ID - device registers with Finix, starting with DVxxxx
    val deviceId: String,
    
    val currency: Currency = Currency.USD,
    
    // Sandbox or Production
    val env: EnvEnum = EnvEnum.SB,
    
    // ROLE_MERCHANT or ROLE_PARTNER UserId
    val userId : String,
    
    //  ROLE_MERCHANT or ROLE_PARTNER password
    val password: String
)

Example : val mpos = MPOSFinix(
    context,
    MerchantData(
        merchantId = "MUxxxxxx",
        mid = "",
        deviceId = "DVxxxxx",
        env = EnvEnum.SB,
        userId = "USxxxxxxxxx",
        password = ""
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
        transactionType: TransactionType
        transactionCallback: MPOSTransactionCallback,
        configureEMVResponseCallback: MPOSEMVProcessingCallback,
        splitTransfers: List<SplitTransfer>? = null,
        tags: Map<String, String>? = null
    )

amount : amount in cents for \$10.10 this value would be 1010
transactionType : TransactionType enum with support for only Sale, Refund and Authorization
transactionCallback : Exposes functions which are invoked with status of the transaction or the Transaction Result
configureEMVResponseCallback: Exposes functions which are invoked to with status of authorizing a card insert
splitTransfers: Optional list of split transfer request data
tags: Optional map of key/value strings to be provided in the request

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
    tags: Map<String, String>? = null
)

transactionId : The transaction id specified correlating to a previous transaction. The ID is returned as part of the TransactionResult
in the above start transaction
refundAmount : amount in cents for \$10.10 this value would be 1010
refundCallback : Exposes functions which are invoked with status of the refund or the Refund Result
```

#### Sample App

Please see the sample app to see how to interact with the SDK. The App works as follows:

1. Ensure the mPOS device (D135) has been paired to your phone/tablet via bluetooth.
2. Launch the app
3. To connect the app to the device, first click the `Scan for Devices` button. This will scan for
   currently paired bluetooth devices.
4. Once the button is clicked a dialog would open showing a list of currently paired bluetooth
   devices
5. Select the mPOS device. This will begin the process of connecting to the device and a loading
   spinner will be shown. You can observe the log output on the screen as it outputs messages of
   current progress
6. Once the connection is complete and successful, one can initiate transactions by typing the
   amount into the `TextField` with the default value of `3.14` and clicking one
   of `Sale, Auth, Refund`

Note: The app requires the addition of retrofit as a dependency due to proguard issues with the AAR

