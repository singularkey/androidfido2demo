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

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

interface RPApi {
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("register/initiate")
    fun registerInitiate(@Body postBody: RequestBody): Call<ResponseBody>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("register/complete")
    fun registerComplete(
        @Header("Cookie") cookie: String,
        @Body body: RequestBody
    ): Call<ResponseBody>
    //fun registerComplete(@Body body: RequestBody): Call<ResponseBody>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/initiate")
    fun authInitiate(@Body postBody: RequestBody): Call<ResponseBody>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/complete")
    fun authComplete(
        @Header("Cookie") cookie: String,
        @Body postBody: RequestBody
    ): Call<ResponseBody>

}

class RPApiService {

    companion object {
        var cookieManager: CookieManager? = null

        fun getApi(): RPApi {

            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            if (cookieManager == null) {
                cookieManager = CookieManager()
                cookieManager!!.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            }

            val okHttpClientBuilder = OkHttpClient().newBuilder() //create OKHTTPClient
            okHttpClientBuilder.cookieJar(JavaNetCookieJar(cookieManager!!))

            val okHttpClient = okHttpClientBuilder
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build()

            val retrofit =
                Retrofit.Builder().client(okHttpClient)
                    .baseUrl(RP_SERVER_URL)
                    .build();

            return retrofit.create(RPApi::class.java)
        }
    }

}