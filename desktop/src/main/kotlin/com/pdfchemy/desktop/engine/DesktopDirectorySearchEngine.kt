package com.pdfchemy.desktop.engine

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class SearchMatchSnippet(
    val pageNumber: Int, // 1-indexed
    val lineSnippet: String,
    val matchStart: Int = 0,
    val matchEnd: Int = 0
)

data class FileSearchResult(
    val file: File,
    val totalMatches: Int,
    val snippets: List<SearchMatchSnippet>
)

data class DirectorySearchProgress(
    val filesScanned: Int,
    val totalFiles: Int,
    val results: List<FileSearchResult>,
    val isComplete: Boolean,
    val isCancelled: Boolean = false
)

object DesktopDirectorySearchEngine {

    fun searchDirectory(
        directory: File,
        query: String,
        matchCase: Boolean = false,
        recursive: Boolean = true,
        cancelFlag: AtomicBoolean = AtomicBoolean(false),
        onProgress: (DirectorySearchProgress) -> Unit
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty() || !directory.exists() || !directory.isDirectory) {
            onProgress(DirectorySearchProgress(0, 0, emptyList(), isComplete = true))
            return
        }

        // Collect all PDF files in folder
        val pdfFiles = mutableListOf<File>()
        if (recursive) {
            directory.walkTopDown().filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }.forEach {
                pdfFiles.add(it)
            }
        } else {
            directory.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }?.let {
                pdfFiles.addAll(it)
            }
        }

        val totalFiles = pdfFiles.size
        if (totalFiles == 0) {
            onProgress(DirectorySearchProgress(0, 0, emptyList(), isComplete = true))
            return
        }

        val results = mutableListOf<FileSearchResult>()
        var scanned = 0
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
        val executor = Executors.newFixedThreadPool(cpuCores)

        try {
            val futures = pdfFiles.map { file ->
                executor.submit<FileSearchResult?> {
                    if (cancelFlag.get()) return@submit null
                    searchSingleFile(file, trimmedQuery, matchCase, cancelFlag)
                }
            }

            for (future in futures) {
                if (cancelFlag.get()) break
                val res = try { future.get() } catch (_: Exception) { null }
                scanned++
                if (res != null && res.totalMatches > 0) {
                    synchronized(results) {
                        results.add(res)
                    }
                }
                if (scanned % 3 == 0 || scanned == totalFiles) {
                    onProgress(
                        DirectorySearchProgress(
                            filesScanned = scanned,
                            totalFiles = totalFiles,
                            results = synchronized(results) { results.sortedByDescending { it.totalMatches } },
                            isComplete = (scanned == totalFiles)
                        )
                    )
                }
            }
        } finally {
            executor.shutdownNow()
        }

        onProgress(
            DirectorySearchProgress(
                filesScanned = scanned,
                totalFiles = totalFiles,
                results = synchronized(results) { results.sortedByDescending { it.totalMatches } },
                isComplete = true,
                isCancelled = cancelFlag.get()
            )
        )
    }

    private fun searchSingleFile(
        file: File,
        query: String,
        matchCase: Boolean,
        cancelFlag: AtomicBoolean
    ): FileSearchResult? {
        try {
            PDDocument.load(file).use { doc ->
                val totalPages = doc.numberOfPages
                if (totalPages == 0) return null

                val snippets = mutableListOf<SearchMatchSnippet>()
                var matchCount = 0
                var currentWriter = java.io.StringWriter()

                val stripper = object : PDFTextStripper() {
                    override fun startPage(page: org.apache.pdfbox.pdmodel.PDPage) {
                        if (cancelFlag.get()) {
                            throw InterruptedException("Search cancelled")
                        }
                        currentWriter = java.io.StringWriter()
                        output = currentWriter
                    }

                    override fun endPage(page: org.apache.pdfbox.pdmodel.PDPage) {
                        output.flush()
                        val pageText = currentWriter.toString()
                        if (pageText.isBlank()) return

                        val p = currentPageNo
                        val lines = pageText.lines()
                        for (line in lines) {
                            val trimmedLine = line.trim()
                            if (trimmedLine.isEmpty()) continue

                            val idx = if (matchCase) {
                                trimmedLine.indexOf(query)
                            } else {
                                trimmedLine.indexOf(query, ignoreCase = true)
                            }

                            if (idx >= 0) {
                                matchCount++
                                if (snippets.size < 12) {
                                    val start = (idx - 30).coerceAtLeast(0)
                                    val end = (idx + query.length + 30).coerceAtMost(trimmedLine.length)
                                    val snippetText = (if (start > 0) "..." else "") +
                                            trimmedLine.substring(start, end).trim() +
                                            (if (end < trimmedLine.length) "..." else "")

                                    snippets.add(
                                        SearchMatchSnippet(
                                            pageNumber = p,
                                            lineSnippet = snippetText,
                                            matchStart = idx,
                                            matchEnd = idx + query.length
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                stripper.startPage = 1
                stripper.endPage = totalPages
                try {
                    stripper.writeText(doc, java.io.StringWriter())
                } catch (e: InterruptedException) {
                    return null
                } catch (_: Exception) {}

                if (matchCount > 0) {
                    return FileSearchResult(
                        file = file,
                        totalMatches = matchCount,
                        snippets = snippets
                    )
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
