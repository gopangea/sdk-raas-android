# SDK-RaaS-Android

**The following guide explains how to install and implement Pangea RaaS' Mobile SDK into your application environment.** 
- Version number: 1.0.2

## [Changelog](https://github.com/gopangea/sdk-raas-android/blob/master/CHANGELOG.md)

### Introduction


The Mobile SDK is analogous to [Pangea's Javascript](https://connect-raas-api.pangeamoneytransfer.com/?java#pangea-js-library) code used for fraud review enhancement on the browser and Funding card tokenization.

Pangea requires the use of our SDK to mitigate fraud and to securely collect debit card information for payment. 
Include this library in your android application using the following instructions.


**To be able to use Pangea RaaS SDK in your application, Pangea must add your hostname to the CORS whitelist.**


### Instructions:

Application is allowed to open network sockets, i.e. should have this line in the

manifest file:
```
<uses-permission android:name="android.permission.INTERNET" />
```

In the app's build.gradle file.
```
minSdkVersion >= 21 
```

### Installation:
* Add the following line to your app's build.gradle file, under the dependencies
section:
```
implementation 'com.github.gopangea:sdk-raas-android:currentVersion'
```
* Add it in your root build.gradle at the end of repositories:
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
* Add the following line to your app's build.gradle file, inside of the android section :
```
compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    // For Kotlin projects
    kotlinOptions {
        jvmTarget = "1.8"
    }
```
* Perform a clean build using Android Studio or in the terminal: 
```
./gradlew clean build
```

### Implementation:

1. Declare the intance and initialize the pangea Session at the end of your main activity's onCreate() method:
* Kotlin
```
private lateinit var mPangea: Pangea
mPangea = Pangea.createSession(context = applicationContext,  debugInfo = false, pangeaSessionID = "YOUR ID IDENTIFIER PER USER HERE 
", environment = Environment.INTEGRATION)
```
* Java
```
private lateinit var mPangea: Pangea
mPangea = Pangea.Companion.createSession(getApplicationContext(),false, "YOUR ID IDENTIFIER PER USER HERE",
                Pangea.Environment.DEV);
```

* createSession() arguments:
  - context - Application Context.
  - pangeaSessionID - Unique String identifier of the user's current session in the app. The same identifier is passed by your backend to Pangea.
  - debugInfo - Boolean parameter that enables pangea logging for debugging.
  - environment - Use it to point to the desired environment, there are three: *PRODUCTION*, *DEV* and *INTEGRATION*
  
2. if you need to retrive the session from mPangea use the following function:
```
mPangea.getSessionId()
```

3. When the user's session changes, update the token by calling:
```
mPangea.updateSessionToken("newToken");
```
4. Notify to pangea on each relevant HTTP request from the app:
```
mPangea.logRequest("https://<request URL>");
```
5. Collect rich device information. This method should usually be called only once per session:
```
mPangea.logSensitiveDeviceInfo();
```
6. (Recommended if your app requests location permissions) Add the following call to the onStop() method in
the app's main activity:
```
mPangea.removeLocationUpdates();
```

### Funding card tokenization:

Pangea RaaS Android sdk must also be used for generating a temporary token representing a sender’s funding card. Pangea retrieves the token by first encrypting the card data using your public key then sends it to Pangea’s server. You will send the temporary token to your server and finally over to the Pangea RaaS API to create a funding card. Pangea uses the temporary token to generate a permanent token with our card payments provider. The card number and cvv is never stored anywhere in Pangea. You will use the same permanent token for each transfer made with that card. The temporary token is valid for 24 hours. Upon successful generation of the temporary token, you can safely send it to your server. Then to create the funding method in the RaaS API, provide the token along with the other required fields: 

```
mPangea.createToken(card: CardInformation, callBack: CallBack<TokenResponse>) 
```
**The structure of Card information is:**

```
data class CardInformation(
    var publicKey: String,
    var partnerIdentifier: String,
    var cardNumber: String,
    var cvv: String,
)
```
* CardInformation() arguments:
  - publicKey - Unique RAS pubic key identifier, provided to you by Pangea, pass this argument as a String with the key only without any blank/enter/tab/etc space, a valid string could be: 
  
  ```
  "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq7BL3rzZbWx6xDmxDxozfUhoJ2xJawfKoGqBgqUa+ZTWUYUtkrCMuS3l8bKZZij4MQQmFb4vvIUJ0AoY0aVK59uxom1MEA9X89Vaz0Ctv5TNdjm7NQN3oosdtKeMd7g1fAxBXoR2XdShM9Nq0IjNHgWbbgFlq4CTKdPyG7N/M5eAnSjDOO9xIADZ9DsWGk3TgZGKbr36EJGYfT8R1E/l+/2YRLVlKf/lLGkl0LSPJ+kv4icB7i48v2GTTAyRs04oFPc9xB/JdoCxCtUmaIcy
  vsjavj9MxRZ3ubOFLNdh8SJ3GmVgRMndxvJGKAVAeURP4eGFK9btnLan9Kzt6BXcFQIDAQAB"
  ```

  - partnerIdentifier -  This is the name of the shop
  - cardNumber - A valid card number, this number will be encryped and send to the pangea server
  - cvv -  A cvv number, this number will be encryped and send to the pangea server
  
You’ll have a different public key for both sandbox and production. The partnerIdentifier will be the same in both environments and will be assigned to you.

**CallBack interface**

This interface will let you retrieve the token from pagea when the service call is completed, it has two methods
  - fun  onResponse(result: T)
  - fun  onFailure(result: T, throwable: Throwable?)

You can use something similar to this:

* Kotlin

```
 mPangea.createToken(cardInformation, object : CallBack<TokenResponse> {
            override fun onResponse(tokenResponse: TokenResponse) {
                Log.e(TAG, "onResponse: The token is; ${tokenResponse.token}" )
                //do something with your token
            }
            override fun onFailure(tokenResponse: TokenResponse, throwable: Throwable?) {
                Log.e(TAG, "the error is ${throwable?.localizedMessage}" )
                 //failed request
            }
        })
```
* Java

```
mPangea.createToken(cardInformation, new CallBack<TokenResponse>() {
          @Override
          public void onResponse(TokenResponse tokenResponse) {
              Log.e(TAG, "onResponse: The token is: " + tokenResponse.token);
              //do something with your token
          }

          @Override
          public void onFailure(TokenResponse tokenResponse, @Nullable Throwable throwable) {
              Log.e(TAG, "the error is: " + throwable.getLocalizedMessage());
              //failed request
          }
      });
```
TokenResponse is just a wrapper for a String

**Client Session Data**

From version 1.0.2 we introduced a new method to get a base64 encoded string which the RaaS partner will store in their database instead of the clientSessionId. This encoded string is a JSON object containing some platform RaaS identifiers and the client session id provided by your implementation.
```
mPangea.getClientData(callback :CallBack<String>)
```

This id is the same that the sessionID, if you already provided one is not necessary to provide another before calling this method, (when you create your pangea instance you pass this ID as the parameter pangeaSessionID)

You can use something similar to this to retrive your encoded client session data: 

* Kotlin
```
pangea.getClientData(new CallBack<String>() {
            @Override
            public void onResponse(String clientInfo) {
                System.out.println(clientInfo);
                //do something with your clientInfo
            }

            @Override
            public void onFailure(String result, @Nullable Throwable throwable) {
                System.out.println("failed: " + throwable.getLocalizedMessage());
                //failed request
            }
        });
```

* Java
```
pangea.getClientData(object :CallBack<String>{
    override fun onResponse(clientInfo: String) {
        println(clientInfo)
        //do something with your clientInfo

    }
    override fun onFailure(result: String, throwable: Throwable?) {
        println("failed: ${throwable?.localizedMessage}")
        //failed request
    }
})
```
### Error handling

From version 1.0.3 we introduced an enhanced error descriptor with tree optionals:raw, body, and bodyError.

It has been implemented to give you more info to help you out to debug a possible error request when you invoke these 2 methods:

```
createToken(card: CardInformation, callBack: CallBack<TokenResponse>) 
getClientData(callback :CallBack<String>)
```

### FAQs

Q: Does the SDK require any external frameworks or additional permissions from the user?
* The SDK makes use of 2 lightweight Google Play Services libraries (ads-identifer
and safetynet) as detailed in the SDK's pom.xml.
* The SDK does not require or prompt for any user permissions.
* The SDK utilizes certain permissions if the host app previously requested them.

Q: When and how should logRequest() be used?
* Call this method every time a user performs a meaningful action inside the flow of the remittance (for
example: start a new remittance , search a contact, finalize the remittance).
* Pangea uses this data for behavioral modeling and analysis, so please take care
to only call logRequest() in response to an actual user action.
* The single URL argument represents the action taken, this can usually be
achieved by passing the same URL used in the backend call for the action (ie.,
https://api.myshop.com/remittance/).

Q: When should logsensitiveDeviceInfo() be called?
* Ideally once per session, depending on the use case. For example - before
attempting to process a remmitance.
* Multiple invocations of this function in a single session will not generate an error,
but have the wasteful effect of generating identical payloads for analysis.




