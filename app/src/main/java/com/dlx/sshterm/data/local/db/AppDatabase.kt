package com.dlx.sshterm.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dlx.sshterm.data.local.db.converter.RoomConverters
import com.dlx.sshterm.data.local.db.dao.HostDao
import com.dlx.sshterm.data.local.db.dao.KnownHostDao
import com.dlx.sshterm.data.local.db.dao.SessionSnapshotDao
import com.dlx.sshterm.data.local.db.entity.HostEntity
import com.dlx.sshterm.data.local.db.entity.KnownHostEntity
import com.dlx.sshterm.data.local.db.entity.SessionSnapshotEntity

/**
 * Main Room database for the app.
 */
@Database(
    entities = [
        HostEntity::class,
        KnownHostEntity::class,
        SessionSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO for SSH host profiles.
     */
    abstract fun hostDao(): HostDao

    /**
     * DAO for known SSH host keys.
     */
    abstract fun knownHostDao(): KnownHostDao

    /**
     * DAO for session snapshots.
     */
    abstract fun sessionSnapshotDao(): SessionSnapshotDao

    companion object {
        const val DATABASE_NAME = "private_ssh.db"
    }
}
