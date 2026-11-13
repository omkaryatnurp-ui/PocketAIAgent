package com.pocketai.agent

import com.pocketai.agent.data.terminal.SandboxedTerminal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SandboxedTerminalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var workspace: File
    private lateinit var terminal: SandboxedTerminal

    @Before
    fun setup() {
        workspace = tempFolder.newFolder("test_workspace")
        terminal = SandboxedTerminal(workspace)
    }

    @Test
    fun testPwdCommand() = runBlocking {
        val result = terminal.executeCommand("pwd")
        assertEquals(workspace.absolutePath, result.output.trim())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testTouchAndLsCommand() = runBlocking {
        terminal.executeCommand("touch testfile.txt")
        val lsResult = terminal.executeCommand("ls")
        assertTrue(lsResult.output.contains("testfile.txt"))
    }

    @Test
    fun testMkdirCommand() = runBlocking {
        terminal.executeCommand("mkdir src")
        val dir = File(workspace, "src")
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun testPathEscapingBlocked() = runBlocking {
        val result = terminal.executeCommand("cat ../../../etc/passwd")
        assertTrue(result.output.contains("Security Error") || result.output.contains("escapes workspace"))
    }

    @Test
    fun testDestructiveCommandDetection() {
        assertTrue(terminal.isCommandDestructive("rm -rf important_code"))
        assertTrue(terminal.isCommandDestructive("rm -f main.kt"))
        assertFalse(terminal.isCommandDestructive("ls -la"))
        assertFalse(terminal.isCommandDestructive("cat file.txt"))
    }
}
