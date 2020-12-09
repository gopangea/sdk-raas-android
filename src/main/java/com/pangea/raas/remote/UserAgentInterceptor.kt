package com.pangea.raas.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException


class UserAgentInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originRequest = chain.request()
        val requestWithUserAgent = originRequest.newBuilder()
            .header("x-pangea-user-agent", ApiClientHelper.getUserAgent())
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}