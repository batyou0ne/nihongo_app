package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

/**
 * Chat ekranındaki tek bir mesajı temsil eder.
 */
@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor(
        id: UUID,
        text: String,
        isUser: Boolean,
        isError: Boolean = false,
        timestamp: Date = Date()
    ) : this(
        id = id.toString(),
        text = text,
        isUser = isUser,
        isError = isError,
        timestamp = timestamp.time
    )
}
