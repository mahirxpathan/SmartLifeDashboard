package com.example.smartlifedashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlifedashboard.model.Task
import com.example.smartlifedashboard.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

enum class TaskSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST, 
    TITLE_A_TO_Z,
    TITLE_Z_TO_A,
    DUE_DATE_SOONEST,
    PRIORITY_HIGH_TO_LOW
}

class TaskReminderViewModel : ViewModel() {
    private val taskRepository = TaskRepository()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredTasks = MutableStateFlow<List<Task>>(emptyList())
    val filteredTasks: StateFlow<List<Task>> = _filteredTasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(TaskSortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder.asStateFlow()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.getTasksFlow().collect { taskList ->
                _tasks.value = taskList
                // Update filtered tasks with the current search query
                filterTasks(_searchQuery.value)
                _isLoading.value = false
                _error.value = null
            }
        }
    }
    
    private fun filterTasks(query: String) {
        val baseTasks = if (query.isEmpty()) {
            _tasks.value
        } else {
            _tasks.value.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true)
            }
        }
        
        // Apply sorting
        val sortedTasks = when (_sortOrder.value) {
            TaskSortOrder.NEWEST_FIRST -> baseTasks.sortedByDescending { it.createdAt }
            TaskSortOrder.OLDEST_FIRST -> baseTasks.sortedBy { it.createdAt }
            TaskSortOrder.TITLE_A_TO_Z -> baseTasks.sortedBy { it.title.lowercase() }
            TaskSortOrder.TITLE_Z_TO_A -> baseTasks.sortedByDescending { it.title.lowercase() }
            TaskSortOrder.DUE_DATE_SOONEST -> baseTasks.sortedWith(
                compareBy(nullsLast()) { task -> 
                    task.dueDate?.time
                }
            )
            TaskSortOrder.PRIORITY_HIGH_TO_LOW -> baseTasks.sortedWith(
                compareByDescending<Task> { task ->
                    when (task.priority) {
                        "high" -> "a"
                        "medium" -> "b" 
                        "low" -> "c"
                        else -> "d"
                    }
                }.thenBy { task -> task.createdAt }
            )
        }
        
        _filteredTasks.value = sortedTasks
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterTasks(query)
    }
    
    fun setSortOrder(sortOrder: TaskSortOrder) {
        _sortOrder.value = sortOrder
        filterTasks(_searchQuery.value)
    }
    
    fun getSortOrder(): TaskSortOrder = _sortOrder.value

    fun addTask(title: String, description: String = "", dueDate: java.util.Date? = null, priority: String = "medium", category: String = "") {
        viewModelScope.launch {
            try {
                val newTask = Task(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    category = category,
                    createdAt = java.util.Date()
                )
                taskRepository.addTask(newTask)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateTask(taskId: String, task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(taskId, task)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(task.id, task)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
                // Real-time updates are handled by the observer
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun markTaskAsCompleted(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val task = _tasks.value.find { it.id == taskId }
                if (task != null) {
                    val updatedTask = task.copy(isCompleted = isCompleted)
                    taskRepository.updateTask(taskId, updatedTask)
                    // Real-time updates are handled by the observer
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}