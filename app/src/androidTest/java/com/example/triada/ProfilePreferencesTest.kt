package com.example.triada

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.repository.DataStoreProfilePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class ProfilePreferencesTest {
    @Test fun selectedProfileSurvivesStoreRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "profile-test-${UUID.randomUUID()}.preferences_pb")
        var job = SupervisorJob()
        try {
            var preferences = DataStoreProfilePreferences(PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file },
            ))
            assertNull(preferences.lastProfileId.first())
            preferences.select("profile-a")
            assertEquals("profile-a", preferences.lastProfileId.first())
            job.cancelAndJoin()
            job = SupervisorJob()
            preferences = DataStoreProfilePreferences(PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file },
            ))
            assertEquals("profile-a", preferences.lastProfileId.first())
            preferences.select("profile-b")
            assertEquals("profile-b", preferences.lastProfileId.first())
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }
}
