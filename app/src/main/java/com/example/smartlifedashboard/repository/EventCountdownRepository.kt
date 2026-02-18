package com.example.smartlifedashboard.repository

import com.example.smartlifedashboard.model.EventCountdown
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

class EventCountdownRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addEvent(event: EventCountdown): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val eventData = mapOf(
            "title" to event.title,
            "eventDate" to event.eventDate,
            "createdAt" to event.createdAt,
            "description" to event.description
        )
        val result = db.collection("users").document(userId).collection("events")
            .add(eventData).await()
        return result.id
    }

    suspend fun updateEvent(eventId: String, event: EventCountdown) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val eventData = mapOf(
            "title" to event.title,
            "eventDate" to event.eventDate,
            "description" to event.description
        )
        db.collection("users").document(userId).collection("events")
            .document(eventId)
            .update(eventData)
            .await()
    }

    suspend fun deleteEvent(eventId: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        db.collection("users").document(userId).collection("events")
            .document(eventId)
            .delete()
            .await()
    }

    suspend fun getEvents(): List<EventCountdown> {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val snapshot = db.collection("users").document(userId).collection("events")
            .orderBy("eventDate", Query.Direction.ASCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { document ->
            val id = document.id
            val title = document.getString("title") ?: return@mapNotNull null
            val eventDate = document.getDate("eventDate")
            val createdAt = document.getDate("createdAt")
            val description = document.getString("description") ?: ""
            
            EventCountdown(
                id = id,
                title = title,
                eventDate = eventDate ?: java.util.Date(),
                createdAt = createdAt ?: java.util.Date(),
                description = description
            )
        }
    }
    
    fun getEventsFlow(): Flow<List<EventCountdown>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val registration = db.collection("users")
            .document(userId)
            .collection("events")
            .orderBy("eventDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Close the flow with an error
                    close(error)
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { document ->
                    val id = document.id
                    val title = document.getString("title") ?: return@mapNotNull null
                    val eventDate = document.getDate("eventDate")
                    val createdAt = document.getDate("createdAt")
                    val description = document.getString("description") ?: ""
                    
                    EventCountdown(
                        id = id,
                        title = title,
                        eventDate = eventDate ?: java.util.Date(),
                        createdAt = createdAt ?: java.util.Date(),
                        description = description
                    )
                } ?: emptyList()
                
                trySend(events)
            }
        
        awaitClose {
            registration.remove()
        }
    }
}