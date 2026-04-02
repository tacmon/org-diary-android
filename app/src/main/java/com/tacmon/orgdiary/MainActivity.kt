package com.tacmon.orgdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var gitRepo: GitRepository
    private lateinit var orgManager: OrgFileManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repoDir = File(filesDir, "my-diary-repo")
        gitRepo = GitRepository(repoDir)
        orgManager = OrgFileManager(File(repoDir, "main.org"))
        
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var content by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("就绪") }
        var showConfig by remember { mutableStateOf(false) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Org日记") },
                    actions = {
                        TextButton(onClick = { showConfig = !showConfig }) {
                            Text("设置")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showConfig) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("GitHub Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                status = "同步中..."
                                gitRepo.cloneOrPull(
                                    "https://github.com/tacmon/my-diary-repo.git",
                                    token
                                ).onSuccess {
                                    status = "同步成功"
                                    showConfig = false
                                }.onFailure {
                                    status = "同步失败: ${it.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("同步仓库")
                    }
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("条目内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )
                    
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                status = "插入中..."
                                if (orgManager.addEntry(content)) {
                                    gitRepo.commitAndPush(token, "添加条目").onSuccess {
                                        status = "插入成功"
                                        content = ""
                                    }.onFailure {
                                        status = "推送失败: ${it.message}"
                                    }
                                } else {
                                    status = "插入失败"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = content.isNotBlank() && token.isNotBlank()
                    ) {
                        Text("插入条目")
                    }
                }
                
                Text("状态: $status")
            }
        }
    }
}
