package com.example.smartlifedashboard.util

/**
 * Utility functions for converting between avatar IDs and indices
 */

fun convertAvatarIdToIndex(avatarId: String?): Int {
    return when (avatarId) {
        "avatar1" -> 0
        "avatar2" -> 1
        "avatar3" -> 2
        "avatar4" -> 3
        "avatar5" -> 4
        else -> 0 // default to first avatar
    }
}

fun convertIndexToAvatarId(index: Int): String {
    return when (index) {
        0 -> "avatar1"
        1 -> "avatar2"
        2 -> "avatar3"
        3 -> "avatar4"
        4 -> "avatar5"
        else -> "avatar1" // default to first avatar
    }
}