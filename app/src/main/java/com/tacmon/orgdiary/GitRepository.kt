package com.tacmon.orgdiary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitRepository(private val repoDir: File) {
    
    suspend fun cloneOrPull(repoUrl: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = if (repoDir.exists() && File(repoDir, ".git").exists()) {
                Git.open(repoDir).apply {
                    pull()
                        .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                        .call()
                }
            } else {
                repoDir.mkdirs()
                Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                    .call()
            }
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun commitAndPush(token: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = Git.open(repoDir)
            git.add().addFilepattern("main.org").call()
            git.commit().setMessage(message).call()
            git.push()
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                .call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
