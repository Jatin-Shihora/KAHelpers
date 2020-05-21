package com.crazylegend.kotlinextensions.retrofit

import android.content.Context
import com.crazylegend.kotlinextensions.isNull
import com.crazylegend.kotlinextensions.retrofit.interceptors.ConnectivityInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Created by Hristijan on 1/23/19 to long live and prosper !
 */
object RetrofitClient {

    private var retrofit: Retrofit? = null

    var connectTimeout = 10L
    var readTimeout = 10L
    var writeTimeout = 10L
    var connectionTimeUnit = TimeUnit.SECONDS

    private val gsonConverter by lazy {
        GsonConverterFactory.create()
    }

    private val moshiConverter by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        MoshiConverterFactory.create(moshi).withNullSerialization()
    }

    private val rxJava2Adapter by lazy {
        RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())
    }

    /*private val rxJava3Adapter by lazy {
        RxJava3CallAdapterFactory.create()
    }*/


    fun removeRetrofitInstance() {
        retrofit = null
    }

    fun customInstanceFactory(context: Context, baseUrl: String, factory: Converter.Factory, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, factory, rxJava2Adapter)
        }
        return retrofit
    }


    fun customInstance(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {},
                       builderCallback: Retrofit.Builder.() -> Retrofit.Builder = { this }): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, builderCallback)
        }
        return retrofit
    }


    fun gsonInstanceRxJava2(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, gsonConverter, rxJava2Adapter)
        }
        return retrofit
    }

    /*fun gsonInstanceRxJava3(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, gsonConverter, rxJava3Adapter)
        }
        return retrofit
    }
*/

    fun gsonInstanceCouroutines(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, gsonConverter)
        }
        return retrofit
    }

    fun moshiInstanceRxJava2(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, moshiConverter, rxJava2Adapter)
        }
        return retrofit
    }

   /* fun moshiInstanceRxJava3(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, moshiConverter, rxJava3Adapter)
        }
        return retrofit
    }*/


    fun moshiInstanceCoroutines(context: Context, baseUrl: String, enableInterceptor: Boolean = false, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): Retrofit? {
        val clientBuilder = buildClient(context, enableInterceptor, okHttpClientConfig)
        doesRetrofitNeedsBuild(baseUrl) {
            retrofit = buildRetrofit(baseUrl, clientBuilder, moshiConverter)
        }
        return retrofit
    }

    private fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient.Builder, converterFactory: Converter.Factory, callAdapterFactory: CallAdapter.Factory): Retrofit? {
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient.build())
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
    }

    private fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient.Builder, converterFactory: Converter.Factory): Retrofit? {
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient.build())
                .addConverterFactory(converterFactory)
                .build()
    }

    private inline fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient.Builder,
                              builderCallback: Retrofit.Builder.() -> Retrofit.Builder = { this }): Retrofit? {
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient.build())
                .builderCallback()
                .build()
    }

    private fun OkHttpClient.Builder.addBasicInterceptors(loggingInterceptor: HttpLoggingInterceptor, connectivityInterceptor: ConnectivityInterceptor) {
        addInterceptor(loggingInterceptor)
        addInterceptor(connectivityInterceptor)
        connectTimeout(connectTimeout, connectionTimeUnit)
        readTimeout(readTimeout, connectionTimeUnit)
        writeTimeout(writeTimeout, connectionTimeUnit)
    }

    private inline fun doesRetrofitNeedsBuild(baseUrl: String, function: () -> Unit) {
        if (retrofit.isNull) {
            function()
        } else {
            if (retrofit?.baseUrl()?.toString() != baseUrl) {
                function()
            }
        }
    }

    private inline fun buildClient(context: Context, enableInterceptor: Boolean, okHttpClientConfig: OkHttpClient.Builder.() -> Unit = {}): OkHttpClient.Builder {
        val clientBuilder = OkHttpClient.Builder()
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level =
                if (enableInterceptor) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        clientBuilder.apply {
            addBasicInterceptors(loggingInterceptor, ConnectivityInterceptor(context))
            okHttpClientConfig.invoke(this)
        }
        return clientBuilder
    }


}

