package com.dlx.sshterm.di

import android.content.Context
import androidx.room.Room
import com.dlx.sshterm.data.local.db.AppDatabase
import com.dlx.sshterm.data.local.db.dao.HostDao
import com.dlx.sshterm.data.local.db.dao.KnownHostDao
import com.dlx.sshterm.data.local.db.dao.SessionSnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .build()
    }

    @Provides
    fun provideHostDao(database: AppDatabase): HostDao =
        database.hostDao()

    @Provides
    fun provideKnownHostDao(database: AppDatabase): KnownHostDao =
        database.knownHostDao()

    @Provides
    fun provideSessionSnapshotDao(database: AppDatabase): SessionSnapshotDao =
        database.sessionSnapshotDao()
}
