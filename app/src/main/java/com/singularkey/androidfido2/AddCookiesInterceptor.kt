package com.singularkey.androidfido2

import android.content.Context
import android.preference.PreferenceManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response


class AddCookiesInterceptor(private var context: Context?) : Interceptor {
    val PREF_COOKIES = "PREF_COOKIES"

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet(PREF_COOKIES, HashSet()) as HashSet<String>?

        for (cookie in preferences!!) {
            builder.addHeader("Cookie", cookie)
        }
        return chain.proceed(builder.build())
    }
}