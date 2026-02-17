package com.hunter.system.app

import android.content.Context
import androidx.room.Room
import com.hunter.system.core.database.QuestDao
import com.hunter.system.core.database.SystemDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module providing singleton dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSystemDatabase(
        @ApplicationContext context: Context
    ): SystemDatabase {
        return Room.databaseBuilder(
            context,
            SystemDatabase::class.java,
            SystemDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideQuestDao(database: SystemDatabase): QuestDao {
        return database.questDao()
    }
}
