package com.pocketai.agent.data.terminal

import com.pocketai.agent.data.security.LogRedactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class CommandResult(
    val command: String,
    val output: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val isDestructive: Boolean = false
)

class SandboxedTerminal(
    private val workspaceDirectory: File
) {
    private val commandHistory = mutableListOf<String>()
    private val isCancelled = AtomicBoolean(false)

    fun getHistory(): List<String> = commandHistory.toList()

    fun cancelActive() {
        isCancelled.set(true)
    }

    fun isCommandDestructive(cmd: String): Boolean {
        val lower = cmd.trim().lowercase()
        return lower.startsWith("rm ") ||
                lower.contains("rm -rf") ||
                lower.contains("rmdir") ||
                lower.contains("mkfs") ||
                lower.contains("dd if=") ||
                lower.contains("chmod 777")
    }

    suspend fun executeCommand(commandStr: String): CommandResult = withContext(Dispatchers.IO) {
        val trimmed = commandStr.trim()
        if (trimmed.isBlank()) {
            return@withContext CommandResult("", "", 0, 0)
        }
        commandHistory.add(trimmed)
        isCancelled.set(false)

        val startTime = System.currentTimeMillis()
        val tokens = trimmed.split(Regex("\\s+"))
        val primaryCmd = tokens[0].lowercase()

        // Verify safety - protect outside workspace
        if (trimmed.contains("../..") || trimmed.contains("/etc") || trimmed.contains("/data/data")) {
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext CommandResult(
                command = trimmed,
                output = "Security Error: Command references paths outside the sandboxed project workspace.",
                exitCode = 1,
                executionTimeMs = elapsed
            )
        }

        val output = try {
            when (primaryCmd) {
                "help" -> {
                    """
                    Pocket AI Sandboxed Terminal
                    Supported built-in commands:
                      pwd               Print current workspace path
                      ls [-la] [dir]    List directory contents
                      cat <file>        Display file contents
                      echo <text>       Display text
                      mkdir <dir>       Create directory
                      touch <file>      Create empty file
                      rm <file>         Delete file (requires safety check)
                      grep <pattern>    Search in files
                      find              List all project files
                      stat <file>       Show file information
                      clear             Clear terminal screen
                      help              Display this guide
                    """.trimIndent()
                }
                "pwd" -> workspaceDirectory.absolutePath
                "ls" -> {
                    val targetDir = if (tokens.size > 1 && !tokens[1].startsWith("-")) {
                        resolveSafeFile(tokens[1])
                    } else workspaceDirectory

                    if (!targetDir.exists()) {
                        "ls: cannot access '${targetDir.name}': No such file or directory"
                    } else {
                        val files = targetDir.listFiles() ?: emptyArray()
                        files.joinToString("\n") { f ->
                            val prefix = if (f.isDirectory) "d " else "- "
                            "$prefix ${f.name.padEnd(25)} ${f.length()} B"
                        }
                    }
                }
                "cat" -> {
                    if (tokens.size < 2) "cat: missing file operand"
                    else {
                        val file = resolveSafeFile(tokens[1])
                        if (file.exists() && file.isFile) file.readText()
                        else "cat: ${tokens[1]}: No such file or is a directory"
                    }
                }
                "echo" -> {
                    tokens.drop(1).joinToString(" ")
                }
                "mkdir" -> {
                    if (tokens.size < 2) "mkdir: missing operand"
                    else {
                        val dir = resolveSafeFile(tokens[1])
                        if (dir.mkdirs()) "Directory created: ${dir.name}"
                        else "Directory already exists or could not be created"
                    }
                }
                "touch" -> {
                    if (tokens.size < 2) "touch: missing file operand"
                    else {
                        val file = resolveSafeFile(tokens[1])
                        file.parentFile?.mkdirs()
                        if (file.createNewFile()) "Created: ${file.name}" else "Updated timestamp: ${file.name}"
                    }
                }
                "rm" -> {
                    if (tokens.size < 2) "rm: missing operand"
                    else {
                        val file = resolveSafeFile(tokens[tokens.size - 1])
                        if (file.exists()) {
                            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                            if (deleted) "Deleted: ${file.name}" else "rm: failed to delete ${file.name}"
                        } else "rm: cannot remove '${tokens[tokens.size - 1]}': No such file"
                    }
                }
                "find" -> {
                    val list = mutableListOf<String>()
                    workspaceDirectory.walkTopDown().forEach {
                        list.add("./" + it.relativeTo(workspaceDirectory).path)
                    }
                    list.joinToString("\n")
                }
                "grep" -> {
                    if (tokens.size < 2) "grep: missing search term"
                    else {
                        val pattern = tokens[1]
                        val matches = mutableListOf<String>()
                        workspaceDirectory.walkTopDown().filter { it.isFile }.forEach { f ->
                            f.useLines { lines ->
                                lines.forEachIndexed { i, line ->
                                    if (line.contains(pattern, ignoreCase = true)) {
                                        matches.add("${f.relativeTo(workspaceDirectory).path}:${i + 1}: $line")
                                    }
                                }
                            }
                        }
                        if (matches.isEmpty()) "No matches found for '$pattern'" else matches.joinToString("\n")
                    }
                }
                else -> {
                    "pocket-sh: command not found: $primaryCmd. Type 'help' for available commands."
                }
            }
        } catch (e: Exception) {
            "Execution error: ${LogRedactor.redact(e.message)}"
        }

        val elapsed = System.currentTimeMillis() - startTime
        CommandResult(
            command = trimmed,
            output = LogRedactor.redact(output),
            exitCode = 0,
            executionTimeMs = elapsed,
            isDestructive = isCommandDestructive(trimmed)
        )
    }

    private fun resolveSafeFile(relativePath: String): File {
        val file = File(workspaceDirectory, relativePath)
        val canonical = file.canonicalPath
        if (!canonical.startsWith(workspaceDirectory.canonicalPath)) {
            throw SecurityException("Access denied: path escapes workspace boundaries.")
        }
        return file
    }
}
