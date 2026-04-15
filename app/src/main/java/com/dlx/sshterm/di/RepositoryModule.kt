package com.dlx.sshterm.di

import com.dlx.sshterm.data.local.datastore.SettingsDataStore
import com.dlx.sshterm.data.local.secure.SecureKeyStorage
import com.dlx.sshterm.data.repository.*
import com.dlx.sshterm.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHostRepository(impl: HostRepositoryImpl): HostRepository

    @Binds
    @Singleton
    abstract fun bindKnownHostRepository(impl: KnownHostRepositoryImpl): KnownHostRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSecureKeyRepository(impl: SecureKeyRepositoryImpl): SecureKeyRepository
}
