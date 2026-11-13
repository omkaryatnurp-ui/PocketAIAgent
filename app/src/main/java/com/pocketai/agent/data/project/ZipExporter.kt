package com.pocketai.agent.data.project

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipExporter {

    fun exportProjectToZip(projectDir: File, outputZip: File) {
        if (!projectDir.exists()) return
        if (outputZip.exists()) outputZip.delete()
        outputZip.parentFile?.mkdirs()

        ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->
            projectDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(projectDir).path
                    val entry = ZipEntry(relativePath)
                    zipOut.putNextEntry(entry)
                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }

    fun extractZipToDirectory(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                // Guard against Zip Slip vulnerability
                if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip entry attempted directory traversal: ${entry.name}")
                }
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
}
