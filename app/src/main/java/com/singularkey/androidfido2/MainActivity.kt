/*
 * Copyright 2019 Singular Key Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.singularkey.androidfido2

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

//var RP_SERVER_URL = "https://12841968ac5b.ngrok.io";  //e.g., https://api.singularkey.com
var RP_SERVER_URL = "https://webauthndemo.singularkey.com";  //e.g., https://api.singularkey.com

//var RPID = "12841968ac5b.ngrok.io"                     // e.g., api.yourcompany.com
var RPID = "webauthndemo.singularkey.com"                     // e.g., api.yourcompany.com


private const val BASE64_FLAG = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE


class MainActivity : AppCompatActivity() {
    companion object {
        private const val LOG_TAG = "SingularKeyFido2Demo"
        private const val REQUEST_CODE_REGISTER = 1
        private const val REQUEST_CODE_SIGN = 2
        private const val KEY_HANDLE_PREF = "key_handle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Listener for the 2 Buttons - Fido2 Register and Fido2 Login
        registerFido2Button.setOnClickListener { fido2RegisterInitiate() }
        loginFido2Button.setOnClickListener { fido2AuthInitiate() }
    }

    //**********************************************************************************************************//
    //******************************* Android FIDO2 API Response ***********************************************//
    //**********************************************************************************************************//
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(LOG_TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        when (resultCode) {
            RESULT_OK -> {
                data?.let {
                    if (it.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {

                        val errorExtra = data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
                        val authenticatorErrorResponse =
                            AuthenticatorErrorResponse.deserializeFromBytes(errorExtra)
                        val errorName = authenticatorErrorResponse.errorCode.name
                        val errorMessage = authenticatorErrorResponse.errorMessage

                        Log.e(LOG_TAG, "errorCode.name: $errorName")
                        Log.e(LOG_TAG, "errorMessage: $errorMessage")

                        resultText.text =
                            "An Error Occurred\n\nError Name:\n$errorName\n\nError Message:\n$errorMessage"

                    } else if (it.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
                        val fido2Response = data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
                        when (requestCode) {
                            REQUEST_CODE_REGISTER -> fido2RegisterComplete(fido2Response)
                            REQUEST_CODE_SIGN -> fido2AuthComplete(fido2Response)
                        }
                    }
                }
            }
            RESULT_CANCELED -> {
                val result = "Operation is cancelled"
                resultText.text = result
                Log.d(LOG_TAG, result)
            }
            else -> {
                val result = "Operation failed, with resultCode: $resultCode"
                resultText.text = result
                Log.e(LOG_TAG, result)
            }
        }
    }

    //**********************************************************************************************************//
    //******************************* FIDO2 Registration Step 1 ************************************************//
    //******************************* Get challenge from the Server ********************************************//
    //**********************************************************************************************************//
    private fun fido2RegisterInitiate() {

        val result = JSONObject()
        val mediaType = "application/json".toMediaTypeOrNull()

        result.put("username", usernameButton.text.toString())

        //Optional
        val jsonObject = JSONObject()
        //jsonObject.put("authenticatorAttachment","platform")
        jsonObject.put("userVerification", "required")
        result.put("authenticatorSelection", jsonObject)

        val requestBody = RequestBody.create(mediaType, result.toString())

        try {
            RPApiService.getApi().registerInitiate(requestBody)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {

                            var obj = JSONObject(response.body()?.string())
                            var intiateResponse = obj.getJSONObject("initiateRegistrationResponse")
                            val c = intiateResponse?.getString("challenge")
                            val challenge = Base64.decode(c!!, BASE64_FLAG)
                            var rpname = intiateResponse?.getJSONObject("rp")!!.getString("name")
                            var username =
                                intiateResponse?.getJSONObject("user")!!.getString("name")
                            var userId = intiateResponse?.getJSONObject("user")!!.getString("id")

                            var authenticatorAttachement = ""
                            if (intiateResponse.has("authenticatorSelection")) {
                                if (intiateResponse?.getJSONObject("authenticatorSelection")
                                        .has("authenticatorAttachment")
                                ) {
                                    authenticatorAttachement =
                                        intiateResponse?.getJSONObject("authenticatorSelection")
                                            ?.getString("authenticatorAttachment")!!
                                    Log.d(
                                        LOG_TAG,
                                        "authenticatorAttachement $authenticatorAttachement"
                                    )
                                }
                            }

                            val attestation = intiateResponse?.getString("attestation")
                            Log.d(LOG_TAG, attestation)
                            var attestationPreference: AttestationConveyancePreference =
                                AttestationConveyancePreference.NONE
                            if (attestation == "direct") {
                                attestationPreference = AttestationConveyancePreference.DIRECT
                            } else if (attestation == "indirect") {
                                attestationPreference = AttestationConveyancePreference.INDIRECT
                            } else if (attestation == "none") {
                                attestationPreference = AttestationConveyancePreference.NONE
                            }

                            fido2AndroidRegister(
                                rpname,
                                challenge,
                                userId,
                                username,
                                authenticatorAttachement,
                                attestationPreference
                            )
                        } else {
                            resultText.text = response.toString()
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d(LOG_TAG, t.message)
                        resultText.text = t.message
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    //**********************************************************************************************************//
    //******************************* FIDO2 Registration Step 2 ************************************************//
    //******************************* Invoke Android FIDO2 API  ************************************************//
    //**********************************************************************************************************//

    private fun fido2AndroidRegister(
        rpname: String,
        challenge: ByteArray,
        userId: String,
        userName: String?,
        authenticatorAttachment: String?,
        attestationPreference: AttestationConveyancePreference
    ) {

        try {
            val options = PublicKeyCredentialCreationOptions.Builder()
                .setAttestationConveyancePreference(attestationPreference)
                .setRp(PublicKeyCredentialRpEntity(RPID, rpname, null))
                .setUser(
                    PublicKeyCredentialUserEntity(
                        userId.toByteArray(),
                        userId,
                        null,
                        userName
                    )
                )
                .setChallenge(challenge)
                .setParameters(
                    listOf(
                        PublicKeyCredentialParameters(
                            PublicKeyCredentialType.PUBLIC_KEY.toString(),
                            EC2Algorithm.ES256.algoValue
                        )
                    )
                )

            if (authenticatorAttachment != "") {
                val builder = AuthenticatorSelectionCriteria.Builder()
                builder.setAttachment(Attachment.fromString("platform"))
                options.setAuthenticatorSelection(builder.build())
            }

            val fido2ApiClient = Fido.getFido2ApiClient(applicationContext)
            val fido2PendingIntentTask = fido2ApiClient.getRegisterIntent(options.build())
            fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                if (fido2PendingIntent.hasPendingIntent()) {
                    try {
                        Log.d(LOG_TAG, "launching Fido2 Pending Intent")
                        fido2PendingIntent.launchPendingIntent(
                            this@MainActivity,
                            REQUEST_CODE_REGISTER
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }


    //**********************************************************************************************************//
    //******************************* FIDO2 Registration Step 3 ************************************************//
    //***************************** Send Signed Challenge (Attestation) to the Server for validation ***********//
    //**********************************************************************************************************//
    private fun fido2RegisterComplete(fido2Response: ByteArray) {
        val attestationResponse =
            AuthenticatorAttestationResponse.deserializeFromBytes(fido2Response)
        val credId = Base64.encodeToString(attestationResponse.keyHandle, BASE64_FLAG)
        val clientDataJson = Base64.encodeToString(attestationResponse.clientDataJSON, BASE64_FLAG)
        val attestationObjectBase64 =
            Base64.encodeToString(attestationResponse.attestationObject, Base64.DEFAULT)

        val webAuthnResponse = JSONObject()
        val response = JSONObject()

        response.put("attestationObject", attestationObjectBase64)
        response.put("clientDataJSON", clientDataJson)


        webAuthnResponse.put("type", "public-key")
        webAuthnResponse.put("id", credId)
        webAuthnResponse.put("rawId", credId)
        webAuthnResponse.put("getClientExtensionResults", JSONObject())
        webAuthnResponse.put("response", response)

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, webAuthnResponse.toString())

        try {
            RPApiService.getApi()
                .registerComplete("username=${usernameButton.text.toString()}", requestBody)
                //.registerComplete( requestBody)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            resultText.text = "Registration Successful"
                            Log.d("response", response.message())
                        } else {
                            resultText.text = "Registration Failed" + "\n" + response.toString()
                            Log.d("response", response.errorBody().toString())
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("response", t.message)

                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //**********************************************************************************************************//
    //******************************* FIDO2 Authentication Step 1 **********************************************//
    //******************************* Get challenge from the Server ********************************************//
    //**********************************************************************************************************//
    private fun fido2AuthInitiate() {

        val result = JSONObject()
        val mediaType = "application/json".toMediaTypeOrNull()
        result.put("username", usernameButton.text.toString())
        val requestBody = RequestBody.create(mediaType, result.toString())
        try {
            RPApiService.getApi().authInitiate(requestBody)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            val obj = JSONObject(response.body()?.string())
                            val c = obj?.getString("challenge")
                            val challenge = Base64.decode(c!!, BASE64_FLAG)
                            val allowCredentials = obj?.getJSONArray("allowCredentials")

                            fido2AndroidAuth(allowCredentials, challenge)

                            Log.d("response", response.message())
                        } else {
                            Log.d("response", response.errorBody().toString())
                            resultText.text = "Authentication Failed" + "\n" + response.toString()
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("response", t.message)

                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //**********************************************************************************************************//
    //******************************* FIDO2 Authentication Step 2 **********************************************//
    //******************************* Invoke Android FIDO2 API  ************************************************//
    //**********************************************************************************************************//
    private fun fido2AndroidAuth(
        allowCredentials: JSONArray,
        challenge: ByteArray
    ) {
        try {
            val list = mutableListOf<PublicKeyCredentialDescriptor>()
            for (i in 0..(allowCredentials.length() - 1)) {
                val item = allowCredentials.getJSONObject(i)
                list.add(
                    PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PUBLIC_KEY.toString(),
                        Base64.decode(item.getString("id"), BASE64_FLAG),
                        /* transports */ null
                    )
                )
            }

            val options = PublicKeyCredentialRequestOptions.Builder()
                .setRpId(RPID)
                .setAllowList(list)
                .setChallenge(challenge)
                .build()

            val fido2ApiClient = Fido.getFido2ApiClient(applicationContext)
            val fido2PendingIntentTask = fido2ApiClient.getSignIntent(options)
            fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                if (fido2PendingIntent.hasPendingIntent()) {
                    try {
                        Log.d(LOG_TAG, "launching Fido2 Pending Intent")
                        fido2PendingIntent.launchPendingIntent(this@MainActivity, REQUEST_CODE_SIGN)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //**********************************************************************************************************//
    //******************************* FIDO2 Authentication Step 3 **********************************************//
    //**************** Send Signed Challenge (Assertion) to the Server for verification ************************//
    //**********************************************************************************************************//
    private fun fido2AuthComplete(fido2Response: ByteArray) {

        val assertionResponse = AuthenticatorAssertionResponse.deserializeFromBytes(fido2Response)
        val credId = Base64.encodeToString(assertionResponse.keyHandle, BASE64_FLAG)
        val signature = Base64.encodeToString(assertionResponse.signature, BASE64_FLAG)
        val authenticatorData =
            Base64.encodeToString(assertionResponse.authenticatorData, BASE64_FLAG)
        val clientDataJson = Base64.encodeToString(assertionResponse.clientDataJSON, BASE64_FLAG)


        val response = JSONObject()
        response.put("clientDataJSON", clientDataJson)
        response.put("signature", signature)
        response.put("userHandle", "")
        response.put("authenticatorData", authenticatorData)

        val jsonObject = JSONObject()
        jsonObject.put("type", "public-key")
        jsonObject.put("id", credId)
        jsonObject.put("rawId", credId)
        jsonObject.put("getClientExtensionResults", JSONObject())
        jsonObject.put("response", response)

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, jsonObject.toString())

        try {
            RPApiService.getApi()
                .authComplete("username=${usernameButton.text.toString()}", requestBody)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            resultText.text = "Authentication Successful"
                            Log.d("response", response.message())
                        } else {
                            Log.d("response", response.errorBody().toString())
                            resultText.text = "Authentication Failed" + "\n" + response.toString()
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("response", t.message)

                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
