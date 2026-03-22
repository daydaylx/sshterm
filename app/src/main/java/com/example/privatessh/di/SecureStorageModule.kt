package com.example.privatessh.di

import android.content.Context
import com.example.privatessh.data.local.secure.KeystoreManager
import com.example.privatessh.data.local.secure.SecureKeyStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for secure storage dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecureStorageModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager {
        return KeystoreManager()
    }

    @Provides
    @Singleton
    fun provideSecureKeyStorage(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): SecureKeyStorage {
        return SecureKeyStorage(context, keystoreManager)
    }
}
