package com.deepid.lgc

import android.app.Application
import com.deepid.lgc.di.dataModule
import com.deepid.lgc.di.networkModule
import com.deepid.lgc.di.repositoryModule
import com.deepid.lgc.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level


class LGCApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger(Level.DEBUG)
            androidContext(this@LGCApplication)
            modules(listOf(
                networkModule,
                repositoryModule,
                dataModule,
                viewModelModule
            ))
        }
    }
}
