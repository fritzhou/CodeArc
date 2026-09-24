package com.codearc.app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/** Real, local Git — Initialize Repository / Commit / View Changes from the Phase 7 plan,
 *  using JGit (a pure-Java git implementation) instead of shelling out to a native git binary
 *  CodeAssist devices don't have. Also backs the "Clone Repository" creation option. Branches,
 *  push and authenticated pull are intentionally left for later, per the plan ("Branches
 *  later", GitHub push/pull "optional"). */
object GitManager {
    fun isRepo(projectDir: File): Boolean = File(projectDir, ".git").isDirectory

    suspend fun init(projectDir: File) = withContext(Dispatchers.IO) {
        Git.init().setDirectory(projectDir).call().close()
    }

    suspend fun status(projectDir: File): GitStatusSnapshot = withContext(Dispatchers.IO) {
        Git.open(projectDir).use { git ->
            val s = git.status().call()
            GitStatusSnapshot(
                added = s.added.toList(),
                modified = s.modified.toList(),
                removed = s.removed.toList(),
                untracked = s.untracked.filterNot { it.startsWith(".trash/") }.toList(),
                missing = s.missing.toList()
            )
        }
    }

    /** Stages every change (respecting nothing special — CodeArc has no .gitignore support yet)
     *  and commits with the given message and identity. Fails honestly (throws) rather than
     *  creating an empty commit when there's nothing staged. */
    suspend fun commitAll(projectDir: File, message: String, authorName: String, authorEmail: String): CommitInfo = withContext(Dispatchers.IO) {
        require(message.isNotBlank()) { "Commit message can't be empty." }
        Git.open(projectDir).use { git ->
            git.add().addFilepattern(".").call()
            git.add().setUpdate(true).addFilepattern(".").call() // also stage deletions
            val status = git.status().call()
            if (status.added.isEmpty() && status.changed.isEmpty() && status.removed.isEmpty()) error("Nothing to commit — no changes since the last commit.")
            val commit = git.commit().setMessage(message).setAuthor(authorName.ifBlank { "CodeArc User" }, authorEmail.ifBlank { "user@codearc.local" }).call()
            CommitInfo(commit.name, commit.name.take(7), message, authorName.ifBlank { "CodeArc User" }, commit.commitTime.toLong() * 1000)
        }
    }

    suspend fun log(projectDir: File, limit: Int = 20): List<CommitInfo> = withContext(Dispatchers.IO) {
        if (!isRepo(projectDir)) return@withContext emptyList()
        Git.open(projectDir).use { git ->
            try {
                git.log().setMaxCount(limit).call().map { CommitInfo(it.name, it.name.take(7), it.shortMessage, it.authorIdent.name, it.commitTime.toLong() * 1000) }
            } catch (e: GitAPIException) { emptyList() } // no commits yet
        }
    }

    /** Clones [url] into [destDir], which must already exist and be empty (callers should
     *  allocate it via ProjectRepository.allocate() first so a failed clone can be cleanly
     *  discarded). [username]/[password] are for a personal access token over HTTPS — CodeArc
     *  never stores them, they're used for this one call only. */
    suspend fun clone(url: String, destDir: File, username: String? = null, password: String? = null) = withContext(Dispatchers.IO) {
        val cmd = Git.cloneRepository().setURI(url).setDirectory(destDir)
        if (!username.isNullOrBlank()) cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password ?: ""))
        cmd.call().close()
    }
}
