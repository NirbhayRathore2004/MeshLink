package com.meshlink.data.model

enum class MessageStatus {
    PENDING,        // Waiting in local queue / sending
    SENT_TO_PEER,   // Successfully transmitted to direct peer(s)
    FORWARDED,      // Intermediate node forwarded the packet
    DELIVERED,      // Target node received and returned ACK
    FAILED          // Max retries exceeded or TTL expired
}
