package com.dlx.sshterm.data.local.db.converter

import androidx.room.TypeConverter
import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.domain.model.NetworkTargetType
import com.dlx.sshterm.domain.model.SessionStatus

/**
 * Room type converters for enums and other types.
 */
class RoomConverters {

    @TypeConverter
    fun fromAuthType(value: AuthType): String = value.name

    @TypeConverter
    fun toAuthType(value: String): AuthType = AuthType.valueOf(value)

    @TypeConverter
    fun fromNetworkTargetType(value: NetworkTargetType): String = value.name

    @TypeConverter
    fun toNetworkTargetType(value: String): NetworkTargetType = NetworkTargetType.valueOf(value)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}
