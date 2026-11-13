package com.pocketai.agent.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Onboarding : Screen("onboarding", "Welcome")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    object ProjectDetails : Screen("project_details/{projectId}", "Project Details") {
        fun createRoute(projectId: String) = "project_details/$projectId"
    }
    object CodeEditor : Screen("code_editor/{projectId}/{filePath}", "Code Editor") {
        fun createRoute(projectId: String, filePath: String) = "code_editor/$projectId/${java.net.URLEncoder.encode(filePath, "UTF-8")}"
    }
    object Terminal : Screen("terminal/{projectId}", "Terminal", Icons.Default.Terminal) {
        fun createRoute(projectId: String) = "terminal/$projectId"
    }
    object AiAgent : Screen("agent", "AI Agent", Icons.Default.SmartToy)
    object ProviderManager : Screen("providers", "AI Providers")
    object ModelSelector : Screen("models", "Select Model")
    object MultiAgentTeam : Screen("multi_agent/{taskId}", "Multi-Agent Team") {
        fun createRoute(taskId: String) = "multi_agent/$taskId"
    }
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Assignment)
    object TaskDetails : Screen("task_details/{taskId}", "Task Logs & Checkpoints") {
        fun createRoute(taskId: String) = "task_details/$taskId"
    }
    object DeviceManager : Screen("device_manager", "Device Manager", Icons.Default.Memory)
    object BatterySettings : Screen("battery_settings", "Battery & Background")
    object NotificationSettings : Screen("notification_settings", "Notifications")
    object GitHubIntegration : Screen("github", "GitHub Integration", Icons.Default.Code)
    object ApiUsage : Screen("usage", "API Usage & Quotas")
    object SecurityPrivacy : Screen("security", "Security & Keys")
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Projects,
    Screen.AiAgent,
    Screen.Tasks,
    Screen.Settings
)
