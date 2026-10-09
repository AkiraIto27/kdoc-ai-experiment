package com.example.kdoctest

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun normalCatalogLoadsAnotherPageAndRefreshes() {
        launch().use {
            awaitCount(50)
            compose.onNodeWithTag("catalog-count").assertIsDisplayed()
            compose.onNodeWithTag("catalog-list")
                .performScrollToNode(hasTestTag("catalog-load-more"))
            compose.onNodeWithTag("catalog-load-more").performClick()
            awaitCount(100)
            compose.onNodeWithTag("catalog-refresh").performClick()
            awaitCount(50)
        }
    }

    @Test
    fun emptyResponseHasAnExplicitEmptyState() {
        launch("empty").use {
            awaitTag("catalog-empty")
            compose.onNodeWithTag("catalog-empty").assertIsDisplayed()
            compose.onNodeWithTag("catalog-count").assertTextEquals("0 / 0 件")
        }
    }

    @Test
    fun initialFailureCanBeRetriedWithoutCrashing() {
        launch("error").use {
            awaitTag("catalog-initial-error")
            compose.onNodeWithTag("catalog-retry").performClick()
            awaitTag("catalog-initial-error")
            compose.onNodeWithTag("catalog-initial-error").assertIsDisplayed()
            compose.onNodeWithTag("catalog-count").assertTextEquals("0 / 0 件")
        }
    }

    @Test
    fun nextPageFailureKeepsTheFirstPage() {
        launch("next-page-error").use {
            awaitCount(50)
            compose.onNodeWithTag("catalog-list")
                .performScrollToNode(hasTestTag("catalog-load-more"))
            compose.onNodeWithTag("catalog-load-more").performClick()
            awaitTag("catalog-more-error")
            compose.onNodeWithTag("catalog-more-error").assertIsDisplayed()
            compose.onNodeWithTag("catalog-count").assertTextEquals("50 / 500 件")
        }
    }

    private fun launch(scenario: String = "normal"): ActivityScenario<MainActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ActivityScenario.launch(
            Intent(context, MainActivity::class.java).putExtra("catalog_scenario", scenario),
        )
    }

    private fun awaitCount(count: Int) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("$count / 500 件").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
