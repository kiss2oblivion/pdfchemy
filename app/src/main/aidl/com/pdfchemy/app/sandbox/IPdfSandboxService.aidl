package com.pdfchemy.app.sandbox;

import com.pdfchemy.app.sandbox.IPdfSandboxCallback;
import android.os.ParcelFileDescriptor;

interface IPdfSandboxService {
    int getWorkerPid();
    
    void auditDocument(in ParcelFileDescriptor inputPfd, IPdfSandboxCallback callback);
    
    void sanitizeDocument(in ParcelFileDescriptor inputPfd, in ParcelFileDescriptor outputPfd, boolean purgeJs, boolean purgeActions, boolean purgeMetadata, IPdfSandboxCallback callback);
    
    void searchRedactionTargets(in ParcelFileDescriptor inputPfd, String query, boolean isRegex, IPdfSandboxCallback callback);
    
    void redactDocument(in ParcelFileDescriptor inputPfd, in ParcelFileDescriptor outputPfd, String configJson, IPdfSandboxCallback callback);
    
    void convertPdfToEpub(in ParcelFileDescriptor inputPfd, in ParcelFileDescriptor outputPfd, String title, String author, IPdfSandboxCallback callback);
    void convertEpubToPdf(in ParcelFileDescriptor inputPfd, in ParcelFileDescriptor outputPfd, IPdfSandboxCallback callback);
}
