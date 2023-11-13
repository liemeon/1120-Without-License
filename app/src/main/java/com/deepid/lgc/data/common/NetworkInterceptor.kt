package com.deepid.lgc.data.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import okhttp3.Interceptor
import okhttp3.Response


class NetworkInterceptor constructor(private val applicationContext: Context) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isConnected()) {
            throw Exception("No internet connection")
        }
        val url = chain.request().url.toString()
        val newRequest = if (!url.contains("s3.ap-northeast-2.amazonaws.com")){
            chain.request().newBuilder().header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjcwLCJkZXZpY2VJZCI6ImNhNmZjZjAzLWEzOGItNDY2Ni1iYzJmLTMxMjA3NWE2NWNkMSIsImlhdCI6MTY5ODkwNDI2Mn0.qp_JgY_ddU3EyYyHl5QXqwkim2gC2vq1wk6eV1LX3XSO-9qr3aGQLrLUR2JXov73fn4mVNsOHjhifpU7mUxH00B_XY8R0PQa7VcdhoumPVmDxZEfo3K0w5pKY5qSDzxtCV95gv0s4MpLcilGKIc2knGvt3mut6FoHvkrKJRxCTwFSphjeuH4wWSXYAVGLOV4V8OWdM7KL_XQ4MYrVHqVu-4GgSSVuVkPUpXGn35gp79I0OcVG--NiwoUzaJZE8XJR8_ZPH8eebZPZP-A4aDxRsx0Zf0w1PLYGF9zphw79DRuNpI707sf31kTZNacNsp4N4gN6ayB5OuNv_RZurErhA").build()
        }else{
            chain.request().newBuilder().header("Content-Type", "image/jpeg") .build()
        }
        Log.d("DEBUGX", "headers type: ${newRequest.headers["Content-Type"]}")
        Log.d("DEBUGX", "headers length: ${newRequest.headers["Content-Length"]}")
        return chain.proceed(newRequest)
    }

    @Suppress("DEPRECATION")
    private fun isConnected(): Boolean {
        val connectivityManager = applicationContext.getSystemService<ConnectivityManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager?.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager?.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }
}
