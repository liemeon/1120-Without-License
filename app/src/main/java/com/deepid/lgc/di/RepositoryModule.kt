package com.deepid.lgc.di

import com.deepid.lgc.data.network.MainNetwork
import com.deepid.lgc.data.repository.MainRepository
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val repositoryModule = module {
    single(named("IODispatcher")) {
        Dispatchers.IO
    }
    single { MainRepository(get(named("IODispatcher")),get())}
}

val dataModule = module {
    single { provideMainNetwork(get()) }
}

fun provideMainNetwork(retrofit: Retrofit): MainNetwork {
    return retrofit.create(MainNetwork::class.java)
}
