package com.pocketai.agent.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.terminal.CommandResult
import com.pocketai.agent.data.terminal.SandboxedTerminal
import com.pocketai.agent.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TerminalScreen(
    projectName: String,
    workspacePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val terminal = remember(workspacePath) { SandboxedTerminal(File(workspacePath)) }

    val terminalLogs = remember {
        mutableStateListOf(
            CommandResult(
                command = "pocket-init",
                output = "Pocket AI Sandboxed Execution Shell\nWorkspace: $workspacePath\nType 'help' for available commands.",
                exitCode = 0,
                executionTimeMs = 0
            )
        )
    }

    var inputCommand by remember { mutableStateOf("") }
    var pendingDangerousCommand by remember { mutableStateOf<String?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        if (terminal.isCommandDestructive(cmd)) {
            pendingDangerousCommand = cmd
            return
        }

        coroutineScope.launch {
            isExecuting = true
            val res = terminal.executeCommand(cmd)
            terminalLogs.add(res)
            isExecuting = false
            inputCommand = ""
            listState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Scaffold(
        containerColor = CharcoalBlack,
        topBar = {
            Surface(
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Terminal: $projectName",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sandboxed local shell",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangeAccent
                        )
                    }

                    IconButton(onClick = {
                        val allOutput = terminalLogs.joinToString("\n\n") { "$ ${it.command}\n${it.output}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Log", allOutput))
                        Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary)
                    }

                    IconButton(onClick = { terminalLogs.clear() }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Quick command shortcuts row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ls -la", "pwd", "find", "help").forEach { quickCmd ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CardDark,
                                modifier = Modifier.clickable { runCommand(quickCmd) }
                            ) {
                                Text(
                                    text = quickCmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = OrangeAccentLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Command input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$ ",
                            fontFamily = FontFamily.Monospace,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = inputCommand,
                            onValueChange = { inputCommand = it },
                            placeholder = { Text("Enter command...", fontSize = 12.sp, color = TextMuted) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = CharcoalDark,
                                unfocusedContainerColor = CharcoalDark
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { runCommand(inputCommand) },
                            enabled = inputCommand.isNotBlank() && !isExecuting,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = OrangeAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Run",
                                tint = CharcoalBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(terminalLogs) { item ->
                Column {
                    if (item.command.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "pocket@device:~$ ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = StatusGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (item.output.isNotBlank()) {
                        Text(
                            text = item.output,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }

            if (isExecuting) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = OrangeAccent,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running command...", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }
    }

    // Dangerous Command Confirmation Dialog
    pendingDangerousCommand?.let { cmd ->
        AlertDialog(
            onDismissRequest = { pendingDangerousCommand = null },
            title = { Text("Destructive Operation Warning", color = StatusRed) },
            text = {
                Text(
                    "You are attempting to execute a potentially destructive command:\n\n`$cmd`\n\nAre you sure you want to proceed?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val confirmed = cmd
                        pendingDangerousCommand = null
                        coroutineScope.launch {
                            isExecuting = true
                            val res = terminal.executeCommand(confirmed)
                            terminalLogs.add(res)
                            isExecuting = false
                            inputCommand = ""
                            listState.animateScrollToItem(terminalLogs.size - 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Authorize & Run", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDangerousCommand = null }) {
                    Text("Abort", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}
