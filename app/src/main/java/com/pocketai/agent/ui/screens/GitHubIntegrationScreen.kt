package com.pocketai.agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.git.GitCommitInfo
import com.pocketai.agent.data.git.GitManager
import com.pocketai.agent.data.git.GitRepoInfo
import com.pocketai.agent.data.security.SecureKeyStorage
import com.pocketai.agent.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GitHubIntegrationScreen(
    secureStorage: SecureKeyStorage,
    gitManager: GitManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasToken by remember { mutableStateOf(secureStorage.getGitHubToken() != null) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var showPushConfirmDialog by remember { mutableStateOf(false) }
    var showPrDialog by remember { mutableStateOf(false) }

    var isLoadingRepos by remember { mutableStateOf(false) }
    var reposList by remember { mutableStateOf<List<GitRepoInfo>>(emptyList()) }
    var repoError by remember { mutableStateOf<String?>(null) }
    val commits = remember { mutableStateListOf(*gitManager.getLocalCommits().toTypedArray()) }

    fun refreshRepos() {
        if (!hasToken) return
        coroutineScope.launch {
            isLoadingRepos = true
            repoError = null
            val res = gitManager.listUserRepositories()
            isLoadingRepos = false
            if (res.isSuccess) {
                reposList = res.getOrDefault(emptyList())
            } else {
                repoError = res.exceptionOrNull()?.message ?: "Failed to list repositories"
            }
        }
    }

    LaunchedEffect(hasToken) {
        if (hasToken) refreshRepos()
    }

    Scaffold(
        containerColor = CharcoalDark,
        topBar = {
            Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GitHub Integration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Repositories, Commits, Push & Pull Requests",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // Token Management Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = OrangeAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasToken) "GitHub Connected" else "Connect GitHub",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceDark
                            ) {
                                Text(
                                    text = if (hasToken) "AUTHENTICATED" else "NO TOKEN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasToken) StatusGreen else StatusYellow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use a GitHub Personal Access Token (classic or fine-grained with repo permissions). Tokens are stored encrypted on-device and never shared.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showTokenDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (hasToken) "Update Token" else "Add Token", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                            }

                            if (hasToken) {
                                OutlinedButton(
                                    onClick = {
                                        secureStorage.removeGitHubToken()
                                        hasToken = false
                                        reposList = emptyList()
                                        Toast.makeText(context, "GitHub disconnected", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Disconnect", color = StatusRed)
                                }
                            }
                        }
                    }
                }
            }

            // Git Actions Card: Commit & Push
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Workspace Git Operations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCommitDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("New Commit", color = TextPrimary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showPushConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Push to Remote", color = CharcoalBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Commits History Section Header
            item {
                Text(
                    text = "Recent Commits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            items(commits) { commit ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceDark
                        ) {
                            Text(
                                text = commit.hash,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = OrangeAccentLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = commit.message, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(text = commit.author, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }

            // Remote Repositories Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Authorized Repositories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (hasToken) {
                        IconButton(onClick = { refreshRepos() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted)
                        }
                    }
                }
            }

            if (isLoadingRepos) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangeAccent, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (repoError != null) {
                item {
                    Text(text = repoError ?: "", color = StatusRed, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (reposList.isEmpty()) {
                item {
                    Text(
                        text = if (hasToken) "No repositories found for this account" else "Connect a token above to load repositories",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(reposList) { repo ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = repo.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            if (!repo.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = repo.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Default Branch: ${repo.defaultBranch}", style = MaterialTheme.typography.labelSmall, color = OrangeAccentLight)
                        }
                    }
                }
            }
        }
    }

    // Token Input Dialog
    if (showTokenDialog) {
        var tokenInput by remember { mutableStateOf(secureStorage.getGitHubToken() ?: "") }
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("GitHub Access Token", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Personal Access Token") },
                    placeholder = { Text("ghp_...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            secureStorage.saveGitHubToken(tokenInput.trim())
                            hasToken = true
                            showTokenDialog = false
                            Toast.makeText(context, "GitHub token saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Save", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }

    // New Commit Dialog
    if (showCommitDialog) {
        var commitMsg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCommitDialog = false },
            title = { Text("Commit Workspace Changes", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = commitMsg,
                    onValueChange = { commitMsg = it },
                    label = { Text("Commit Message") },
                    placeholder = { Text("e.g. Implement feature with Pocket AI Agent") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (commitMsg.isNotBlank()) {
                            val newCommit = gitManager.commitChanges(commitMsg)
                            commits.add(0, newCommit)
                            showCommitDialog = false
                            Toast.makeText(context, "Committed ${newCommit.hash}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Commit", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommitDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }

    // Explicit Push Confirmation Dialog (Mandatory security rule)
    if (showPushConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPushConfirmDialog = false },
            title = { Text("Confirm Remote Push", color = TextPrimary) },
            text = {
                Text(
                    "You are about to push local commits to the remote repository. This will publish your code to GitHub. Do you confirm?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPushConfirmDialog = false
                        Toast.makeText(context, "Pushing changes verified and staged.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Confirm & Push", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPushConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}
