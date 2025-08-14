package com.ml.lansonesscan.presentation.analysis

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ml.lansonesscan.presentation.analysis.components.GalleryPicker
import com.ml.lansonesscan.presentation.analysis.components.ImagePreviewDialog
import com.ml.lansonesscan.ui.theme.LansonesScanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraGalleryIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun galleryPicker_displaysCorrectButton() {
        composeTestRule.setContent {
            LansonesScanTheme {
                GalleryPicker(
                    onImageSelected = { },
                    onError = { }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Choose from Gallery")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Select an existing image")
            .assertIsDisplayed()
    }

    @Test
    fun galleryPicker_hasCorrectAccessibility() {
        composeTestRule.setContent {
            LansonesScanTheme {
                GalleryPicker(
                    onImageSelected = { },
                    onError = { }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Select image from gallery")
            .assertIsDisplayed()
    }

    @Test
    fun galleryPicker_enabledState_isClickable() {
        var clicked = false
        
        composeTestRule.setContent {
            LansonesScanTheme {
                GalleryPicker(
                    onImageSelected = { clicked = true },
                    onError = { },
                    enabled = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Choose from Gallery")
            .assertIsEnabled()
    }

    @Test
    fun galleryPicker_disabledState_isNotClickable() {
        composeTestRule.setContent {
            LansonesScanTheme {
                GalleryPicker(
                    onImageSelected = { },
                    onError = { },
                    enabled = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("Choose from Gallery")
            .assertIsNotEnabled()
    }

    @Test
    fun imagePreviewDialog_displaysWhenUriProvided() {
        val testUri = Uri.parse("content://test/image.jpg")
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Preview Image")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Is this image clear and suitable for analysis?")
            .assertIsDisplayed()
    }

    @Test
    fun imagePreviewDialog_doesNotDisplayWhenUriIsNull() {
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = null,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Preview Image")
            .assertDoesNotExist()
    }

    @Test
    fun imagePreviewDialog_showsCorrectButtonsForCamera() {
        val testUri = Uri.parse("content://test/image.jpg")
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { },
                    isFromCamera = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Cancel")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Retake")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Use Image")
            .assertIsDisplayed()
    }

    @Test
    fun imagePreviewDialog_showsCorrectButtonsForGallery() {
        val testUri = Uri.parse("content://test/image.jpg")
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { },
                    isFromCamera = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("Cancel")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Reselect")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Use Image")
            .assertIsDisplayed()
    }

    @Test
    fun imagePreviewDialog_confirmButton_callsCallback() {
        val testUri = Uri.parse("content://test/image.jpg")
        var confirmCalled = false
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { confirmCalled = true },
                    onRetake = { },
                    onCancel = { }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Use Image")
            .performClick()

        assert(confirmCalled)
    }

    @Test
    fun imagePreviewDialog_retakeButton_callsCallback() {
        val testUri = Uri.parse("content://test/image.jpg")
        var retakeCalled = false
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { retakeCalled = true },
                    onCancel = { }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Retake")
            .performClick()

        assert(retakeCalled)
    }

    @Test
    fun imagePreviewDialog_cancelButton_callsCallback() {
        val testUri = Uri.parse("content://test/image.jpg")
        var cancelCalled = false
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { cancelCalled = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assert(cancelCalled)
    }

    @Test
    fun imagePreviewDialog_hasCorrectAccessibility() {
        val testUri = Uri.parse("content://test/image.jpg")
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Image preview dialog")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Preview of selected image")
            .assertIsDisplayed()
    }

    @Test
    fun imagePreviewDialog_showsCorrectTextForGalleryImage() {
        val testUri = Uri.parse("content://test/image.jpg")
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { },
                    isFromCamera = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("Use this image for lansones disease analysis?")
            .assertIsDisplayed()
    }

    @Test
    fun imagePreviewDialog_showsCorrectTextForCameraImage() {
        val testUri = Uri.parse("content://test/image.jpg")
        
        composeTestRule.setContent {
            LansonesScanTheme {
                ImagePreviewDialog(
                    imageUri = testUri,
                    onConfirm = { },
                    onRetake = { },
                    onCancel = { },
                    isFromCamera = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Is this image clear and suitable for analysis?")
            .assertIsDisplayed()
    }
}