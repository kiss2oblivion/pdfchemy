package com.pdfchemy.app.jail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Isolated process service for processing PDF documents securely.
 */
class PdfJailService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val binder = object : IPdfJailService.Stub() {
        override fun compressPdf(
            sourceFd: ParcelFileDescriptor?,
            targetFd: ParcelFileDescriptor?,
            targetDpi: Float,
            quality: Float,
            rasterizePages: Boolean,
            callback: IPdfJailCallback?
        ) {
            if (sourceFd == null || targetFd == null || callback == null) {
                callback?.onFailure(-1, "Invalid null arguments")
                return
            }

            // Move to background thread
            serviceScope.launch {
                var success = false
                var outputSize = 0L
                try {
                    val inputStream = ParcelFileDescriptor.AutoCloseInputStream(sourceFd)
                    val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(targetFd)

                    inputStream.use { streamIn ->
                        outputStream.use { streamOut ->
                            // Load from stream
                            PDDocument.load(streamIn, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                                // Compress images
                                var compressedCount = 0
                                for (page in document.pages) {
                                    val resources = page.resources ?: continue
                                    for (name in resources.xObjectNames) {
                                        if (resources.isImageXObject(name)) {
                                            val xObject = resources.getXObject(name) as? PDImageXObject ?: continue
                                            try {
                                                val image = xObject.image ?: continue
                                                val newImage = JPEGFactory.createFromImage(document, image, quality, targetDpi.toInt())
                                                resources.put(name, newImage)
                                                compressedCount++
                                            } catch (e: Exception) {
                                                // Ignore individual image compression failure
                                            }
                                        }
                                    }
                                }

                                // Save to output stream
                                document.save(streamOut)
                            }
                        }
                    }
                    
                    // Determine size (targetFd statSize doesn't work well after write, but we can try)
                    outputSize = targetFd.statSize
                    success = true
                } catch (e: Exception) {
                    try {
                        callback.onFailure(1, e.message ?: "Unknown compression error")
                    } catch (re: RemoteException) {
                        // Dead object
                    }
                } finally {
                    if (success) {
                        try {
                            callback.onSuccess(outputSize)
                        } catch (re: RemoteException) {
                            // Dead object
                        }
                    }
                    // Stop service after processing 1 job (disposable worker requirement)
                    stopSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
