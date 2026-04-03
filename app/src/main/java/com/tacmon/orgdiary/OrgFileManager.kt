package com.tacmon.orgdiary

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class OrgFileManager(private val orgFile: File) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd E", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd E HH:mm", Locale.CHINA)
    private val orgDateFormat = SimpleDateFormat("yyyy-MM-dd E", Locale.CHINA)
    
    fun addEntry(content: String, timestamp: Date = Date()): Boolean {
        if (!orgFile.exists()) return false
        
        val dateStr = dateFormat.format(timestamp)
        val timeStr = timeFormat.format(timestamp)
        
        val lines = orgFile.readLines().toMutableList()
        var insertIndex = -1
        
        // 查找日期标题
        for (i in lines.indices) {
            if (lines[i].startsWith("* <$dateStr>")) {
                insertIndex = i + 1
                break
            }
        }
        
        // 如果没找到日期，创建新日期标题
        if (insertIndex == -1) {
            // 找到合适的插入位置（按日期倒序）
            for (i in lines.indices) {
                if (lines[i].startsWith("* <")) {
                    insertIndex = i
                    break
                }
            }
            if (insertIndex == -1) insertIndex = lines.size
            
            lines.add(insertIndex, "* <$dateStr>")
            insertIndex++
        }
        
        // 插入条目
        lines.add(insertIndex, "**** <$timeStr>")
        lines.add(insertIndex + 1, content)
        lines.add(insertIndex + 2, "")
        
        orgFile.writeText(lines.joinToString("\n"))
        return true
    }
    
    fun addTodoEntry(content: String, scheduledTime: Date?, deadlineTime: Date?, timestamp: Date = Date()): Boolean {
        if (!orgFile.exists()) return false
        
        val dateStr = dateFormat.format(timestamp)
        val timeStr = timeFormat.format(timestamp)
        
        val lines = orgFile.readLines().toMutableList()
        var insertIndex = -1
        
        for (i in lines.indices) {
            if (lines[i].startsWith("* <$dateStr>")) {
                insertIndex = i + 1
                break
            }
        }
        
        if (insertIndex == -1) {
            for (i in lines.indices) {
                if (lines[i].startsWith("* <")) {
                    insertIndex = i
                    break
                }
            }
            if (insertIndex == -1) insertIndex = lines.size
            
            lines.add(insertIndex, "* <$dateStr>")
            insertIndex++
        }
        
        lines.add(insertIndex, "**** TODO <$timeStr>")
        
        val scheduleParts = mutableListOf<String>()
        scheduledTime?.let { scheduleParts.add("SCHEDULED: <${orgDateFormat.format(it)}>") }
        deadlineTime?.let { scheduleParts.add("DEADLINE: <${orgDateFormat.format(it)}>") }
        
        if (scheduleParts.isNotEmpty()) {
            lines.add(insertIndex + 1, scheduleParts.joinToString(" "))
            lines.add(insertIndex + 2, content)
            lines.add(insertIndex + 3, "")
        } else {
            lines.add(insertIndex + 1, content)
            lines.add(insertIndex + 2, "")
        }
        
        orgFile.writeText(lines.joinToString("\n"))
        return true
    }
    
    data class DiaryEntry(val date: String, val time: String, val content: String, val isTodo: Boolean = false)
    data class TodoItem(val date: String, val time: String, val content: String, val scheduled: String?, val deadline: String?)
    
    fun getRecentEntries(limit: Int = 20): List<DiaryEntry> {
        if (!orgFile.exists()) return emptyList()
        
        val entries = mutableListOf<DiaryEntry>()
        val lines = orgFile.readLines()
        var currentDate = ""
        var i = 0
        
        while (i < lines.size && entries.size < limit) {
            val line = lines[i]
            if (line.startsWith("* <") && line.endsWith(">")) {
                currentDate = line.substring(3, line.length - 1)
            } else if (line.startsWith("**** ")) {
                val isTodo = line.contains("TODO")
                val timeMatch = Regex("<(.+?)>").find(line)
                val time = timeMatch?.groupValues?.get(1) ?: ""
                val content = if (i + 1 < lines.size) lines[i + 1].trim() else ""
                if (content.isNotEmpty()) {
                    entries.add(DiaryEntry(currentDate, time, content, isTodo))
                }
            }
            i++
        }
        return entries
    }
    
    fun getTodoItems(): List<TodoItem> {
        if (!orgFile.exists()) return emptyList()
        
        val todos = mutableListOf<TodoItem>()
        val lines = orgFile.readLines()
        var currentDate = ""
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("* <") && line.endsWith(">")) {
                currentDate = line.substring(3, line.length - 1)
            } else if (line.startsWith("**** TODO")) {
                val timeMatch = Regex("<(.+?)>").find(line)
                val time = timeMatch?.groupValues?.get(1) ?: ""
                
                var scheduled: String? = null
                var deadline: String? = null
                var content = ""
                
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    if (nextLine.contains("SCHEDULED:") || nextLine.contains("DEADLINE:")) {
                        val schedMatch = Regex("SCHEDULED: <(.+?)>").find(nextLine)
                        val deadMatch = Regex("DEADLINE: <(.+?)>").find(nextLine)
                        scheduled = schedMatch?.groupValues?.get(1)
                        deadline = deadMatch?.groupValues?.get(1)
                        if (i + 2 < lines.size) content = lines[i + 2].trim()
                    } else {
                        content = nextLine.trim()
                    }
                }
                
                if (content.isNotEmpty()) {
                    todos.add(TodoItem(currentDate, time, content, scheduled, deadline))
                }
            }
            i++
        }
        return todos
    }
}
