package com.example.smartlifedashboard.model

import java.util.Date

data class Task(
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Date = Date(),
    val dueDate: Date? = null,
    val priority: String = "medium", // low, medium, high
    val description: String = "",
    val category: String = "" // Custom category
)