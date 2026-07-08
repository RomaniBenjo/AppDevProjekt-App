package com.example.commingsoon.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class Profile(
    val id: Int,
    val name: String,
    val image: Int? = null
)

class ProfileViewModel : ViewModel() {
    var profile by mutableStateOf(ProfilePlaceholder.profile)
        private set

    fun updateName(name: String) {
        profile = profile.copy(
            name = name
        )
    }

    fun updateImage(image: Int?) {
        profile = profile.copy(
            image = image
        )
    }
}