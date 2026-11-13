package com.pocketai.agent.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.pocketai.agent.data.agent.MultiAgentCoordinator
import com.pocketai.agent.data.local.AppDatabase
import com.pocketai.agent.data.project.WorkspaceManager
import com.pocketai.agent.data.provider.ProviderRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class TaskExecutionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null
    private var coordinator: MultiAgentCoordinator? = null
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var database: AppDatabase
    private lateinit var providerRegistry: ProviderRegistry

    private var currentTaskId: String? = null
    private var currentTaskTitle: String = "AI Task"
    private var currentStage: String = "Starting"
    private var currentProgress: Int = 0
    private val isPaused = AtomicBoolean(false)

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_PROMPT = "extra_prompt"
        const val EXTRA_PROVIDER_ID = "extra_provider_id"
        const val EXTRA_MODEL_NAME = "extra_model_name"

        fun startTask(
            context: Context,
            taskId: String,
            taskTitle: String,
            projectId: String?,
            prompt: String,
            providerId: String,
            modelName: String
        ) {
            val intent = Intent(context, TaskExecutionService::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_PROMPT, prompt)
                putExtra(EXTRA_PROVIDER_ID, providerId)
                putExtra(EXTRA_MODEL_NAME, modelName)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTask(context: Context) {
            val intent = Intent(context, TaskExecutionService::class.java).apply {
                action = NotificationHelper.ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        database = AppDatabase.getInstance(this)
        providerRegistry = ProviderRegistry(this)
        val workspaceManager = WorkspaceManager(this, database)
        coordinator = MultiAgentCoordinator(this, database, workspaceManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            NotificationHelper.ACTION_PAUSE -> {
                pauseTask()
                return START_STICKY
            }
            NotificationHelper.ACTION_RESUME -> {
                resumeTask()
                return START_STICKY
            }
            NotificationHelper.ACTION_CANCEL -> {
                cancelTask()
                return START_NOT_STICKY
            }
        }

        val taskId = intent?.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Coding Task"
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: ""
        val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID) ?: "FREE_LOCAL"
        val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "local-rule-engine"

        currentTaskId = taskId
        currentTaskTitle = taskTitle
        currentStage = "Starting"
        currentProgress = 5

        // Start foreground with ongoing notification
        val notification = notificationHelper.buildForegroundNotification(
            taskTitle = currentTaskTitle,
            currentStage = currentStage,
            progressPercent = currentProgress,
            isPaused = false
        )
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)

        // Run multi-agent pipeline
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            val provider = providerRegistry.getActiveConfiguredProvider(providerId)
            coordinator?.runWorkflow(
                taskId = taskId,
                projectId = projectId,
                prompt = prompt,
                provider = provider,
                modelName = modelName
            )?.collectLatest { progress ->
                currentStage = progress.stage.stageName
                currentProgress = progress.stage.progressPercent

                val updatedNotification = notificationHelper.buildForegroundNotification(
                    taskTitle = currentTaskTitle,
                    currentStage = currentStage,
                    progressPercent = currentProgress,
                    isPaused = isPaused.get()
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, updatedNotification)

                if (progress.isFinished) {
                    if (progress.isError) {
                        notificationHelper.showTaskAlert("Task Failed", "$currentTaskTitle failed: ${progress.message}")
                    } else {
                        notificationHelper.showTaskAlert("Task Completed", "$currentTaskTitle completed successfully!")
                    }
                    stopForegroundTask()
                }
            }
        }

        return START_STICKY
    }

    private fun pauseTask() {
        isPaused.set(true)
        coordinator?.pause()
        currentTaskId?.let { id ->
            serviceScope.launch {
                database.taskDao().updateTaskStatus(id, "PAUSED")
            }
        }
        val notif = notificationHelper.buildForegroundNotification(
            taskTitle = currentTaskTitle,
            currentStage = "Paused",
            progressPercent = currentProgress,
            isPaused = true
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notif)
    }

    private fun resumeTask() {
        isPaused.set(false)
        coordinator?.resume()
        currentTaskId?.let { id ->
            serviceScope.launch {
                database.taskDao().updateTaskStatus(id, "RUNNING")
            }
        }
        val notif = notificationHelper.buildForegroundNotification(
            taskTitle = currentTaskTitle,
            currentStage = currentStage,
            progressPercent = currentProgress,
            isPaused = false
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notif)
    }

    private fun cancelTask() {
        coordinator?.cancel()
        activeJob?.cancel()
        currentTaskId?.let { id ->
            serviceScope.launch {
                database.taskDao().updateTaskStatus(id, "CANCELLED")
            }
        }
        notificationHelper.showTaskAlert("Task Cancelled", "$currentTaskTitle was cancelled by user")
        stopForegroundTask()
    }

    private fun stopForegroundTask() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
