package com.example.fantahelpfe.di

import android.content.Context
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.DataRepositoryImpl
import com.example.fantahelpfe.data.UserPreferencesRepository
import com.example.fantahelpfe.data.UserPreferencesRepositoryImpl
import com.example.fantahelpfe.data.remote.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This module will live as long as the app does
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        // Optional: Setup logging interceptor for debugging network calls
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Or Level.BASIC, or Level.NONE for release
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS) // Example: 20 seconds
            .readTimeout(60, TimeUnit.SECONDS)    // Example: 60 seconds (for long API calls)
            .writeTimeout(20, TimeUnit.SECONDS)   // Example: 20 seconds
            .addInterceptor(loggingInterceptor)     // Add logging (optional)
            // Add any other interceptors you might already have or need
            .build()

        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:60001/")
            .client(okHttpClient) // <-- Use the configured OkHttpClient
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDataRepository(apiService: ApiService): DataRepository {
        return DataRepositoryImpl(apiService)
    }

    @Provides
    @Singleton // Ensures only one instance is created
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        // Assuming UserPreferencesRepository takes a DataStore<Preferences>
        // Adjust the constructor call based on your UserPreferencesRepository implementation
        return UserPreferencesRepositoryImpl()
    }
}