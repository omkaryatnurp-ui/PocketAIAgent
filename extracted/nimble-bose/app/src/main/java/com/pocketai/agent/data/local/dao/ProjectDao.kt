package com.pocketai.agent.data.local.dao

import androidx.room.*
import com.pocketai.agent.data.local.entities.ProjectEntity
import com.pocketai.agent.data.local.entities.ProjectFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    // Project Files
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY isDirectory DESC, relativePath ASC")
    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND relativePath = :relativePath")
    suspend fun getFileByPath(projectId: String, relativePath: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Query("DELETE FROM project_files WHERE projectId = :projectId AND relativePath = :relativePath")
    suspend fun deleteFile(projectId: String, relativePath: String)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteAllFilesForProject(projectId: String)
}
