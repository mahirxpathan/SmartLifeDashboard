package com.example.smartlifedashboard.repository

import com.example.smartlifedashboard.model.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.ListenerRegistration

class HabitRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addHabit(habit: Habit): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val habitData = mapOf(
            "title" to habit.title,
            "frequency" to habit.frequency,
            "streak" to habit.streak,
            "completedToday" to habit.completedToday,
            "createdAt" to habit.createdAt,
            "history" to habit.history,
            "icon" to habit.icon,
            "color" to habit.color
        )
        val result = db.collection("users").document(userId).collection("habits")
            .add(habitData).await()
        return result.id
    }

    suspend fun updateHabit(habitId: String, habit: Habit) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val habitData = mapOf(
            "title" to habit.title,
            "frequency" to habit.frequency,
            "streak" to habit.streak,
            "completedToday" to habit.completedToday,
            "history" to habit.history,
            "icon" to habit.icon,
            "color" to habit.color
        )
        db.collection("users").document(userId).collection("habits")
            .document(habitId)
            .update(habitData)
            .await()
    }

    suspend fun deleteHabit(habitId: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        db.collection("users").document(userId).collection("habits")
            .document(habitId)
            .delete()
            .await()
    }

    suspend fun getHabits(): List<Habit> {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val snapshot = db.collection("users").document(userId).collection("habits")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { document ->
            val id = document.id
            val title = document.getString("title") ?: return@mapNotNull null
            val frequency = document.getString("frequency") ?: return@mapNotNull null
            val streak = document.getLong("streak")?.toInt() ?: 0
            val history = document.get("history") as? Map<String, Boolean> ?: mapOf()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val completedToday = history[today] == true
            val createdAt = document.getDate("createdAt")
            val icon = document.getString("icon") ?: ""
            val color = document.getString("color") ?: ""
            
            Habit(
                id = id,
                title = title,
                frequency = frequency,
                streak = streak,
                completedToday = completedToday,
                createdAt = createdAt ?: java.util.Date(),
                history = history,
                icon = icon,
                color = color
            )
        }
    }
    
    fun getHabitsFlow() = callbackFlow {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val registration: ListenerRegistration = db.collection("users")
            .document(userId)
            .collection("habits")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                
                val habits = snapshot?.documents?.mapNotNull { document ->
                    val id = document.id
                    val title = document.getString("title") ?: return@mapNotNull null
                    val frequency = document.getString("frequency") ?: return@mapNotNull null
                    val streak = document.getLong("streak")?.toInt() ?: 0
                    val history = document.get("history") as? Map<String, Boolean> ?: mapOf()
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val completedToday = history[today] == true
                    val createdAt = document.getDate("createdAt")
                    val icon = document.getString("icon") ?: ""
                    val color = document.getString("color") ?: ""
                    
                    Habit(
                        id = id,
                        title = title,
                        frequency = frequency,
                        streak = streak,
                        completedToday = completedToday,
                        createdAt = createdAt ?: java.util.Date(),
                        history = history,
                        icon = icon,
                        color = color
                    )
                } ?: emptyList()
                
                trySend(Result.success(habits))
            }
        
        awaitClose {
            registration.remove()
        }
    }
}