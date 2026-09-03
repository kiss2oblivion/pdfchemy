package com.pdfchemy.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.pdfchemy.desktop.ui.DesktopApp
import com.pdfchemy.desktop.ui.DesktopFileDialog
import com.pdfchemy.desktop.ui.theme.DesktopTheme
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

fun main(args: Array<String>) = application {
    var droppedFile by remember {
        mutableStateOf(args.firstOrNull { it.lowercase().endsWith(".pdf") }?.let { File(it) })
    }
    var isDarkTheme by remember { mutableStateOf(true) }

    val windowState = rememberWindowState(
        width = 1240.dp,
        height = 840.dp,
        position = WindowPosition.PlatformDefault
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "PDFchemy Tools — Local-First Offline PDF Utility",
        icon = painterResource("icons/linux/icon.png"),
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                // Ctrl+O (or Cmd+O) to Open File
                if (keyEvent.isCtrlPressed && keyEvent.key == Key.O) {
                    val file = DesktopFileDialog.openPdf()
                    if (file != null) {
                        droppedFile = file
                    }
                    true
                } else if (keyEvent.isCtrlPressed && keyEvent.key == Key.Q) {
                    exitApplication()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    ) {
        // Attach native OS Drag & Drop listener to the AWT Window Frame
        DisposableEffect(window) {
            val dropTarget = object : DropTarget() {
                @Suppress("UNCHECKED_CAST")
                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY)
                        val droppedData = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                        val pdfFile = droppedData?.firstOrNull { it.name.lowercase().endsWith(".pdf") }
                        if (pdfFile != null && pdfFile.exists()) {
                            droppedFile = pdfFile
                        }
                        evt.dropComplete(true)
                    } catch (e: Exception) {
                        evt.dropComplete(false)
                    }
                }
            }
            window.dropTarget = dropTarget
            onDispose {
                window.dropTarget = null
            }
        }

        DesktopTheme(darkTheme = isDarkTheme) {
            DesktopApp(
                initialFile = droppedFile,
                isDarkTheme = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme }
            )
        }
    }
}
