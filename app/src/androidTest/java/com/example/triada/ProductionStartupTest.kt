package com.example.triada

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

/** Exercises the real Application/Hilt/Room graph without creating or changing profiles. */
class ProductionStartupTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realApplicationStartsAndActivityCanBeRecreated() {
        compose.onNodeWithText("Inicia sesión para comenzar").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("login-user").performScrollTo().assertIsDisplayed()
    }
}
