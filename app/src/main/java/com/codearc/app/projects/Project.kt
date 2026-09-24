package com.codearc.app.projects

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class Project(
 @PrimaryKey val id: String,
 val name: String,
 val language: String,
 val path: String,
 val created: Long,
 val modified: Long,
 val favorite: Boolean = false,
 val executionMode: String = "Automatic",
 val mainFile: String,
 val runtimePreference: String = "Default"
)
@Dao interface ProjectDao {
 @Query("SELECT * FROM projects ORDER BY modified DESC") fun observe(): Flow<List<Project>>
 @Query("SELECT * FROM projects WHERE id = :id") suspend fun get(id: String): Project?
 @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(project: Project)
 @Update suspend fun update(project: Project)
 @Query("DELETE FROM projects WHERE id = :id") suspend fun delete(id: String)
}
