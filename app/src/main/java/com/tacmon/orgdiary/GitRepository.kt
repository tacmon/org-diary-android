package com.tacmon.orgdiary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitRepository(private val repoDir: File) {
    private val giteeUsername = "tacmon"
    private val defaultRepoUrl = "https://gitee.com/tacmon/my-diary"

    private fun ensureOriginRemote(config: StoredConfig, repoUrl: String) {
        config.setString("remote", "origin", "url", repoUrl)
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
        config.save()
    }

    private fun credentialsProvider(token: String): UsernamePasswordCredentialsProvider {
        return UsernamePasswordCredentialsProvider(giteeUsername, token)
    }

    private fun prepareRepository(git: Git, repoUrl: String) {
        ensureOriginRemote(git.repository.config, repoUrl)

        val repository = git.repository
        val branch = repository.branch
        val localBranchRef = repository.findRef("refs/heads/$branch")
        val remoteBranchRef = repository.findRef("refs/remotes/origin/$branch")

        if (localBranchRef == null && remoteBranchRef != null) {
            git.checkout()
                .setCreateBranch(true)
                .setName(branch)
                .setStartPoint("origin/$branch")
                .setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.TRACK)
                .call()
        }
    }

    private fun pullLatest(git: Git, repoUrl: String, token: String) {
        prepareRepository(git, repoUrl)
        git.fetch()
            .setRemote("origin")
            .setCredentialsProvider(credentialsProvider(token))
            .call()
        git.reset()
            .setMode(ResetCommand.ResetType.HARD)
            .setRef("origin/${git.repository.branch}")
            .call()
        git.clean()
            .setCleanDirectories(true)
            .call()
    }
    
    suspend fun cloneOrPull(repoUrl: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = if (repoDir.exists() && File(repoDir, ".git").exists()) {
                Git.open(repoDir).apply {
                    pullLatest(this, repoUrl, token)
                }
            } else {
                repoDir.mkdirs()
                Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .setCredentialsProvider(credentialsProvider(token))
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
            prepareRepository(git, defaultRepoUrl)
            git.add().addFilepattern("main.org").call()
            if (!git.status().call().isClean) {
                git.commit().setMessage(message).call()
            }
            git.push()
                .setRemote("origin")
                .setCredentialsProvider(credentialsProvider(token))
                .call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
