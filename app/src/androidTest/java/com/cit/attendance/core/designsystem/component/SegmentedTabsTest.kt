package com.cit.attendance.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cit.attendance.core.designsystem.theme.CitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The segmented control is how analytics, history and reports are reached, so
 * its selection behaviour is worth pinning down.
 */
class SegmentedTabsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var selected = 0

    private fun launch(options: List<String> = listOf("Analytics", "History", "Reports")) {
        composeRule.setContent {
            CitTheme {
                var index by remember { mutableIntStateOf(0) }
                SegmentedTabs(
                    options = options,
                    selectedIndex = index,
                    onSelect = {
                        index = it
                        selected = it
                    },
                )
            }
        }
    }

    @Test
    fun everyOptionIsShown() {
        launch()

        composeRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText("Reports").assertIsDisplayed()
    }

    @Test
    fun choosingAnOptionReportsItsIndex() {
        launch()

        composeRule.onNodeWithText("Reports").performClick()

        assertEquals(2, selected)
    }

    @Test
    fun theMostRecentChoiceWins() {
        launch()

        composeRule.onNodeWithText("Reports").performClick()
        composeRule.onNodeWithText("History").performClick()

        assertEquals(1, selected)
    }

    @Test
    fun aTwoOptionControlWorksTheSameWay() {
        launch(options = listOf("Analytics", "Reports"))

        composeRule.onNodeWithText("Reports").performClick()

        assertEquals(1, selected)
    }
}
