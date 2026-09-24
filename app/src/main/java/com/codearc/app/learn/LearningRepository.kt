package com.codearc.app.learn

import android.content.Context
import androidx.room.*
import com.codearc.app.data.CodeArcDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lesson_progress", primaryKeys = ["language", "level", "lessonId"])
data class LessonProgressEntity(
    val language: String,
    val level: String,
    val lessonId: String,
    val completed: Boolean,
    val bookmarked: Boolean,
    val updated: Long
)

@Entity(tableName = "quiz_results", primaryKeys = ["language", "level", "lessonId"])
data class QuizResultEntity(
    val language: String,
    val level: String,
    val lessonId: String,
    val correct: Boolean,
    val attempts: Int,
    val updated: Long
)

@Entity(tableName = "practice_progress", primaryKeys = ["language", "projectId"])
data class PracticeProgressEntity(
    val language: String,
    val projectId: String,
    val completed: Boolean,
    val updated: Long
)

@Dao
interface LearningDao {
    @Query("SELECT * FROM lesson_progress WHERE language = :language")
    fun observeLessonProgress(language: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE language = :language AND level = :level AND lessonId = :lessonId")
    suspend fun getLessonProgress(language: String, level: String, lessonId: String): LessonProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLessonProgress(entity: LessonProgressEntity)

    @Query("SELECT * FROM quiz_results WHERE language = :language AND level = :level AND lessonId = :lessonId")
    suspend fun getQuizResult(language: String, level: String, lessonId: String): QuizResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putQuizResult(entity: QuizResultEntity)

    @Query("SELECT * FROM practice_progress WHERE language = :language")
    fun observePracticeProgress(language: String): Flow<List<PracticeProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPracticeProgress(entity: PracticeProgressEntity)

    @Query("SELECT * FROM lesson_progress ORDER BY updated DESC LIMIT 1")
    suspend fun mostRecent(): LessonProgressEntity?
}

/** Wraps LearningDao so the UI never touches Room directly. Progress and quiz results persist
 *  across restarts (Room), matching the Phase 5 deliverable ("Learning progress must survive
 *  app restarts"). Lesson content itself lives in LearnContent.kt, not here — this class only
 *  tracks what the user has done with it. */
class LearningRepository(context: Context) {
    private val dao = CodeArcDatabase.get(context).learning()

    fun observeProgress(languageId: String): Flow<List<LessonProgressEntity>> = dao.observeLessonProgress(languageId)
    fun observePractice(languageId: String): Flow<List<PracticeProgressEntity>> = dao.observePracticeProgress(languageId)

    suspend fun isCompleted(languageId: String, level: String, lessonId: String): Boolean =
        dao.getLessonProgress(languageId, level, lessonId)?.completed ?: false

    suspend fun isBookmarked(languageId: String, level: String, lessonId: String): Boolean =
        dao.getLessonProgress(languageId, level, lessonId)?.bookmarked ?: false

    suspend fun markCompleted(languageId: String, level: String, lessonId: String) {
        val existing = dao.getLessonProgress(languageId, level, lessonId)
        dao.putLessonProgress(LessonProgressEntity(languageId, level, lessonId, true, existing?.bookmarked ?: false, System.currentTimeMillis()))
    }

    suspend fun toggleBookmark(languageId: String, level: String, lessonId: String): Boolean {
        val existing = dao.getLessonProgress(languageId, level, lessonId)
        val next = !(existing?.bookmarked ?: false)
        dao.putLessonProgress(LessonProgressEntity(languageId, level, lessonId, existing?.completed ?: false, next, System.currentTimeMillis()))
        return next
    }

    suspend fun recordQuiz(languageId: String, level: String, lessonId: String, correct: Boolean) {
        val existing = dao.getQuizResult(languageId, level, lessonId)
        dao.putQuizResult(QuizResultEntity(languageId, level, lessonId, correct, (existing?.attempts ?: 0) + 1, System.currentTimeMillis()))
    }

    suspend fun markProjectStarted(languageId: String, projectId: String) {
        dao.putPracticeProgress(PracticeProgressEntity(languageId, projectId, true, System.currentTimeMillis()))
    }

    /** Used by Home/Learn's "Continue Learning" card. Null if nothing has been started yet. */
    suspend fun mostRecent(): LessonProgressEntity? = dao.mostRecent()
}
