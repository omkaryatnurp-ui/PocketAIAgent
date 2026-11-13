package com.pocketai.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.git.GitManager
import com.pocketai.agent.data.security.SecureKeyStorage
import com.pocketai.agent.ui.theme.*

@Composable
fun CodeEditorScreen(
    filePath: String,
    initialContent: String,
    onNavigateBack: () -> Unit,
    onSaveContent: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var originalContent by remember { mutableStateOf(initialContent) }
    var isDirty by remember { mutableStateOf(false) }
    var showDiffView by remember { mutableStateOf(false) }
    var showSearchRow by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val lines = content.lines()

    Scaffold(
        containerColor = CharcoalDark,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = filePath.substringAfterLast('/'),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (isDirty) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "• Edited", color = OrangeAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(
                            text = filePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = { showSearchRow = !showSearchRow }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                    }

                    IconButton(onClick = { showDiffView = !showDiffView }) {
                        Icon(
                            imageVector = Icons.Default.Difference,
                            contentDescription = "Diff",
                            tint = if (showDiffView) OrangeAccent else TextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            onSaveContent(content)
                            originalContent = content
                            isDirty = false
                        },
                        enabled = isDirty,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", color = CharcoalBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Optional Search & Replace Bar
            if (showSearchRow) {
                Surface(
                    color = CardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary)
                        )
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            placeholder = { Text("Replace...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 12.sp, color = TextPrimary)
                        )
                        Button(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    content = content.replace(searchQuery, replaceQuery)
                                    isDirty = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("All", color = CharcoalBlack, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (showDiffView) {
                // Unified Diff View
                val diffText = remember(content, originalContent) {
                    val dummyStorage = SecureKeyStorage(com.pocketai.agent.PocketAiApp())
                    GitManager(dummyStorage).generateDiff(originalContent, content)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CodeBackground)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = diffText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            } else {
                // Real Editable Text with Line Numbers
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CodeBackground)
                        .verticalScroll(verticalScroll)
                ) {
                    // Line numbers gutter
                    Column(
                        modifier = Modifier
                            .background(SurfaceDark)
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lines.size) {
                            Text(
                                text = "$i",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = TextMuted,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Editable Code View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = content,
                            onValueChange = {
                                content = it
                                isDirty = (it != originalContent)
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(OrangeAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
