package com.example.smartlifedashboard.model

import java.util.Date

data class EventCountdown(
    val id: String = "",
    val title: String = "",
    val eventDate: Date = Date(),
    val createdAt: Date = Date(),
    val description: String = ""
)