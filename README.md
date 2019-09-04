# Singular Key Android FIDO2 Demo

This project demonstrates the registration and use of a FIDO2 credential in an Android App. It uses Android's FIDO2 native API and Singular Key's FIDO2 Cloud Service. FIDO2 Credentials are phishing resistant, attested public key based credentials for strong authentication of users.
The demo supports Android Platform authenticator using fingerprint/screen lock and BLE,NFC,USB based security Keys (roaming authenticators)

This demonstration requires Demo RP (Relying Party) Server (https://github.com/singularkey/webauthndemo) which communicates to Singular Key's FIDO2 Cloud Service. Please contact support (`support@singularkey.com`) for your `free Api Key`.

------------

### Dependencies
* Android 7+ device with latest updates with a registered fingerprint. Screenlock can be used as well.
* RP Server (https://github.com/singularkey/webauthndemo) - e.g. https://api.yourcompany.com
* Singular Key API Key

### Install
```sh
git clone https://github.com/singularkey/androidfido2demo.git
```

### Configure

##### RP Server  (https://github.com/singularkey/webauthndemo)

* View Readme.md for https://github.com/singularkey/webauthndemo to install and configure the RP Server
*  The Android FIDO2 App needs to communicate with the RP Server on `https://`, so you will either need to front the Node RP     Server with a reverse Proxy like Nginx and install the certificate in Nginx or just enable https on the Node Service itself.  In order to enable https on the Node Service, edit `server/config.json
```js
"https":{
    "enabled":false,
    "keyFilePath":"PATH_TO_SSL_KEY_FILE",
    "certFilePath":"PATH_TO_SSL_CERT_FILE"
  }
```

##### Associate your website with your android App  - Update `assetlinks.json`
* To use the FIDO2 API in your android App, you will need to associate your android app with your website. The Android App uses the `RPID` (you'll configure in the next section) to construct a URL to fetch the assetslinks.json file from your RP Server.  The url is `https://<RPID>/.well-known/assetlinks.json`
We have provided you with the assetlinks.json in the RP Server `webapp/.well-known/assetlinks.json`

* Find the SHA256 Fingerprint of the signing certifiate of your android app by executing the following command in your android project. Password for the keystore is `fido2android`
```js
cd app
keytool -exportcert -list -v  -keystore ./keystore.jks
```
* Update the /webapp/.well-known/assetslinks.json file in the RP Server project
```
sha256_cert_fingerprints entry in the assetlinks.json file.
```

##### Android FIDO2 App (This Repository)
#
Edit `MainActivity.kt`
```Js
var RP_SERVER_URL = "ADD_YOUR_RP_SERVER_URL_HERE"; //e.g., https://api.singularkey.com
var RPID = "ADD_YOUR_RPID_HERE"  // e.g., api.yourcompany.com.  RPID is a valid domain string that identifies the WebAuthn Relying Party on whose behalf a given registration or authentication ceremony is being performed. A public key credential can only be used for authentication with the same entity (as identified by RP ID) it was registered with.
```

##### Singular Key FIDO2 Settings  (https://devportal.singularkey.com)
* There are two main settings for the FIDO2 Section in your client app in the Singular Key Admin Portal. Log into the Admin portal using the credentials provided to you.
    * `Supported Origin Domain Name`: In case of Android, this will be the apk-key-hash in the format android:apk-key-hash:<YOUR_APK_HASH> (e.g. android:apk-key-hash:xYvjmzazZxLXDNrFnWUq_EObrht2yX2hfmkrehWrJ5Y). One way to find out this value is to attempt to register a FIDO2 credential using the Android Demo app. You'll see a client mismatch error in the RP Server logs with the android origin value.
    * `Rp Id`: Enable this and update the value with the RPID used in the Android Fido2 App. (in the section above)

Click on `Save` towards the bottom of the Fido2 settings form to persist your changes.

### Run
Build your Android App and install it on an android device. Below is a demonstration of the functionality:


<img src="https://singularkey.s3-us-west-2.amazonaws.com/androidfido2.gif" width="50%" height="50%" />

### Architecture
`Android FIDO2 Demo App` --> `RP Server (Default Port 3001)` API --> `Singular Key's FIDO Cloud Service`

### Key Files
 * `MainActivity.kt`  : Check out https://webauthn.singularkey.com/ for FIDO2 Sequence Diagrams.
    * FIDO2 Registration Steps:
        *  1. `fido2RegisterInitiate()` : Relying Party (RP) Server API call which is proxied to Singular Key FIDO Service to initiate the FIDO2 registration process to retrieve a randomly generated challenge and other RP and User information
        *  2. `fido2AndroidRegister()` : Android Attestation API call to create a fingerprint/screenlock secured public key based strong `FIDO2 credential`and sign the response (`public key`, challenge and other information)
        *  3. `fido2RegisterComplete()` : The signed response is sent to the RP Server API which is proxied to Singular Key FIDO Service to complete the FIDO2 registration process
    * FIDO2 Authentication Steps:
        *   `fido2AuthInitiate()` : RP Server API call which is proxied to Singular Key FIDO Service to initiate the FIDO2 Authentication process to retrieve a randomly generated challenge and other information
        *   `fido2AndroidAuth()` : Android Assertion API call to create a signed response (challenge and other information) with the previously created FIDO2 Credential.
        *   `fido2AuthComplete()` : The signed response is sent to the RP Server API  which is proxied to Singular Key FIDO Service to complete the FIDO2 Authentication process

 * `RPApiService.kt` - RP API Interface
    * POST /register/initiate
    * POST /register/complete
    * POST /auth/initiate
    * POST /auth/complete

------------
# Support
Have questions? Please contact Support (`support@singularkey.com`) or sign up at http://singularkey.com/singular-key-web-authn-fido-developer-program-api/

# License
Apache 2.0