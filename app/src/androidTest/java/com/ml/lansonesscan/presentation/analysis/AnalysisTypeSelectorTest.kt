package com.ml.lansonesscan.presentation.analysis

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.presentation.analysis.components.AnalysisTypeSelector
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisTypeSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun analysisTypeSelector_displaysCorrectTitle() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Select Analysis Type")
            .assertIsDisplayed()
    }

    @Test
    fun analysisTypeSelector_displaysBothOptions() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { }
                )
            }
        }

        // Check that both analysis types are displayed
        composeTestRule
            .onNodeWithText("Fruit Analysis")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Leaf Analysis")
            .assertIsDisplayed()
    }

    @Test
    fun analysisTypeSelector_displaysDescriptions() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { }
                )
            }
        }

        // Check that descriptions are displayed
        composeTestRule
            .onNodeWithText("Analyzes fruit surface for diseases, ripeness, and quality issues")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Analyzes leaves for diseases, pest damage, and nutrient deficiencies")
            .assertIsDisplayed()
    }

    @Test
    fun analysisTypeSelector_fruitSelection_callsCallback() {
        var selectedType: AnalysisType? = null
        
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { selectedType = it }
                )
            }
        }

        // Click on fruit analysis option
        composeTestRule
            .onNodeWithText("Fruit Analysis")
            .performClick()

        // Verify callback was called with correct type
        assert(selectedType == AnalysisType.FRUIT)
    }

    @Test
    fun analysisTypeSelector_leavesSelection_callsCallback() {
        var selectedType: AnalysisType? = null
        
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { selectedType = it }
                )
            }
        }

        // Click on leaves analysis option
        composeTestRule
            .onNodeWithText("Leaf Analysis")
            .performClick()

        // Verify callback was called with correct type
        assert(selectedType == AnalysisType.LEAVES)
    }

    @Test
    fun analysisTypeSelector_showsSelectedState() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = AnalysisType.FRUIT,
                    onTypeSelected = { }
                )
            }
        }

        // Check that selected indicator is shown for fruit
        composeTestRule
            .onAllNodesWithText("Selected")
            .assertCountEquals(1)
        
        // Verify the fruit card is selected (has "Selected" text)
        composeTestRule
            .onNode(
                hasText("Fruit Analysis") and 
                hasAnyDescendant(hasText("Selected"))
            )
            .assertExists()
    }

    @Test
    fun analysisTypeSelector_switchesSelection() {
        var selectedType: AnalysisType? = AnalysisType.FRUIT
        
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )
            }
        }

        // Initially fruit should be selected
        composeTestRule
            .onNode(
                hasText("Fruit Analysis") and 
                hasAnyDescendant(hasText("Selected"))
            )
            .assertExists()

        // Click on leaves option
        composeTestRule
            .onNodeWithText("Leaf Analysis")
            .performClick()

        // Verify selection changed
        assert(selectedType == AnalysisType.LEAVES)
    }

    @Test
    fun analysisTypeSelector_disabledState_doesNotRespond() {
        var selectedType: AnalysisType? = null
        
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { selectedType = it },
                    enabled = false
                )
            }
        }

        // Try to click on fruit analysis option
        composeTestRule
            .onNodeWithText("Fruit Analysis")
            .performClick()

        // Verify callback was not called
        assert(selectedType == null)
    }

    @Test
    fun analysisTypeSelector_hasCorrectAccessibilityLabels() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { }
                )
            }
        }

        // Check main selector has content description
        composeTestRule
            .onNodeWithContentDescription("Analysis type selection")
            .assertIsDisplayed()

        // Check individual cards have proper content descriptions
        composeTestRule
            .onNodeWithContentDescription("Fruit Analysis: Analyzes fruit surface for diseases, ripeness, and quality issues")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Leaf Analysis: Analyzes leaves for diseases, pest damage, and nutrient deficiencies")
            .assertIsDisplayed()
    }

    @Test
    fun analysisTypeSelector_hasRadioButtonRole() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = null,
                    onTypeSelected = { }
                )
            }
        }

        // Check that cards have radio button role for accessibility
        composeTestRule
            .onAllNodesWithRole(Role.RadioButton)
            .assertCountEquals(2)
    }

    @Test
    fun analysisTypeSelector_visualIndicators_showCorrectly() {
        composeTestRule.setContent {
            LansonesScanTheme {
                AnalysisTypeSelector(
                    selectedType = AnalysisType.LEAVES,
                    onTypeSelected = { }
                )
            }
        }

        // Only the selected option should show "Selected" text
        composeTestRule
            .onAllNodesWithText("Selected")
            .assertCountEquals(1)
        
        // Verify it's associated with the leaves option
        composeTestRule
            .onNode(
                hasText("Leaf Analysis") and 
                hasAnyDescendant(hasText("Selected"))
            )
            .assertExists()
        
        // Verify fruit option doesn't have selected indicator
        composeTestRule
            .onNode(
                hasText("Fruit Analysis") and 
                hasAnyDescendant(hasText("Selected"))
            )
            .assertDoesNotExist()
    }
}