package com.pocketai.agent.data.local.dao

import androidx.room.*
import com.pocketai.agent.data.local.entities.TaskCheckpointEntity
import com.pocketai.agent.data.local.entities.TaskLogEntity
import com.pocketai.agent.data.local.entities.TaskRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskRecordEntity>>

    @Query("SELECT * FROM tasks WHERE status IN ('QUEUED', 'STARTING', 'RUNNING') ORDER BY priority DESC, createdAt ASC")
    fun getActiveAndQueuedTasks(): Flow<List<TaskRecordEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getTasksByStatus(status: String): Flow<List<TaskRecordEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskRecordEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskById(id: String): Flow<TaskRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskRecordEntity)

    @Update
    suspend fun updateTask(task: TaskRecordEntity)

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET progressPercent = :progress, currentStage = :stage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskProgress(id: String, progress: Int, stage: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTask(task: TaskRecordEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearCompletedTasks()

    // Checkpoints
    @Query("SELECT * FROM task_checkpoints WHERE taskId = :taskId ORDER BY checkpointIndex DESC")
    fun getCheckpointsForTask(taskId: String): Flow<List<TaskCheckpointEntity>>

    @Query("SELECT * FROM task_checkpoints WHERE taskId = :taskId ORDER BY checkpointIndex DESC LIMIT 1")
    suspend fun getLatestCheckpoint(taskId: String): TaskCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: TaskCheckpointEntity)

    // Logs
    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getLogsForTask(taskId: String): Flow<List<TaskLogEntity>>

    @Insert
    suspend fun insertLog(log: TaskLogEntity)

    @Insert
    suspend fun insertLogs(logs: List<TaskLogEntity>)

    @Query("DELETE FROM task_logs WHERE taskId = :taskId")
    suspend fun deleteLogsForTask(taskId: String)
}
