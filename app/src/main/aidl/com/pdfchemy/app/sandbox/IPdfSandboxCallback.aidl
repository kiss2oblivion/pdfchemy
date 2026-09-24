package com.pdfchemy.app.sandbox;

interface IPdfSandboxCallback {
    void onSuccess(String resultJson);
    void onError(String errorMessage);
    void onProgress(int progress, String message);
}
