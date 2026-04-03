package com.tacmon.orgdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
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
        var entryType by remember { mutableStateOf("日记") }
        var scheduledDate by remember { mutableStateOf("") }
        var deadlineDate by remember { mutableStateOf("") }
        var currentTab by remember { mutableStateOf(0) }
        
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
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("✏️") },
                        label = { Text("添加") },
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Text("📖") },
                        label = { Text("日记") },
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Text("📅") },
                        label = { Text("待办") },
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 }
                    )
                }
            }
        ) { padding ->
            when (currentTab) {
                0 -> AddEntryScreen(padding, showConfig, token, { token = it }, content, { content = it }, 
                    entryType, { entryType = it }, scheduledDate, { scheduledDate = it }, 
                    deadlineDate, { deadlineDate = it }, status, { status = it })
                1 -> DiaryScreen(padding)
                2 -> AgendaScreen(padding)
            }
        }
    }
    
    @Composable
    fun AddEntryScreen(
        padding: PaddingValues, showConfig: Boolean, token: String, onTokenChange: (String) -> Unit,
        content: String, onContentChange: (String) -> Unit, entryType: String, onEntryTypeChange: (String) -> Unit,
        scheduledDate: String, onScheduledChange: (String) -> Unit, deadlineDate: String, onDeadlineChange: (String) -> Unit,
        status: String, onStatusChange: (String) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showConfig) {
                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    label = { Text("GitHub Token") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            onStatusChange("同步中...")
                            gitRepo.cloneOrPull("https://github.com/tacmon/my-diary-repo.git", token)
                                .onSuccess { onStatusChange("同步成功") }
                                .onFailure { onStatusChange("同步失败: ${it.message}") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("同步仓库") }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEntryTypeChange("日记") },
                        colors = if (entryType == "日记") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) { Text("日记") }
                    Button(
                        onClick = { onEntryTypeChange("TODO") },
                        colors = if (entryType == "TODO") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) { Text("TODO") }
                }
                
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("条目内容") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
                
                if (entryType == "TODO") {
                    OutlinedTextField(
                        value = scheduledDate,
                        onValueChange = onScheduledChange,
                        label = { Text("计划时间 (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deadlineDate,
                        onValueChange = onDeadlineChange,
                        label = { Text("截止时间 (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            onStatusChange("插入中...")
                            val success = if (entryType == "TODO") {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                                val scheduled = scheduledDate.takeIf { it.isNotBlank() }?.let { try { dateFormat.parse(it) } catch (e: Exception) { null } }
                                val deadline = deadlineDate.takeIf { it.isNotBlank() }?.let { try { dateFormat.parse(it) } catch (e: Exception) { null } }
                                orgManager.addTodoEntry(content, scheduled, deadline)
                            } else {
                                orgManager.addEntry(content)
                            }
                            
                            if (success) {
                                gitRepo.commitAndPush(token, "添加${entryType}条目").onSuccess {
                                    onStatusChange("插入成功")
                                    onContentChange("")
                                    onScheduledChange("")
                                    onDeadlineChange("")
                                }.onFailure {
                                    onStatusChange("推送失败: ${it.message}")
                                }
                            } else {
                                onStatusChange("插入失败")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = content.isNotBlank() && token.isNotBlank()
                ) { Text("插入条目") }
            }
            Text("状态: $status")
        }
    }
    
    @Composable
    fun DiaryScreen(padding: PaddingValues) {
        var entries by remember { mutableStateOf<List<OrgFileManager.DiaryEntry>>(emptyList()) }
        
        LaunchedEffect(Unit) {
            entries = orgManager.getRecentEntries(50)
        }
        
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("最近日记", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { entries = orgManager.getRecentEntries(50) }) {
                    Text("刷新")
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(entry.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            if (entry.isTodo) {
                                Text("TODO", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                            Text(entry.content, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun AgendaScreen(padding: PaddingValues) {
        var todos by remember { mutableStateOf<List<OrgFileManager.TodoItem>>(emptyList()) }
        
        LaunchedEffect(Unit) {
            todos = orgManager.getTodoItems()
        }
        
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("待办事项", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { todos = orgManager.getTodoItems() }) {
                    Text("刷新")
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(todos) { todo ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(todo.content, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(todo.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(top = 4.dp))
                            todo.scheduled?.let {
                                Text("📅 计划: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                            }
                            todo.deadline?.let {
                                Text("⏰ 截止: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
