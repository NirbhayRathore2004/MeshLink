package com.meshlink.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.meshlink.data.database.dao.ConversationDao
import com.meshlink.data.database.dao.MessageDao
import com.meshlink.data.database.dao.PeerDao
import com.meshlink.data.database.dao.ProcessedPacketDao
import com.meshlink.data.database.dao.QueuedPacketDao
import com.meshlink.data.database.dao.RouteDao
import com.meshlink.data.database.entity.ConversationEntity
import com.meshlink.data.database.entity.MessageEntity
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.data.database.entity.ProcessedPacketEntity
import com.meshlink.data.database.entity.QueuedPacketEntity
import com.meshlink.data.database.entity.RouteEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        PeerEntity::class,
        ProcessedPacketEntity::class,
        QueuedPacketEntity::class,
        RouteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeshDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun peerDao(): PeerDao
    abstract fun processedPacketDao(): ProcessedPacketDao
    abstract fun queuedPacketDao(): QueuedPacketDao
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: MeshDatabase? = null

        fun getDatabase(context: Context): MeshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeshDatabase::class.java,
                    "meshlink_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
