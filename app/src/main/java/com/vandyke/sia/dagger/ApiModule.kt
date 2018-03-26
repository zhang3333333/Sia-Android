/*
 * Copyright (c) 2017 Nicholas van Dyke. All rights reserved.
 */

package com.vandyke.sia.dagger

import android.util.Base64
import com.squareup.moshi.Moshi
import com.vandyke.sia.data.local.Prefs
import com.vandyke.sia.data.remote.*
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class ApiModule {
    @Provides
    @Singleton
    fun provideSiaApi(): SiaApi {
//        return MockSiaApi()
        val clientBuilder = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // no timeout because some Sia API calls can take a long time to return
//                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .addInterceptor {
                    val original: Request = it.request()
                    val request: Request = original.newBuilder()
                            .header("User-agent", "Sia-Agent")
                            .header("Authorization", "Basic " + Base64.encodeToString(":${Prefs.apiPassword}".toByteArray(), Base64.NO_WRAP))
                            .method(original.method(), original.body())
                            .build()
                    try {
                        val response = it.proceed(request)
                        if (!response.isSuccessful) {
                            val errorMsg = response.peekBody(256).string()
                            val siaException = SiaException.fromError(errorMsg)
                            if (siaException != null)
                                throw siaException
                        }
                        return@addInterceptor response
                    } catch (e: ConnectException) {
                        if (e.message == "Failed to connect to localhost/127.0.0.1:9980")
                            throw SiadNotRunning()
                        else
                            throw e
                    }
                }

        return Retrofit.Builder()
                .addConverterFactory(
                        MoshiConverterFactory.create(
                                Moshi.Builder()
                                        .add(BigDecimalAdapter())
                                        .add(DataAdapters())
                                        .build()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(clientBuilder.build())
//                .baseUrl("http://10.0.22.248:9980/")
                .baseUrl("http://localhost:9980/")
                .build()
                .create(SiaApi::class.java)
    }
}