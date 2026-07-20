package com.example.comingsoon.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.comingsoon.auth.AuthenticatedUser

data class Profile(
    val id: Long,
    val name: String,
    val image: Int? = null,
    val imageUrl: String? = null
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
            image = image,
            imageUrl = null
        )
    }

    fun updateFromAuthenticatedUser(user: AuthenticatedUser) {
        profile = Profile(
            id = user.id,
            name = user.name?.takeIf { it.isNotBlank() }
                ?: user.email.substringBefore('@'),
            image = null,
            imageUrl = user.pictureUrl
        )
    }
}
