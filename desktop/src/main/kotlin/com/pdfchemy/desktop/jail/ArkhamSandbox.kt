package com.pdfchemy.desktop.jail

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

data class VerifiedWorker(
    val jvmPath: String,
    val verifiedJarPath: String,
    val classPath: String,
    val mainClass: String,
    val workerArgs: List<String>,
    val environment: Map<String, String>,
    val workingDirectory: File,
    val maxMemoryBytes: Long,
    val maxCpuPercentage: Int,
    val verifiedJarHash: String
) {
    fun toCommandList(): List<String> {
        return listOf(
            jvmPath, 
            "-Djava.awt.headless=true",
            "-Djava.io.tmpdir=${workingDirectory.absolutePath}",
            "-Duser.home=${workingDirectory.absolutePath}",
            "-Duser.dir=${workingDirectory.absolutePath}",
            "-Dpdfbox.fontcache=${workingDirectory.absolutePath}",
            "-XX:-UsePerfData",
            "-cp", 
            classPath, 
            mainClass
        ) + workerArgs
    }
}

data class SandboxCapabilitySnapshot(
    val osVersion: String,
    val kernelVersion: String,
    val sandboxBackendVersion: String,
    val containmentStrategy: String,
    val networkDenied: Boolean,
    
    // Core Identity
    val launcherHash: String,
    val workerHash: String,
    
    // Linux Specific
    val bwrapHash: String?,
    val bwrapVersion: String?,
    val sandboxPolicyHash: String?,
    val filesystemManifestHash: String?,
    val resourcePolicyHash: String?,
    val namespaceInodes: Map<String, String>?,
    val cgroupIdentity: String?,
    
    // Windows Specific
    val tokenAppContainerIdentity: String?,
    val capabilitySetHash: String?,
    val jobIdentity: String?,
    val creationTime: Long?
)

interface SandboxedProcess {
    val inputStream: InputStream
    val outputStream: OutputStream
    val errorStream: InputStream
    val pid: Long
    
    fun waitFor(timeout: Long, unit: TimeUnit): Boolean
    fun exitValue(): Int
    val isAlive: Boolean
}

enum class SandboxStatus {
    RUNNING, TERMINATED_CLEANLY, KILLED_BY_HOST, CRASHED
}

interface ArkhamSandbox {
    /** 
     * Launches the worker process atomically within the constrained OS environment.
     * The implementation (e.g. native launcher) MUST guarantee that either the process 
     * is returned already inside the required OS boundary, or the launch fails entirely 
     * without exposing the worker to the host.
     */
    @Throws(SecurityException::class)
    fun launch(worker: VerifiedWorker): SandboxedProcess
    
    /** 
     * Recursively terminates the entire process tree. 
     * E.g. Explicit TerminateJobObject on Windows, or PID namespace killing on Linux.
     */
    fun terminate()
    
    /** Inspects the sandbox state or exit reason. */
    fun inspect(): SandboxStatus
    
    /** Returns the capability fingerprint snapshot established at launch. */
    fun getCapabilitySnapshot(): SandboxCapabilitySnapshot
}

object ArkhamSandboxFactory {
    fun create(): ArkhamSandbox {
        val osName = System.getProperty("os.name").lowercase()
        
        return when {
            osName.contains("win") -> {
                // Windows uses the native C++ launcher. In a dev environment it might not be built.
                val userDir = System.getProperty("user.dir")
                val launcherPath = if (userDir.endsWith("desktop")) {
                    Paths.get(userDir, "src", "main", "cpp", "arkham-launcher.exe")
                } else {
                    Paths.get(userDir, "desktop", "src", "main", "cpp", "arkham-launcher.exe")
                }
                if (!Files.exists(launcherPath)) {
                     // Normally we'd throw SecurityException, but for dev fallback if not compiled yet.
                     // Wait, the plan explicitly says NO INSECURE FALLBACK.
                     throw SecurityException("Arkham OS isolation unavailable; Windows native launcher not found at $launcherPath")
                }
                WindowsArkhamSandbox(launcherPath)
            }
            osName.contains("linux") -> {
                // Verify bubblewrap is available
                LinuxArkhamSandbox("bwrap")
            }
            else -> {
                throw SecurityException("Arkham OS isolation unavailable on OS: $osName; secure processing cannot start.")
            }
        }
    }
}
