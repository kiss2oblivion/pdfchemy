package com.pdfchemy.desktop.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

object DesktopFileDialog {

    /**
     * Opens native file dialog to select a single PDF.
     */
    fun openPdf(parent: Frame? = null): File? {
        val dialog = FileDialog(parent, "Open PDF Document", FileDialog.LOAD).apply {
            filenameFilter = FilenameFilter { _, name ->
                name.lowercase().endsWith(".pdf")
            }
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        return File(dir, file)
    }

    /**
     * Opens native file dialog to select multiple PDFs (e.g. for Merge).
     */
    fun openMultiplePdfs(parent: Frame? = null): List<File> {
        val dialog = FileDialog(parent, "Select PDF Documents", FileDialog.LOAD).apply {
            isMultipleMode = true
            filenameFilter = FilenameFilter { _, name ->
                name.lowercase().endsWith(".pdf")
            }
            isVisible = true
        }
        val files = dialog.files ?: return emptyList()
        return files.toList()
    }

    /**
     * Opens native file dialog to save a PDF file.
     */
    fun savePdf(parent: Frame? = null, suggestedName: String = "document_edited.pdf"): File? {
        val dialog = FileDialog(parent, "Save PDF Document", FileDialog.SAVE).apply {
            file = if (suggestedName.lowercase().endsWith(".pdf")) suggestedName else "$suggestedName.pdf"
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val target = File(dir, file)
        return if (target.name.lowercase().endsWith(".pdf")) target else File(dir, "${target.name}.pdf")
    }

    /**
     * Opens native directory chooser.
     */
    fun chooseDirectory(parent: Frame? = null): File? {
        val dialog = FileDialog(parent, "Select Output Folder", FileDialog.LOAD).apply {
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            isVisible = true
        }
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val selected = File(dir, file)
        return if (selected.isDirectory) selected else File(dir)
    }
}
