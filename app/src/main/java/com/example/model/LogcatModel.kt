package com.example.model

enum class LogLevel(val letter: String) {
    ALL("A"),
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E")
}

data class LogEntry(
    val id: Long,
    val raw: String,
    val level: LogLevel = LogLevel.DEBUG,
    val tag: String = "",
    val message: String = ""
)
