package com.androidengineers.agent_quickstart_android.fillerfree.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionHistoryEntity::class], version = 1, exportSchema = false)
abstract class FillerFreeDatabase : RoomDatabase() {

    abstract fun sessionHistoryDao(): SessionHistoryDao


    companion object {
        @Volatile
        private var instance: FillerFreeDatabase? = null

        fun getInstance(context: Context): FillerFreeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FillerFreeDatabase::class.java,
                    "filler_free.db",
                ).build().also { instance = it }
            }
        }
    }
}