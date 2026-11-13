package com.pocketai.agent.data.project

import android.content.Context
import com.pocketai.agent.data.local.AppDatabase
import com.pocketai.agent.data.local.entities.ProjectEntity
import com.pocketai.agent.data.local.entities.ProjectFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class WorkspaceManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val baseWorkspaceDir: File = File(context.filesDir, "workspaces").apply {
        if (!exists()) mkdirs()
    }

    suspend fun createProject(
        name: String,
        description: String,
        projectType: String
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val projectId = UUID.randomUUID().toString()
        val safeName = name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val projectDir = File(baseWorkspaceDir, "${safeName}_$projectId")
        projectDir.mkdirs()

        // Scaffold initial starter files based on projectType
        scaffoldStarterFiles(projectDir, projectType, name)

        val project = ProjectEntity(
            id = projectId,
            name = name,
            description = description,
            projectType = projectType,
            workspacePath = projectDir.absolutePath
        )

        database.projectDao().insertProject(project)
        syncProjectFiles(projectId, projectDir.absolutePath)
        project
    }

    private fun scaffoldStarterFiles(dir: File, type: String, name: String) {
        when (type.uppercase()) {
            "ANDROID", "KOTLIN" -> {
                val srcDir = File(dir, "src/main/kotlin").apply { mkdirs() }
                File(srcDir, "Main.kt").writeText(
                    """
                    package com.example.$name

                    fun main() {
                        println("Hello from Pocket AI Agent on Android!")
                    }
                    """.trimIndent()
                )
                File(dir, "README.md").writeText("# $name\nKotlin project created by Pocket AI Agent.")
            }
            "PYTHON" -> {
                File(dir, "main.py").writeText(
                    """
                    def main():
                        print("Hello from Pocket AI Python Workspace!")

                    if __name__ == "__main__":
                        main()
                    """.trimIndent()
                )
                File(dir, "requirements.txt").writeText("# Add Python dependencies here\n")
                File(dir, "README.md").writeText("# $name\nPython project created by Pocket AI Agent.")
            }
            "WEB" -> {
                File(dir, "index.html").writeText(
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>$name</title>
                        <link rel="stylesheet" href="style.css">
                    </head>
                    <body>
                        <h1>$name</h1>
                        <p>Built on mobile with Pocket AI Agent.</p>
                        <script src="app.js"></script>
                    </body>
                    </html>
                    """.trimIndent()
                )
                File(dir, "style.css").writeText("body { background: #121212; color: #EEE; font-family: sans-serif; }")
                File(dir, "app.js").writeText("console.log('Pocket AI Web App initialized');")
            }
            "RUST" -> {
                val srcDir = File(dir, "src").apply { mkdirs() }
                File(srcDir, "main.rs").writeText(
                    """
                    fn main() {
                        println!("Hello from Pocket AI Rust Project!");
                    }
                    """.trimIndent()
                )
                File(dir, "Cargo.toml").writeText(
                    """
                    [package]
                    name = "${name.lowercase()}"
                    version = "0.1.0"
                    edition = "2021"
                    """.trimIndent()
                )
            }
            "NODEJS" -> {
                File(dir, "index.js").writeText(
                    """
                    console.log("Pocket AI Node.js server starting...");
                    """.trimIndent()
                )
                File(dir, "package.json").writeText(
                    """
                    {
                      "name": "${name.lowercase()}",
                      "version": "1.0.0",
                      "main": "index.js"
                    }
                    """.trimIndent()
                )
            }
            else -> {
                File(dir, "main.txt").writeText("Pocket AI Project: $name\n")
                File(dir, "README.md").writeText("# $name\nCreated with Pocket AI Agent.")
            }
        }
    }

    suspend fun syncProjectFiles(projectId: String, workspacePath: String) = withContext(Dispatchers.IO) {
        val rootDir = File(workspacePath)
        if (!rootDir.exists()) return@withContext

        val entities = mutableListOf<ProjectFileEntity>()
        var totalSize = 0L
        var count = 0

        rootDir.walkTopDown().forEach { file ->
            if (file.absolutePath != rootDir.absolutePath) {
                val rel = file.relativeTo(rootDir).path
                val isDir = file.isDirectory
                val size = if (isDir) 0L else file.length()
                totalSize += size
                count++

                entities.add(
                    ProjectFileEntity(
                        projectId = projectId,
                        relativePath = rel,
                        fileName = file.name,
                        fileExtension = file.extension,
                        sizeBytes = size,
                        lastModified = file.lastModified(),
                        isDirectory = isDir
                    )
                )
            }
        }

        database.projectDao().deleteAllFilesForProject(projectId)
        database.projectDao().insertFiles(entities)

        // Update project stats
        val project = database.projectDao().getProjectById(projectId)
        if (project != null) {
            database.projectDao().updateProject(
                project.copy(
                    fileCount = count,
                    totalSizeBytes = totalSize,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun readFileContent(projectPath: String, relativePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(projectPath, relativePath)
            if (!file.exists()) return@withContext Result.failure(NoSuchFileException(file))
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFileContent(projectPath: String, relativePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(projectPath, relativePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(projectId: String, projectPath: String, relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(projectPath, relativePath)
            if (file.exists()) {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            syncProjectFiles(projectId, projectPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchInFiles(projectPath: String, query: String): List<FileSearchResult> = withContext(Dispatchers.IO) {
        val root = File(projectPath)
        val results = mutableListOf<FileSearchResult>()
        if (!root.exists() || query.isBlank()) return@withContext results

        root.walkTopDown().filter { it.isFile }.forEach { file ->
            try {
                file.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        if (line.contains(query, ignoreCase = true)) {
                            results.add(
                                FileSearchResult(
                                    relativePath = file.relativeTo(root).path,
                                    lineNumber = index + 1,
                                    lineContent = line.trim()
                                )
                            )
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
        results
    }
}

data class FileSearchResult(
    val relativePath: String,
    val lineNumber: Int,
    val lineContent: String
)
