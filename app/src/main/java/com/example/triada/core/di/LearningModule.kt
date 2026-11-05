package com.example.triada.core.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.triada.data.local.LearningDatabase
import com.example.triada.data.repository.DataStoreProfilePreferences
import com.example.triada.data.repository.RoomLearningRepository
import com.example.triada.domain.repository.LearningRepository
import com.example.triada.domain.repository.ProfilePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore(name = "profile_preferences")

@Module
@InstallIn(SingletonComponent::class)
object LearningModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LearningDatabase =
        Room.databaseBuilder(context, LearningDatabase::class.java, "aulanexo.db").build()

    @Provides @Singleton
    fun repository(database: LearningDatabase): LearningRepository = RoomLearningRepository(database)

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): ProfilePreferences =
        DataStoreProfilePreferences(context.profileDataStore)
}
