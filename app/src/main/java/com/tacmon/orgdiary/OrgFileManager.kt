package com.tacmon.orgdiary

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class OrgFileManager(private val orgFile: File) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd E", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd E HH:mm", Locale.CHINA)
    
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
}
