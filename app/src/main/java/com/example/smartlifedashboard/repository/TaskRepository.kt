package com.example.smartlifedashboard.repository

import com.example.smartlifedashboard.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addTask(task: Task): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val taskData = mapOf(
            "title" to task.title,
            "isCompleted" to task.isCompleted,
            "createdAt" to task.createdAt,
            "dueDate" to task.dueDate,
            "priority" to task.priority,
            "description" to task.description,
            "category" to task.category
        )
        val result = db.collection("users").document(userId).collection("tasks")
            .add(taskData).await()
        return result.id
    }

    suspend fun updateTask(taskId: String, task: Task) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val taskData = mapOf(
            "title" to task.title,
            "isCompleted" to task.isCompleted,
            "dueDate" to task.dueDate,
            "priority" to task.priority,
            "description" to task.description,
            "category" to task.category
        )
        db.collection("users").document(userId).collection("tasks")
            .document(taskId)
            .update(taskData)
            .await()
    }

    suspend fun deleteTask(taskId: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        db.collection("users").document(userId).collection("tasks")
            .document(taskId)
            .delete()
            .await()
    }

    suspend fun getTasks(): List<Task> {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val snapshot = db.collection("users").document(userId).collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { document ->
            val id = document.id
            val title = document.getString("title") ?: return@mapNotNull null
            val isCompleted = document.getBoolean("isCompleted") ?: false
            val createdAt = document.getDate("createdAt")
            val dueDate = document.getDate("dueDate")
            val priority = document.getString("priority") ?: "medium"
            val description = document.getString("description") ?: ""
            val category = document.getString("category") ?: ""
            
            Task(
                id = id,
                title = title,
                isCompleted = isCompleted,
                createdAt = createdAt ?: java.util.Date(),
                dueDate = dueDate,
                priority = priority,
                description = description,
                category = category
            )
        }
    }
    
    fun getTasksFlow(): Flow<List<Task>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val registration = db.collection("users")
            .document(userId)
            .collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Close the flow with an error
                    close(error)
                    return@addSnapshotListener
                }
                
                val tasks = snapshot?.documents?.mapNotNull { document ->
                    val id = document.id
                    val title = document.getString("title") ?: return@mapNotNull null
                    val isCompleted = document.getBoolean("isCompleted") ?: false
                    val createdAt = document.getDate("createdAt")
                    val dueDate = document.getDate("dueDate")
                    val priority = document.getString("priority") ?: "medium"
                    val description = document.getString("description") ?: ""
                    val category = document.getString("category") ?: ""
                    
                    Task(
                        id = id,
                        title = title,
                        isCompleted = isCompleted,
                        createdAt = createdAt ?: java.util.Date(),
                        dueDate = dueDate,
                        priority = priority,
                        description = description,
                        category = category
                    )
                } ?: emptyList()
                
                trySend(tasks)
            }
        
        awaitClose {
            registration.remove()
        }
    }
}