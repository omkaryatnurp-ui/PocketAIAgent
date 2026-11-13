package com.pocketai.agent

import com.pocketai.agent.data.project.ZipExporter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZipExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testZipExportAndExtract() {
        val srcDir = tempFolder.newFolder("source_project")
        val file1 = File(srcDir, "Main.kt").apply { writeText("fun main() = println(\"Test\")") }
        val subDir = File(srcDir, "sub").apply { mkdirs() }
        val file2 = File(subDir, "helper.py").apply { writeText("def test(): pass") }

        val zipFile = File(tempFolder.root, "exported.zip")
        ZipExporter.exportProjectToZip(srcDir, zipFile)

        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        val destDir = tempFolder.newFolder("extracted_project")
        ZipExporter.extractZipToDirectory(zipFile, destDir)

        val extractedFile1 = File(destDir, "Main.kt")
        val extractedFile2 = File(destDir, "sub/helper.py")

        assertTrue(extractedFile1.exists())
        assertEquals("fun main() = println(\"Test\")", extractedFile1.readText())
        assertTrue(extractedFile2.exists())
        assertEquals("def test(): pass", extractedFile2.readText())
    }
}
