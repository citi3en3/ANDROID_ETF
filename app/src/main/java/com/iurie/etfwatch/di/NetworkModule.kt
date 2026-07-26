package com.iurie.etfwatch.di

import android.content.Context
import com.iurie.etfwatch.BuildConfig
import com.iurie.etfwatch.data.remote.FmpService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE = "https://financialmodelingprep.com/api/v3/"

    @Provides @Singleton
    fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides @Singleton
    fun okHttp(@ApplicationContext ctx: Context): OkHttpClient {
        val cacheDir = ctx.cacheDir.resolve("http")
        val cache = Cache(cacheDir, 5L * 1024 * 1024)
        val apiKey = BuildConfig.FMP_API_KEY
        val keyInterceptor = Interceptor { chain ->
            val req = chain.request()
            val url = req.url.newBuilder().addQueryParameter("apikey", apiKey).build()
            chain.proceed(req.newBuilder().url(url).build())
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(keyInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE.toHttpUrl())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun fmp(retrofit: Retrofit): FmpService = retrofit.create(FmpService::class.java)
}
