package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        LyricsEntity::class,
        PlaybackHistoryEntity::class,
        QueueItemEntity::class,
        SearchHistoryEntity::class,
        ExcludedFolderEntity::class,
        SafFolderEntity::class,
        BookmarkEntity::class,
        NamedQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PlayMixDatabase : RoomDatabase() {
    abstract fun playMixDao(): PlayMixDao

    companion object {
        @Volatile
        private var INSTANCE: PlayMixDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE songs ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE songs ADD COLUMN isCompilation INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE songs ADD COLUMN trackGainDb REAL NOT NULL DEFAULT -1.2")
                    db.execSQL("ALTER TABLE songs ADD COLUMN albumGainDb REAL NOT NULL DEFAULT -1.0")
                    db.execSQL("ALTER TABLE songs ADD COLUMN onlineProviderId TEXT")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS bookmarks (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `songId` TEXT NOT NULL, `positionMs` INTEGER NOT NULL, `label` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_songId ON bookmarks(songId)")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS named_queues (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `songIdsCsv` TEXT NOT NULL, `currentIndex` INTEGER NOT NULL, `currentPositionMs` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                    )
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE songs ADD COLUMN primaryCategory TEXT NOT NULL DEFAULT 'MUSIC'")
                    db.execSQL("ALTER TABLE songs ADD COLUMN originalCategory TEXT NOT NULL DEFAULT 'MUSIC'")
                    db.execSQL("ALTER TABLE songs ADD COLUMN isManualClassification INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE songs ADD COLUMN classificationReason TEXT NOT NULL DEFAULT 'MediaStore IS_MUSIC flag'")
                    db.execSQL("ALTER TABLE songs ADD COLUMN removedFromCategoriesCsv TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE songs ADD COLUMN exclusionReason TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE songs ADD COLUMN categoryUpdatedAt INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE songs ADD COLUMN mimeType TEXT NOT NULL DEFAULT 'audio/wav'")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_primaryCategory ON songs(primaryCategory)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isHidden ON songs(isHidden)")
                }
            }
        }

        fun getInstance(context: Context): PlayMixDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlayMixDatabase::class.java,
                    "play_mix_music.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
