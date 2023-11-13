package com.deepid.lgc.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.deepid.lgc.BuildConfig
import com.deepid.lgc.data.common.NetworkInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


val networkModule = module {
    single { provideInterceptor(androidContext()) }
    single { provideHttpClient(get(), androidContext()) }
    single { provideRetrofit(get()) }
}

fun provideRetrofit(client: OkHttpClient): Retrofit {
    return Retrofit.Builder().apply {
        baseUrl(BuildConfig.BASE_URL)
        addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
        client(client)
    }.build()
}

fun provideHttpClient(networkInterceptor: NetworkInterceptor, applicationContext: Context): OkHttpClient {
    return OkHttpClient.Builder().apply {
        addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        addInterceptor(ChuckerInterceptor(applicationContext))
        readTimeout(60, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
        connectTimeout(60, TimeUnit.SECONDS)
        addInterceptor(networkInterceptor)
    }.build()
}


fun provideInterceptor(applicationContext: Context): NetworkInterceptor {
    return NetworkInterceptor(applicationContext)
}
