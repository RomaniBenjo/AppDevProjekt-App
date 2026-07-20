package com.example.comingsoon.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.data.AppPreferenceRepository
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val repository: AppPreferenceRepository
) : ViewModel() {

    private var journeyReminderEnabledState  by mutableStateOf(true)
    private var journeyReminderTimeState by mutableStateOf(LocalTime.of(9, 0))

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect {
                journeyReminderTimeState = it.notificationTime
            }
        }
    }

    fun isJourneyReminderEnabled() = journeyReminderEnabledState

    fun getReminderTime() = journeyReminderTimeState

    fun updateJourneyReminderEnabled(enabled: Boolean) {
        journeyReminderEnabledState  = enabled

        viewModelScope.launch {
            repository.saveJourneyReminderEnabled(enabled)
        }
    }

    fun updateReminderTime(time: LocalTime) {
        journeyReminderTimeState = time

        viewModelScope.launch {
            repository.saveNotificationTime(time)
        }
    }
}