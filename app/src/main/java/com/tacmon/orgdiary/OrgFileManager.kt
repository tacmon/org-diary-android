package com.tacmon.orgdiary

import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
                // 找到该日期下的最后一个条目位置
                insertIndex = i + 1
                for (j in i + 1 until lines.size) {
                    if (lines[j].startsWith("* <")) {
                        // 遇到下一个日期标题，在它之前插入
                        insertIndex = j
                        break
                    }
                    if (j == lines.size - 1) {
                        // 已到文件末尾
                        insertIndex = lines.size
                    }
                }
                break
            }
        }
        
        // 如果没找到日期，创建新日期标题（追加到文件末尾）
        if (insertIndex == -1) {
            insertIndex = lines.size
            if (insertIndex > 0 && lines[insertIndex - 1].isNotBlank()) {
                lines.add("")
            }
            lines.add("* <$dateStr>")
            insertIndex = lines.size
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
        
        // 查找日期标题
        for (i in lines.indices) {
            if (lines[i].startsWith("* <$dateStr>")) {
                // 找到该日期下的最后一个条目位置
                insertIndex = i + 1
                for (j in i + 1 until lines.size) {
                    if (lines[j].startsWith("* <")) {
                        // 遇到下一个日期标题，在它之前插入
                        insertIndex = j
                        break
                    }
                    if (j == lines.size - 1) {
                        // 已到文件末尾
                        insertIndex = lines.size
                    }
                }
                break
            }
        }
        
        // 如果没找到日期，创建新日期标题（追加到文件末尾）
        if (insertIndex == -1) {
            insertIndex = lines.size
            if (insertIndex > 0 && lines[insertIndex - 1].isNotBlank()) {
                lines.add("")
            }
            lines.add("* <$dateStr>")
            insertIndex = lines.size
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
    data class TodoItem(
        val date: String,
        val time: String,
        val content: String,
        val status: String,
        val scheduled: String?,
        val deadline: String?,
        val sortKey: Long,
        val urgencyText: String
    )

    private data class ParsedEntry(
        val date: String,
        val time: String,
        val content: String,
        val status: String?,
        val scheduled: String?,
        val deadline: String?
    )

    private fun parseEntries(): List<ParsedEntry> {
        if (!orgFile.exists()) return emptyList()

        val entries = mutableListOf<ParsedEntry>()
        val lines = orgFile.readLines()
        var currentDate = ""
        var i = 0
        val headingRegex = Regex("^(\\*+)\\s+(?:(TODO|IN-PROGRESS)\\s+)?(.*)$")

        while (i < lines.size) {
            val line = lines[i]

            if (line.startsWith("* <") && line.endsWith(">")) {
                currentDate = line.substring(3, line.length - 1)
                i++
                continue
            }

            val headingMatch = headingRegex.find(line)
            if (headingMatch != null) {
                val status = headingMatch.groupValues[2].ifBlank { null }
                val headingText = headingMatch.groupValues[3].trim()
                val timeMatch = Regex("<(.+?)>").find(line)
                val time = timeMatch?.groupValues?.get(1) ?: ""

                var scheduled: String? = null
                var deadline: String? = null
                val contentLines = mutableListOf<String>()
                val titleWithoutTimestamp = headingText
                    .replace(Regex("<[^>]+>"), "")
                    .trim()
                var j = i + 1

                while (j < lines.size) {
                    val bodyLine = lines[j]
                    if (bodyLine.startsWith("*")) break

                    if (bodyLine.contains("SCHEDULED:") || bodyLine.contains("DEADLINE:")) {
                        val schedMatch = Regex("SCHEDULED: <(.+?)>").find(bodyLine)
                        val deadMatch = Regex("DEADLINE: <(.+?)>").find(bodyLine)
                        scheduled = schedMatch?.groupValues?.get(1) ?: scheduled
                        deadline = deadMatch?.groupValues?.get(1) ?: deadline
                    } else if (bodyLine.isNotBlank()) {
                        contentLines.add(bodyLine.trim())
                    }
                    j++
                }

                val bodyContent = contentLines.joinToString("\n").trim()
                val content = when {
                    titleWithoutTimestamp.isNotBlank() && bodyContent.isNotBlank() -> "$titleWithoutTimestamp\n$bodyContent"
                    titleWithoutTimestamp.isNotBlank() -> titleWithoutTimestamp
                    else -> bodyContent
                }.trim()

                if (content.isNotBlank() || status != null) {
                    entries.add(ParsedEntry(currentDate, time, content, status, scheduled, deadline))
                }

                i = j
                continue
            }

            i++
        }

        return entries
    }

    private fun parseOrgDate(date: String?): LocalDate? {
        if (date.isNullOrBlank()) return null
        return try {
            orgDateFormat.parse(date)?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        } catch (e: Exception) {
            null
        }
    }

    private fun buildUrgencyText(deadline: String?, scheduled: String?): Pair<Long, String> {
        val today = LocalDate.now()
        val deadlineDate = parseOrgDate(deadline)
        val scheduledDate = parseOrgDate(scheduled)

        deadlineDate?.let {
            val diff = ChronoUnit.DAYS.between(today, it)
            val text = when {
                diff < 0 -> "已逾期 ${-diff} 天"
                diff == 0L -> "今天截止"
                diff == 1L -> "明天截止"
                else -> "$diff 天后截止"
            }
            return diff to text
        }

        scheduledDate?.let {
            val diff = ChronoUnit.DAYS.between(today, it)
            val text = when {
                diff < 0 -> "计划时间已过 ${-diff} 天"
                diff == 0L -> "今天计划执行"
                diff == 1L -> "明天计划执行"
                else -> "$diff 天后计划执行"
            }
            return (10_000L + diff) to text
        }

        return Long.MAX_VALUE to "无截止时间"
    }
    
    fun getRecentEntries(limit: Int = Int.MAX_VALUE): List<DiaryEntry> {
        return parseEntries()
            .map {
                DiaryEntry(
                    date = it.date,
                    time = it.time,
                    content = it.content,
                    isTodo = it.status == "TODO" || it.status == "IN-PROGRESS"
                )
            }
            .reversed()
            .take(limit)
    }
    
    fun getTodoItems(): List<TodoItem> {
        return parseEntries()
            .asSequence()
            .filter { it.status == "TODO" || it.status == "IN-PROGRESS" }
            .map {
                val urgency = buildUrgencyText(it.deadline, it.scheduled)
                TodoItem(
                    date = it.date,
                    time = it.time,
                    content = it.content,
                    status = it.status ?: "TODO",
                    scheduled = it.scheduled,
                    deadline = it.deadline,
                    sortKey = urgency.first,
                    urgencyText = urgency.second
                )
            }
            .sortedWith(
                compareBy<TodoItem> { it.sortKey }
                    .thenBy { parseOrgDate(it.deadline) ?: LocalDate.MAX }
                    .thenBy { parseOrgDate(it.scheduled) ?: LocalDate.MAX }
                    .thenByDescending { it.time }
            )
            .toList()
    }
}
