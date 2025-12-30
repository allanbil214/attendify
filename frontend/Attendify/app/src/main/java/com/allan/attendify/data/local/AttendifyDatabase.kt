package com.allan.attendify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allan.attendify.data.local.entity.AttendanceEntity
import com.allan.attendify.data.local.entity.UserEntity
import com.allan.attendify.data.local.dao.UserDao
import com.allan.attendify.data.local.dao.AttendanceDao

@Database(
    entities = [UserEntity::class, AttendanceEntity::class],
    version = 1
)
abstract class AttendifyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attendanceDao(): AttendanceDao
}
