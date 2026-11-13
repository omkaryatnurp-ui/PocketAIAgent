package com.pocketai.agent.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pocketai.agent.data.agent.MultiAgentCoordinator
import com.pocketai.agent.data.local.AppDatabase
import com.pocketai.agent.data.project.WorkspaceManager
import com.pocketai.agent.data.provider.ProviderRegistry
import kotlinx.coroutines.flow.collectLatest

class TaskWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(TaskExecutionService.EXTRA_TASK_ID) ?: return Result.failure()
        val projectId = inputData.getString(TaskExecutionService.EXTRA_PROJECT_ID)
        val prompt = inputData.getString(TaskExecutionService.EXTRA_PROMPT) ?: ""
        val providerId = inputData.getString(TaskExecutionService.EXTRA_PROVIDER_ID) ?: "FREE_LOCAL"
        val modelName = inputData.getString(TaskExecutionService.EXTRA_MODEL_NAME) ?: "local-rule-engine"

        val database = AppDatabase.getInstance(applicationContext)
        val providerRegistry = ProviderRegistry(applicationContext)
        val workspaceManager = WorkspaceManager(applicationContext, database)
        val coordinator = MultiAgentCoordinator(applicationContext, database, workspaceManager)

        return try {
            val provider = providerRegistry.getActiveConfiguredProvider(providerId)
            var isSuccess = false
            coordinator.runWorkflow(
                taskId = taskId,
                projectId = projectId,
                prompt = prompt,
                provider = provider,
                modelName = modelName
            ).collectLatest { progress ->
                if (progress.isFinished && !progress.isError) {
                    isSuccess = true
                }
            }

            if (isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
