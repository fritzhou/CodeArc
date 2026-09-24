package com.codearc.app.data
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codearc.app.projects.*
import com.codearc.app.learn.LearningDao
import com.codearc.app.learn.LessonProgressEntity
import com.codearc.app.learn.QuizResultEntity
import com.codearc.app.learn.PracticeProgressEntity
@Entity(tableName = "app_metadata")
data class AppMetadata(@PrimaryKey val key: String, val value: String)
@Dao interface MetadataDao {
 @Query("SELECT * FROM app_metadata WHERE `key` = :key") suspend fun get(key: String): AppMetadata?
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(metadata: AppMetadata)
}
@Database(
    entities = [AppMetadata::class, Project::class, LessonProgressEntity::class, QuizResultEntity::class, PracticeProgressEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CodeArcDatabase : RoomDatabase() {
 abstract fun projects(): ProjectDao
 abstract fun metadata(): MetadataDao
 abstract fun learning(): LearningDao
 companion object {
 val MIGRATION_1_2 = object : Migration(1, 2) {
 override fun migrate(db: SupportSQLiteDatabase) {
 db.execSQL("CREATE TABLE IF NOT EXISTS projects (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, language TEXT NOT NULL, path TEXT NOT NULL, created INTEGER NOT NULL, modified INTEGER NOT NULL, favorite INTEGER NOT NULL, executionMode TEXT NOT NULL, mainFile TEXT NOT NULL, runtimePreference TEXT NOT NULL)")
 }
 }
 val MIGRATION_2_3 = object : Migration(2, 3) {
 override fun migrate(db: SupportSQLiteDatabase) {
 db.execSQL("CREATE TABLE IF NOT EXISTS lesson_progress (language TEXT NOT NULL, level TEXT NOT NULL, lessonId TEXT NOT NULL, completed INTEGER NOT NULL, bookmarked INTEGER NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(language, level, lessonId))")
 db.execSQL("CREATE TABLE IF NOT EXISTS quiz_results (language TEXT NOT NULL, level TEXT NOT NULL, lessonId TEXT NOT NULL, correct INTEGER NOT NULL, attempts INTEGER NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(language, level, lessonId))")
 db.execSQL("CREATE TABLE IF NOT EXISTS practice_progress (language TEXT NOT NULL, projectId TEXT NOT NULL, completed INTEGER NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(language, projectId))")
 }
 }

 @Volatile private var instance: CodeArcDatabase? = null
 fun get(context: Context): CodeArcDatabase = instance ?: synchronized(this) {
 instance ?: Room.databaseBuilder(context.applicationContext, CodeArcDatabase::class.java, "codearc.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
 }
 }
}
