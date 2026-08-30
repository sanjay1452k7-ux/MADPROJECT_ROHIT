package com.cit.attendance.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cit.attendance.core.designsystem.theme.CitTheme
import com.cit.attendance.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Role selection gates entry to the whole product, so the rule that matters is
 * that Continue does nothing until a role has actually been chosen.
 */
class RoleSelectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var continuedWith: UserRole? = null

    private fun launchScreen() {
        composeRule.setContent {
            CitTheme {
                RoleSelectionScreen(
                    institutionName = "Chennai Institute of Technology",
                    onContinue = { continuedWith = it },
                )
            }
        }
    }

    @Test
    fun allThreeRolesAreOffered() {
        launchScreen()

        composeRule.onNodeWithText("Student").assertIsDisplayed()
        composeRule.onNodeWithText("Teacher").assertIsDisplayed()
        composeRule.onNodeWithText("Admin").assertIsDisplayed()
    }

    @Test
    fun continueDoesNothingUntilARoleIsChosen() {
        launchScreen()

        composeRule.onNodeWithText("Continue").performClick()

        assertNull("Continue must be inert before a role is selected", continuedWith)
    }

    @Test
    fun choosingStudentThenContinuingReportsTheStudentRole() {
        launchScreen()

        composeRule.onNodeWithText("Student").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        assertEquals(UserRole.STUDENT, continuedWith)
    }

    @Test
    fun theMostRecentlyChosenRoleWins() {
        launchScreen()

        composeRule.onNodeWithText("Student").performClick()
        composeRule.onNodeWithText("Admin").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        assertEquals(UserRole.ADMIN, continuedWith)
    }
}
