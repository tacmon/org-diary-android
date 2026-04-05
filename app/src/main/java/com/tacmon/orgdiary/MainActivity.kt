package com.tacmon.orgdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var gitRepo: GitRepository
    private lateinit var orgManager: OrgFileManager
    private val remoteRepoUrl = "https://gitee.com/tacmon/my-diary"

    private suspend fun syncBeforeEdit(token: String): Result<Unit> {
        return gitRepo.cloneOrPull(remoteRepoUrl, token)
    }
    
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showConfig) {
                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    label = { Text("Gitee 私人令牌") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "同步仓库：$remoteRepoUrl",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            onStatusChange("同步中...")
                            gitRepo.cloneOrPull(remoteRepoUrl, token)
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
                    var showScheduledPicker by remember { mutableStateOf(false) }
                    var showDeadlinePicker by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showScheduledPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (scheduledDate.isBlank()) "📅 选择计划时间（可选）" else "📅 计划: $scheduledDate")
                    }
                    
                    if (scheduledDate.isNotBlank()) {
                        TextButton(onClick = { onScheduledChange("") }) {
                            Text("清除计划时间")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { showDeadlinePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (deadlineDate.isBlank()) "⏰ 选择截止时间（可选）" else "⏰ 截止: $deadlineDate")
                    }
                    
                    if (deadlineDate.isNotBlank()) {
                        TextButton(onClick = { onDeadlineChange("") }) {
                            Text("清除截止时间")
                        }
                    }
                    
                    if (showScheduledPicker) {
                        DatePickerDialog(
                            onDateSelected = { date ->
                                onScheduledChange(date)
                                showScheduledPicker = false
                            },
                            onDismiss = { showScheduledPicker = false }
                        )
                    }
                    
                    if (showDeadlinePicker) {
                        DatePickerDialog(
                            onDateSelected = { date ->
                                onDeadlineChange(date)
                                showDeadlinePicker = false
                            },
                            onDismiss = { showDeadlinePicker = false }
                        )
                    }
                }
                
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            onStatusChange("插入前同步中...")
                            syncBeforeEdit(token)
                                .onFailure {
                                    onStatusChange("插入前同步失败: ${it.message}")
                                    return@launch
                                }

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
            entries = orgManager.getRecentEntries()
        }
        
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("全部日记", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { entries = orgManager.getRecentEntries() }) {
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
                Column {
                    Text("Agenda", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "按截止时间从紧张到松弛排序，共 ${todos.size} 项",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { todos = orgManager.getTodoItems() }) {
                    Text("刷新")
                }
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (todos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                "暂未发现 TODO / IN-PROGRESS 条目，请先同步仓库或添加任务。",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(todos) { todo ->
                    val cardColor = if (todo.deadline != null) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                    val urgencyColor = when {
                        todo.deadline != null && todo.sortKey < 0 -> MaterialTheme.colorScheme.error
                        todo.deadline != null && todo.sortKey == 0L -> MaterialTheme.colorScheme.error
                        todo.deadline != null && todo.sortKey <= 3 -> Color(0xFFB26A00)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (todo.status == "IN-PROGRESS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        todo.status,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    todo.urgencyText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = urgencyColor
                                )
                            }

                            Text(
                                todo.content,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "创建于 ${todo.time}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            todo.scheduled?.let {
                                Text("📅 计划: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            todo.deadline?.let {
                                Text("⏰ 截止: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                            Text(
                                "归档日期：${todo.date}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DatePickerDialog(
        onDateSelected: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                        onDateSelected(dateFormat.format(calendar.time))
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
