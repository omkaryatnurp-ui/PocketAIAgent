package com.pocketai.agent.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.pocketai.agent.data.device.BatteryProfile
import com.pocketai.agent.data.device.DeviceManager
import com.pocketai.agent.data.git.GitManager
import com.pocketai.agent.data.local.AppDatabase
import com.pocketai.agent.data.local.entities.ProjectEntity
import com.pocketai.agent.data.local.entities.TaskRecordEntity
import com.pocketai.agent.data.project.WorkspaceManager
import com.pocketai.agent.data.project.ZipExporter
import com.pocketai.agent.data.provider.ProviderRegistry
import com.pocketai.agent.data.security.SecureKeyStorage
import com.pocketai.agent.data.service.TaskExecutionService
import com.pocketai.agent.ui.screens.*
import com.pocketai.agent.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder

@Composable
fun AppNavigation(
    navController: NavHostController,
    database: AppDatabase,
    providerRegistry: ProviderRegistry,
    secureStorage: SecureKeyStorage,
    workspaceManager: WorkspaceManager,
    gitManager: GitManager,
    deviceManager: DeviceManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val projects by database.projectDao().getAllProjects().collectAsState(initial = emptyList())
    val providers by database.aiProviderDao().getAllProviders().collectAsState(initial = emptyList())
    val tasks by database.taskDao().getAllTasks().collectAsState(initial = emptyList())
    val recentActivity by database.agentDao().getRecentAgentMessages(8).collectAsState(initial = emptyList())
    val usageList by database.aiProviderDao().getAllUsage().collectAsState(initial = emptyList())
    val totalCost by database.aiProviderDao().getTotalEstimatedCost().collectAsState(initial = 0.0)
    val totalTokens by database.aiProviderDao().getTotalTokens().collectAsState(initial = 0L)

    var currentBatteryProfile by remember { mutableStateOf(BatteryProfile.BALANCED) }

    val activeTasks = tasks.filter { it.status == "RUNNING" || it.status == "STARTING" }
    val queuedTasks = tasks.filter { it.status == "QUEUED" }
    val activeProvider = providers.find { it.hasKeyConfigured } ?: providers.find { it.isFreeTier } ?: providers.firstOrNull()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        containerColor = CharcoalDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    BottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OrangeAccent,
                                selectedTextColor = OrangeAccent,
                                indicatorColor = OrangeGlow,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // 1. Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = { _ ->
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Onboarding Screen
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // 3. Dashboard Screen
            composable(Screen.Dashboard.route) {
                val metrics = remember { deviceManager.getDeviceMetrics(activeTasks.size) }
                DashboardScreen(
                    activeProvider = activeProvider,
                    runningTasks = activeTasks,
                    queuedTasks = queuedTasks,
                    recentProjects = projects,
                    recentActivity = recentActivity,
                    deviceMetrics = metrics,
                    onNavigateToProject = { id -> navController.navigate(Screen.ProjectDetails.createRoute(id)) },
                    onNavigateToAgent = { navController.navigate(Screen.AiAgent.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToDeviceManager = { navController.navigate(Screen.DeviceManager.route) },
                    onNavigateToProviders = { navController.navigate(Screen.ProviderManager.route) },
                    onNavigateToNewProject = { navController.navigate(Screen.Projects.route) }
                )
            }

            // 4. Projects Screen
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    projects = projects,
                    onSelectProject = { id -> navController.navigate(Screen.ProjectDetails.createRoute(id)) },
                    onCreateProject = { name, desc, type ->
                        coroutineScope.launch {
                            val created = workspaceManager.createProject(name, desc, type)
                            Toast.makeText(context, "Project '${created.name}' created", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteProject = { id ->
                        coroutineScope.launch {
                            database.projectDao().deleteProjectById(id)
                            Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExportProjectZip = { project ->
                        coroutineScope.launch {
                            val zipFile = File(context.cacheDir, "${project.name}.zip")
                            ZipExporter.exportProjectToZip(File(project.workspacePath), zipFile)
                            Toast.makeText(context, "Exported ZIP to ${zipFile.name}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // 5. Project Details Screen
            composable(
                route = Screen.ProjectDetails.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val project by database.projectDao().observeProjectById(projectId).collectAsState(initial = null)
                val files by database.projectDao().getFilesForProject(projectId).collectAsState(initial = emptyList())

                ProjectDetailsScreen(
                    project = project,
                    files = files,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFile = { relPath ->
                        navController.navigate(Screen.CodeEditor.createRoute(projectId, relPath))
                    },
                    onOpenTerminal = { id ->
                        navController.navigate(Screen.Terminal.createRoute(id))
                    },
                    onStartAiTask = { id ->
                        navController.navigate(Screen.AiAgent.route)
                    },
                    onExportZip = { p ->
                        coroutineScope.launch {
                            val zipFile = File(context.cacheDir, "${p.name}.zip")
                            ZipExporter.exportProjectToZip(File(p.workspacePath), zipFile)
                            Toast.makeText(context, "ZIP exported: ${zipFile.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCreateFile = { fileName ->
                        coroutineScope.launch {
                            project?.let { p ->
                                workspaceManager.writeFileContent(p.workspacePath, fileName, "// New file $fileName\n")
                                workspaceManager.syncProjectFiles(projectId, p.workspacePath)
                            }
                        }
                    },
                    onDeleteFile = { relPath ->
                        coroutineScope.launch {
                            project?.let { p ->
                                workspaceManager.deleteFile(projectId, p.workspacePath, relPath)
                            }
                        }
                    }
                )
            }

            // 6. Code Editor Screen
            composable(
                route = Screen.CodeEditor.route,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("filePath") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val filePath = URLDecoder.decode(encodedPath, "UTF-8")

                var fileContent by remember { mutableStateOf("") }
                val project by database.projectDao().observeProjectById(projectId).collectAsState(initial = null)

                LaunchedEffect(project, filePath) {
                    project?.let { p ->
                        val read = workspaceManager.readFileContent(p.workspacePath, filePath)
                        fileContent = read.getOrDefault("")
                    }
                }

                CodeEditorScreen(
                    filePath = filePath,
                    initialContent = fileContent,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveContent = { newText ->
                        coroutineScope.launch {
                            project?.let { p ->
                                workspaceManager.writeFileContent(p.workspacePath, filePath, newText)
                                workspaceManager.syncProjectFiles(projectId, p.workspacePath)
                                Toast.makeText(context, "Saved $filePath", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 7. Terminal Screen
            composable(
                route = Screen.Terminal.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val project by database.projectDao().observeProjectById(projectId).collectAsState(initial = null)

                TerminalScreen(
                    projectName = project?.name ?: "Project",
                    workspacePath = project?.workspacePath ?: context.filesDir.absolutePath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 8. AI Agent Screen
            composable(Screen.AiAgent.route) {
                AiAgentScreen(
                    providers = providers,
                    projects = projects,
                    providerRegistry = providerRegistry,
                    onNavigateToProviders = { navController.navigate(Screen.ProviderManager.route) },
                    onLaunchMultiAgentWorkflow = { projId, prompt, provId, model ->
                        coroutineScope.launch {
                            val taskId = java.util.UUID.randomUUID().toString()
                            val taskRecord = TaskRecordEntity(
                                id = taskId,
                                projectId = projId,
                                title = prompt.take(40),
                                prompt = prompt,
                                providerId = provId,
                                modelName = model,
                                status = "RUNNING"
                            )
                            database.taskDao().insertTask(taskRecord)
                            TaskExecutionService.startTask(context, taskId, taskRecord.title, projId, prompt, provId, model)
                            navController.navigate(Screen.MultiAgentTeam.createRoute(taskId))
                        }
                    }
                )
            }

            // 9. AI Provider Manager Screen
            composable(Screen.ProviderManager.route) {
                AiProviderManagerScreen(
                    providers = providers,
                    providerRegistry = providerRegistry,
                    secureStorage = secureStorage,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveApiKey = { providerId, apiKey ->
                        secureStorage.saveApiKey(providerId, apiKey)
                        coroutineScope.launch {
                            database.aiProviderDao().updateKeyConfigured(providerId, true)
                        }
                    },
                    onRemoveApiKey = { providerId ->
                        secureStorage.removeApiKey(providerId)
                        coroutineScope.launch {
                            database.aiProviderDao().updateKeyConfigured(providerId, false)
                        }
                    },
                    onSelectModel = { providerId ->
                        navController.navigate(Screen.ModelSelector.route)
                    }
                )
            }

            // 10. Model Selector Screen
            composable(Screen.ModelSelector.route) {
                ModelSelectorScreen(
                    providerRegistry = providerRegistry,
                    onNavigateBack = { navController.popBackStack() },
                    onSelectModel = { providerId, modelId ->
                        coroutineScope.launch {
                            database.aiProviderDao().updateSelectedModel(providerId, modelId)
                            Toast.makeText(context, "Selected model: $modelId", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 11. Multi-Agent Team Screen
            composable(
                route = Screen.MultiAgentTeam.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val task by database.taskDao().observeTaskById(taskId).collectAsState(initial = null)
                val messages by database.agentDao().getMessagesForTask(taskId).collectAsState(initial = emptyList())

                MultiAgentTeamScreen(
                    task = task,
                    messages = messages,
                    onNavigateBack = { navController.popBackStack() },
                    onPauseTask = { id ->
                        val intent = android.content.Intent(context, TaskExecutionService::class.java).apply {
                            action = NotificationHelper.ACTION_PAUSE
                        }
                        context.startService(intent)
                    },
                    onResumeTask = { id ->
                        val intent = android.content.Intent(context, TaskExecutionService::class.java).apply {
                            action = NotificationHelper.ACTION_RESUME
                        }
                        context.startService(intent)
                    },
                    onCancelTask = { id ->
                        val intent = android.content.Intent(context, TaskExecutionService::class.java).apply {
                            action = NotificationHelper.ACTION_CANCEL
                        }
                        context.startService(intent)
                    },
                    onApprovePlan = { messageId ->
                        coroutineScope.launch {
                            database.agentDao().updateMessageApproval(messageId, true)
                            Toast.makeText(context, "Plan Approved! Developer Agent resuming.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onNavigateToProjectEditor = { projId ->
                        navController.navigate(Screen.ProjectDetails.createRoute(projId))
                    }
                )
            }

            // 12. Tasks Queue Screen
            composable(Screen.Tasks.route) {
                TaskQueueScreen(
                    tasks = tasks,
                    onSelectTask = { id -> navController.navigate(Screen.TaskDetails.createRoute(id)) },
                    onPauseTask = { id ->
                        val intent = android.content.Intent(context, TaskExecutionService::class.java).apply {
                            action = NotificationHelper.ACTION_PAUSE
                        }
                        context.startService(intent)
                    },
                    onResumeTask = { id ->
                        val intent = android.content.Intent(context, TaskExecutionService::class.java).apply {
                            action = NotificationHelper.ACTION_RESUME
                        }
                        context.startService(intent)
                    },
                    onCancelTask = { id ->
                        val intent = android.content.Intent(context, TaskExecutionService::class.java).apply {
                            action = NotificationHelper.ACTION_CANCEL
                        }
                        context.startService(intent)
                    },
                    onRetryTask = { task ->
                        coroutineScope.launch {
                            database.taskDao().updateTaskStatus(task.id, "RUNNING")
                            TaskExecutionService.startTask(
                                context,
                                task.id,
                                task.title,
                                task.projectId,
                                task.prompt,
                                task.providerId,
                                task.modelName
                            )
                        }
                    },
                    onDeleteTask = { id ->
                        coroutineScope.launch {
                            database.taskDao().deleteTaskById(id)
                        }
                    },
                    onClearCompleted = {
                        coroutineScope.launch {
                            database.taskDao().clearCompletedTasks()
                        }
                    }
                )
            }

            // 13. Task Details Screen
            composable(
                route = Screen.TaskDetails.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val task by database.taskDao().observeTaskById(taskId).collectAsState(initial = null)
                val logs by database.taskDao().getLogsForTask(taskId).collectAsState(initial = emptyList())
                val checkpoints by database.taskDao().getCheckpointsForTask(taskId).collectAsState(initial = emptyList())

                TaskDetailsScreen(
                    task = task,
                    logs = logs,
                    checkpoints = checkpoints,
                    onNavigateBack = { navController.popBackStack() },
                    onRestoreCheckpoint = { cp ->
                        coroutineScope.launch {
                            task?.let { t ->
                                database.taskDao().updateTaskProgress(t.id, 50, cp.stageName)
                                Toast.makeText(context, "Restored state from ${cp.stageName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 14. Device Manager Screen
            composable(Screen.DeviceManager.route) {
                val metrics = remember { deviceManager.getDeviceMetrics(activeTasks.size) }
                DeviceManagerScreen(
                    metrics = metrics,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBatterySettings = { navController.navigate(Screen.BatterySettings.route) }
                )
            }

            // 15. Battery Settings Screen
            composable(Screen.BatterySettings.route) {
                BatterySettingsScreen(
                    currentProfile = currentBatteryProfile,
                    onSelectProfile = { currentBatteryProfile = it },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 16. Notification Settings Screen
            composable(Screen.NotificationSettings.route) {
                NotificationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 17. GitHub Integration Screen
            composable(Screen.GitHubIntegration.route) {
                GitHubIntegrationScreen(
                    secureStorage = secureStorage,
                    gitManager = gitManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 18. API Usage Screen
            composable(Screen.ApiUsage.route) {
                ApiUsageScreen(
                    usageList = usageList,
                    totalCost = totalCost ?: 0.0,
                    totalTokens = totalTokens ?: 0L,
                    providerRegistry = providerRegistry,
                    onNavigateBack = { navController.popBackStack() },
                    onClearUsage = {
                        coroutineScope.launch {
                            database.aiProviderDao().clearUsageHistory()
                        }
                    }
                )
            }

            // 19. Security & Privacy Screen
            composable(Screen.SecurityPrivacy.route) {
                SecurityPrivacyScreen(
                    secureStorage = secureStorage,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 20. Application Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToProviders = { navController.navigate(Screen.ProviderManager.route) },
                    onNavigateToUsage = { navController.navigate(Screen.ApiUsage.route) },
                    onNavigateToBattery = { navController.navigate(Screen.BatterySettings.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                    onNavigateToGitHub = { navController.navigate(Screen.GitHubIntegration.route) },
                    onNavigateToSecurity = { navController.navigate(Screen.SecurityPrivacy.route) },
                    onNavigateToDeviceManager = { navController.navigate(Screen.DeviceManager.route) }
                )
            }
        }
    }
}
