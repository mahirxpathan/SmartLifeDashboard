package com.example.smartlifedashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlifedashboard.model.EventCountdown
import com.example.smartlifedashboard.repository.EventCountdownRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

enum class EventSortOrder {
    SOONEST_FIRST,
    FARTHEST_FIRST,
    TITLE_A_TO_Z,
    TITLE_Z_TO_A,
    NEWEST_ADDED,
    OLDEST_ADDED
}

class EventCountdownViewModel : ViewModel() {
    private val eventRepository = EventCountdownRepository()

    private val _events = MutableStateFlow<List<EventCountdown>>(emptyList())
    val events: StateFlow<List<EventCountdown>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(EventSortOrder.SOONEST_FIRST)
    val sortOrder: StateFlow<EventSortOrder> = _sortOrder.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredEvents = MutableStateFlow<List<EventCountdown>>(emptyList())
    val filteredEvents: StateFlow<List<EventCountdown>> = _filteredEvents.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { eventList ->
                _events.value = eventList
                filterAndSortEvents(eventList)
                _isLoading.value = false
                _error.value = null
            }
        }
    }
    
    private fun filterAndSortEvents(eventList: List<EventCountdown>) {
        val query = _searchQuery.value.lowercase()
        val filtered = if (query.isEmpty()) {
            eventList
        } else {
            eventList.filter { event ->
                event.title.lowercase().contains(query) ||
                event.description.lowercase().contains(query)
            }
        }
        val sortedEvents = sortEvents(filtered)
        _filteredEvents.value = sortedEvents
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterAndSortEvents(_events.value)
    }

    fun addEvent(title: String, eventDate: java.util.Date, description: String = "") {
        viewModelScope.launch {
            try {
                val newEvent = EventCountdown(
                    title = title,
                    eventDate = eventDate,
                    description = description,
                    createdAt = java.util.Date()
                )
                eventRepository.addEvent(newEvent)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateEvent(eventId: String, event: EventCountdown) {
        viewModelScope.launch {
            try {
                eventRepository.updateEvent(eventId, event)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                eventRepository.deleteEvent(eventId)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun calculateDaysRemaining(eventDate: java.util.Date): Int {
        val currentDate = java.util.Calendar.getInstance()
        currentDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
        currentDate.set(java.util.Calendar.MINUTE, 0)
        currentDate.set(java.util.Calendar.SECOND, 0)
        currentDate.set(java.util.Calendar.MILLISECOND, 0)
        
        val eventCalendar = java.util.Calendar.getInstance()
        eventCalendar.time = eventDate
        
        // Clear time parts for date comparison only
        val eventDateOnly = java.util.Calendar.getInstance()
        eventDateOnly.time = eventDate
        eventDateOnly.set(java.util.Calendar.HOUR_OF_DAY, 0)
        eventDateOnly.set(java.util.Calendar.MINUTE, 0)
        eventDateOnly.set(java.util.Calendar.SECOND, 0)
        eventDateOnly.set(java.util.Calendar.MILLISECOND, 0)
        
        val diffInMillis = eventDateOnly.timeInMillis - currentDate.timeInMillis
        val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
        
        return diffInDays.toInt()
    }
    
    private fun sortEvents(events: List<EventCountdown>): List<EventCountdown> {
        return when (_sortOrder.value) {
            EventSortOrder.SOONEST_FIRST -> events.sortedBy { it.eventDate.time }
            EventSortOrder.FARTHEST_FIRST -> events.sortedByDescending { it.eventDate.time }
            EventSortOrder.TITLE_A_TO_Z -> events.sortedBy { it.title.lowercase() }
            EventSortOrder.TITLE_Z_TO_A -> events.sortedByDescending { it.title.lowercase() }
            EventSortOrder.NEWEST_ADDED -> events.sortedByDescending { it.createdAt.time }
            EventSortOrder.OLDEST_ADDED -> events.sortedBy { it.createdAt.time }
        }
    }
    
    fun setSortOrder(sortOrder: EventSortOrder) {
        _sortOrder.value = sortOrder
        // Re-filter and sort the current events
        filterAndSortEvents(_events.value)
    }
    
    fun getSortOrder(): EventSortOrder = _sortOrder.value
}