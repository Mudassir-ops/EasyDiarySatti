package com.example.easydiarysatti

import android.util.Log

object AppLogger {

    private const val STACK_TRACE_LEVELS_UP_LINE = 3

    private const val STACK_TRACE_LEVELS_UP_CLASS = 3

    fun createLog(tag: String = "AppLogger", message: String) {
        val stack = Thread.currentThread().stackTrace
        val lineNr = stack[STACK_TRACE_LEVELS_UP_LINE].lineNumber
        val className = stack[STACK_TRACE_LEVELS_UP_CLASS].fileName
        Log.d(tag, "$lineNr $className $message")
    }

    fun createCompleteLog(tag: String = "AppLogger", message: String) {
        val maxLogSize = 1000
        for (i in 0..message.length / maxLogSize) {
            val start = i * maxLogSize
            val end = ((i + 1) * maxLogSize).coerceAtMost(message.length)
            createLog(tag, message.substring(start, end))
        }
    }
}