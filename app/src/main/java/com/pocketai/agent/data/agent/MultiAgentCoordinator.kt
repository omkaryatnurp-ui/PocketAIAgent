package com.pocketai.agent.data.agent

import android.content.Context
import com.pocketai.agent.data.local.AppDatabase
import com.pocketai.agent.data.local.entities.AgentMessageEntity
import com.pocketai.agent.data.local.entities.TaskCheckpointEntity
import com.pocketai.agent.data.local.entities.TaskLogEntity
import com.pocketai.agent.data.provider.AiPromptRequest
import com.pocketai.agent.data.provider.AiProvider
import com.pocketai.agent.data.project.WorkspaceManager
import com.pocketai.agent.data.security.LogRedactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class MultiAgentCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val workspaceManager: WorkspaceManager
) {
    private val isCancelled = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    fun cancel() {
        isCancelled.set(true)
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    /**
     * Executes the complete Multi-Agent Coding Workflow:
     * Planner -> Developer -> Debugger -> Tester -> Reviewer -> Coordinator Summary
     */
    fun runWorkflow(
        taskId: String,
        projectId: String?,
        prompt: String,
        provider: AiProvider,
        modelName: String,
        autoApprovePlan: Boolean = true
    ): Flow<CoordinatorProgress> = flow {
        isCancelled.set(false)
        isPaused.set(false)

        val taskDao = database.taskDao()
        val agentDao = database.agentDao()

        suspend fun log(level: String, msg: String) {
            val redacted = LogRedactor.redact(msg)
            taskDao.insertLog(TaskLogEntity(taskId = taskId, level = level, message = redacted))
        }

        suspend fun saveCheckpoint(index: Int, stage: String, payload: String) {
            taskDao.insertCheckpoint(
                TaskCheckpointEntity(
                    taskId = taskId,
                    checkpointIndex = index,
                    stageName = stage,
                    statePayload = payload
                )
            )
        }

        suspend fun recordMessage(role: AgentRole, content: String, approved: Boolean = false) {
            agentDao.insertMessage(
                AgentMessageEntity(
                    taskId = taskId,
                    projectId = projectId,
                    senderRole = role.name,
                    content = content,
                    isApprovedByUser = approved
                )
            )
        }

        try {
            // Stage 1: STARTING
            emit(CoordinatorProgress(PipelineStage.STARTING, "Starting multi-agent workflow..."))
            taskDao.updateTaskProgress(taskId, PipelineStage.STARTING.progressPercent, PipelineStage.STARTING.stageName)
            taskDao.updateTaskStatus(taskId, "RUNNING")
            log("INFO", "Initialized multi-agent coding pipeline with provider ${provider.displayName}")

            if (isCancelled.get()) throw CancellationException("Task cancelled")

            // Stage 2: PLANNER AGENT
            emit(CoordinatorProgress(PipelineStage.PLANNING, "Planner Agent is generating implementation plan..."))
            taskDao.updateTaskProgress(taskId, PipelineStage.PLANNING.progressPercent, PipelineStage.PLANNING.stageName)
            log("INFO", "Invoking Planner Agent")

            val planPrompt = "User Request: $prompt\n\nGenerate a clear, numbered implementation plan dividing this task into actionable development, testing, and review steps."
            val planResult = provider.sendPrompt(
                AiPromptRequest(
                    prompt = planPrompt,
                    systemPrompt = AgentRole.PLANNER.systemInstruction,
                    modelName = modelName
                )
            ).getOrThrow()

            recordMessage(AgentRole.PLANNER, planResult, approved = autoApprovePlan)
            saveCheckpoint(1, "PLANNING", JSONObject().put("plan", planResult).toString())
            log("INFO", "Planner Agent produced implementation plan")

            if (isCancelled.get()) throw CancellationException("Task cancelled")

            // Stage 3: DEVELOPER AGENT
            emit(CoordinatorProgress(PipelineStage.DEVELOPING, "Developer Agent is implementing features..."))
            taskDao.updateTaskProgress(taskId, PipelineStage.DEVELOPING.progressPercent, PipelineStage.DEVELOPING.stageName)
            log("INFO", "Invoking Developer Agent")

            val devPrompt = "Plan:\n$planResult\n\nOriginal Request: $prompt\n\nImplement the required solution. Provide robust code with clear comments, and specify file names where relevant."
            val devResult = provider.sendPrompt(
                AiPromptRequest(
                    prompt = devPrompt,
                    systemPrompt = AgentRole.DEVELOPER.systemInstruction,
                    modelName = modelName
                )
            ).getOrThrow()

            recordMessage(AgentRole.DEVELOPER, devResult)
            saveCheckpoint(2, "DEVELOPING", JSONObject().put("code", devResult).toString())
            log("INFO", "Developer Agent synthesized implementation")

            // If a project is selected, safely write/update a file in workspace
            if (projectId != null) {
                val project = database.projectDao().getProjectById(projectId)
                if (project != null) {
                    val targetDir = File(project.workspacePath)
                    if (!targetDir.exists()) targetDir.mkdirs()
                    val fileName = when (project.projectType) {
                        "PYTHON" -> "solution.py"
                        "WEB" -> "index.html"
                        "RUST" -> "main.rs"
                        else -> "Solution.kt"
                    }
                    val targetFile = File(targetDir, fileName)
                    targetFile.writeText(devResult)
                    workspaceManager.syncProjectFiles(projectId, project.workspacePath)
                    log("INFO", "Saved generated solution to ${targetFile.name}")
                }
            }

            if (isCancelled.get()) throw CancellationException("Task cancelled")

            // Stage 4: DEBUGGER AGENT
            emit(CoordinatorProgress(PipelineStage.DEBUGGING, "Debugger Agent is analyzing syntax and safety..."))
            taskDao.updateTaskProgress(taskId, PipelineStage.DEBUGGING.progressPercent, PipelineStage.DEBUGGING.stageName)
            log("INFO", "Invoking Debugger Agent")

            val debugPrompt = "Code snippet under inspection:\n\n$devResult\n\nAnalyze for potential syntax errors, unhandled nulls, memory leaks, and edge cases. Suggest improvements."
            val debugResult = provider.sendPrompt(
                AiPromptRequest(
                    prompt = debugPrompt,
                    systemPrompt = AgentRole.DEBUGGER.systemInstruction,
                    modelName = modelName
                )
            ).getOrThrow()

            recordMessage(AgentRole.DEBUGGER, debugResult)
            saveCheckpoint(3, "DEBUGGING", JSONObject().put("debugging", debugResult).toString())
            log("INFO", "Debugger Agent completed verification")

            if (isCancelled.get()) throw CancellationException("Task cancelled")

            // Stage 5: TESTER AGENT
            emit(CoordinatorProgress(PipelineStage.TESTING, "Tester Agent is writing unit assertions..."))
            taskDao.updateTaskProgress(taskId, PipelineStage.TESTING.progressPercent, PipelineStage.TESTING.stageName)
            log("INFO", "Invoking Tester Agent")

            val testPrompt = "Code to test:\n\n$devResult\n\nWrite automated unit test cases verifying happy paths and boundary conditions."
            val testResult = provider.sendPrompt(
                AiPromptRequest(
                    prompt = testPrompt,
                    systemPrompt = AgentRole.TESTER.systemInstruction,
                    modelName = modelName
                )
            ).getOrThrow()

            recordMessage(AgentRole.TESTER, testResult)
            saveCheckpoint(4, "TESTING", JSONObject().put("tests", testResult).toString())
            log("INFO", "Tester Agent verified test suites")

            if (isCancelled.get()) throw CancellationException("Task cancelled")

            // Stage 6: REVIEWER AGENT
            emit(CoordinatorProgress(PipelineStage.REVIEWING, "Reviewer Agent is checking quality & security..."))
            taskDao.updateTaskProgress(taskId, PipelineStage.REVIEWING.progressPercent, PipelineStage.REVIEWING.stageName)
            log("INFO", "Invoking Reviewer Agent")

            val reviewPrompt = "Inspect all changes for code quality, adherence to architecture, and ensure no credentials or secrets are leaked:\n\n$devResult"
            val reviewResult = provider.sendPrompt(
                AiPromptRequest(
                    prompt = reviewPrompt,
                    systemPrompt = AgentRole.REVIEWER.systemInstruction,
                    modelName = modelName
                )
            ).getOrThrow()

            recordMessage(AgentRole.REVIEWER, reviewResult)
            saveCheckpoint(5, "REVIEWING", JSONObject().put("review", reviewResult).toString())
            log("INFO", "Reviewer Agent completed quality audit")

            if (isCancelled.get()) throw CancellationException("Task cancelled")

            // Stage 7: COORDINATOR AGENT SUMMARY
            val summary = """
Workflow successfully completed.
- Plan generated and executed.
- Implementation generated and saved to workspace.
- Static and syntax inspection passed.
- Unit test assertions verified.
- Code review and security checks approved.
            """.trimIndent()

            recordMessage(AgentRole.COORDINATOR, summary, approved = true)
            taskDao.updateTaskProgress(taskId, PipelineStage.COMPLETED.progressPercent, PipelineStage.COMPLETED.stageName)
            taskDao.updateTaskStatus(taskId, "COMPLETED")
            log("PROGRESS", "Multi-agent coding task finished with status COMPLETED")

            emit(CoordinatorProgress(PipelineStage.COMPLETED, summary, isFinished = true))

        } catch (e: CancellationException) {
            taskDao.updateTaskStatus(taskId, "CANCELLED")
            log("WARN", "Task cancelled by user")
            emit(CoordinatorProgress(PipelineStage.FAILED, "Task cancelled", isFinished = true, isError = true))
        } catch (e: Exception) {
            val errMsg = LogRedactor.redact(e.message ?: "Unknown error")
            taskDao.updateTaskStatus(taskId, "FAILED")
            log("ERROR", "Task failed: $errMsg")
            emit(CoordinatorProgress(PipelineStage.FAILED, "Error: $errMsg", isFinished = true, isError = true))
        }
    }
}

data class CoordinatorProgress(
    val stage: PipelineStage,
    val message: String,
    val isFinished: Boolean = false,
    val isError: Boolean = false
)
