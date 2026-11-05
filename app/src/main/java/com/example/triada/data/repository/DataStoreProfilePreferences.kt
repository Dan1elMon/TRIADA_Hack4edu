package com.example.triada.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.triada.domain.repository.ProfilePreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

class DataStoreProfilePreferences(private val store: DataStore<Preferences>) : ProfilePreferences {
    private val profileKey = stringPreferencesKey("last_profile_id")
    override val lastProfileId = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { it[profileKey] }
    override suspend fun select(profileId: String) {
        store.edit { it[profileKey] = profileId }
    }
}
