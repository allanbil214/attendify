package com.allan.attendify.data.remote

import com.allan.attendify.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authEventBus: AuthEventBus
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = chain.request().newBuilder()
        
        if (token != null) {
            request.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(request.build())

        if (response.code == 401) {
            // Use runBlocking because interceptors are not suspend functions
            runBlocking {
                authEventBus.postEvent(AuthEvent.SessionExpired)
            }
        }
        
        return response
    }
}
