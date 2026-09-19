package com.nyxiaglow.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetouchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun looksAreVisible() {
        composeRule.setContent { TestRetouchScreen() }

        composeRule.onNodeWithText("Natural").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Nude").assertIsDisplayed()
        composeRule.onNodeWithText("Glam").assertIsDisplayed()
        composeRule.onNodeWithText("Matte").assertIsDisplayed()
        composeRule.onNodeWithText("Dewy").assertIsDisplayed()
    }

    @Test
    fun everyLookCanBeSelected() {
        composeRule.setContent { TestRetouchScreen() }

        GlowLooks.all.forEach { look ->
            composeRule.onNodeWithText(look.title).performClick().assertIsSelected()
        }
    }

    @Test
    fun smoothingSliderReportsBoundedValue() {
        val values = mutableListOf<Float>()
        composeRule.setContent {
            RetouchScreen(
                preserveTexture = true,
                smoothingIntensity = 0.45f,
                selectedTool = "Skin",
                selectedPreset = "natural",
                onTextureToggle = {},
                onSmoothingChange = { values += it },
                onToolSelected = {},
                onPresetSelected = {},
                onReset = {},
                onApply = {}
            )
        }

        composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        ).performTouchInput { swipeRight() }

        assert(values.isNotEmpty())
        assert(values.all { it in 0f..1f })
    }

    @Test
    fun textureToggleInvokesCallback() {
        var toggleCount = 0
        composeRule.setContent {
            RetouchScreen(
                preserveTexture = true,
                smoothingIntensity = 0.45f,
                selectedTool = "Skin",
                selectedPreset = "natural",
                onTextureToggle = { toggleCount++ },
                onSmoothingChange = {},
                onToolSelected = {},
                onPresetSelected = {},
                onReset = {},
                onApply = {}
            )
        }

        composeRule.onNodeWithText("Keep natural texture").performClick()

        assert(toggleCount == 1)
    }

    @Test
    fun selectingLookUpdatesSemantics() {
        composeRule.setContent { TestRetouchScreen() }

        composeRule.onNodeWithText("Dewy").performClick()
        composeRule.onNodeWithText("Dewy").assertIsSelected()
    }

    @Test
    fun resetAndApplyInvokeCallbacks() {
        val resetCalled = mutableStateOf(false)
        val applyCalled = mutableStateOf(false)
        composeRule.setContent {
            RetouchScreen(
                preserveTexture = true,
                smoothingIntensity = 0.45f,
                selectedTool = "Skin",
                selectedPreset = "natural",
                onTextureToggle = {},
                onSmoothingChange = {},
                onToolSelected = {},
                onPresetSelected = {},
                onReset = { resetCalled.value = true },
                onApply = { applyCalled.value = true }
            )
        }

        composeRule.onNodeWithText("RESET").performClick()
        composeRule.onNodeWithText("APPLY TO PREVIEW").performClick()

        assert(resetCalled.value)
        assert(applyCalled.value)
    }

    @Composable
    private fun TestRetouchScreen() {
        val selectedPreset = remember { mutableStateOf("natural") }

        RetouchScreen(
            preserveTexture = true,
            smoothingIntensity = 0.45f,
            selectedTool = "Skin",
            selectedPreset = selectedPreset.value,
            onTextureToggle = {},
            onSmoothingChange = {},
            onToolSelected = {},
            onPresetSelected = { selectedPreset.value = it },
            onReset = {},
            onApply = {}
        )
    }
}
