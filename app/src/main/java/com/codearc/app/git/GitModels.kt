package com.codearc.app.git

data class GitStatusSnapshot(
    val added: List<String>,
    val modified: List<String>,
    val removed: List<String>,
    val untracked: List<String>,
    val missing: List<String>
) {
    val isClean get() = added.isEmpty() && modified.isEmpty() && removed.isEmpty() && untracked.isEmpty() && missing.isEmpty()
}

data class CommitInfo(val id: String, val shortId: String, val message: String, val author: String, val time: Long)
