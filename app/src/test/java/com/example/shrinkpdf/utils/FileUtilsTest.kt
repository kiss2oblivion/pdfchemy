package com.example.shrinkpdf.utils

import android.content.Context
import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileUtilsTest {

    @Test
    fun getFileName_fromFileUri_returnsCorrectName() {
        // Arrange
        val context = mockk<Context>(relaxed = true)
        val uri = Uri.parse("file:///storage/emulated/0/Download/test_document.pdf")

        // Act
        val fileName = FileUtils.getFileName(context, uri)

        // Assert
        assertEquals("test_document.pdf", fileName)
    }

    @Test
    fun getFileName_fromSimplePath_returnsCorrectName() {
        val context = mockk<Context>(relaxed = true)
        val uri = Uri.parse("some_folder/another_folder/image.png")

        val fileName = FileUtils.getFileName(context, uri)

        assertEquals("image.png", fileName)
    }
}
