package com.duchamp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        SessionEntity::class,
        MetricSampleEntity::class,
        JournalEntryEntity::class,
        EventMarkerEntity::class,
        StateEntryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class DuchampDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun metricSampleDao(): MetricSampleDao
    abstract fun journalDao(): JournalDao
    abstract fun eventDao(): EventDao
    abstract fun stateDao(): StateDao

    companion object {
        @Volatile
        private var instance: DuchampDatabase? = null

        fun get(context: Context): DuchampDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DuchampDatabase::class.java,
                    "duchamp.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DuchampDatabase =
        DuchampDatabase.get(context)

    @Provides
    fun provideSessionDao(db: DuchampDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMetricSampleDao(db: DuchampDatabase): MetricSampleDao = db.metricSampleDao()

    @Provides
    fun provideJournalDao(db: DuchampDatabase): JournalDao = db.journalDao()

    @Provides
    fun provideEventDao(db: DuchampDatabase): EventDao = db.eventDao()

    @Provides
    fun provideStateDao(db: DuchampDatabase): StateDao = db.stateDao()
}
