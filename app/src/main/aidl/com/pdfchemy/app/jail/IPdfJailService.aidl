package com.pdfchemy.app.jail;

import android.os.ParcelFileDescriptor;
import com.pdfchemy.app.jail.IPdfJailCallback;

/**
 * IPC Interface for the isolated PDF Jail Worker.
 */
oneway interface IPdfJailService {
    /**
     * Executes the COMPRESS operation in the isolated service.
     * 
     * @param sourceFd The ParcelFileDescriptor containing the untrusted source PDF.
     * @param targetFd The ParcelFileDescriptor where the processed output should be written.
     * @param targetDpi Target DPI for image downsampling.
     * @param quality JPEG quality factor (0.0 to 1.0).
     * @param rasterizePages True to flatten pages entirely.
     * @param callback Callback to receive the result asynchronously.
     */
    void compressPdf(
        in ParcelFileDescriptor sourceFd, 
        in ParcelFileDescriptor targetFd, 
        float targetDpi, 
        float quality, 
        boolean rasterizePages,
        IPdfJailCallback callback
    );
}
