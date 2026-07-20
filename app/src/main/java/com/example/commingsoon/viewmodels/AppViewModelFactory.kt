package com.example.commingsoon.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commingsoon.data.AppPreferenceRepository
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.friends.FriendsRepository
import com.example.commingsoon.ui.theme.AppThemeViewModel
import kotlin.jvm.java

class AppViewModelFactory (
    private val repository: AppPreferenceRepository,
    private val friendsRepository: FriendsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppThemeViewModel::class.java) ->
                AppThemeViewModel(repository) as T
            modelClass.isAssignableFrom(AppLanguageViewModel::class.java) ->
                AppLanguageViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository) as T
            modelClass.isAssignableFrom(FriendViewModel::class.java) ->
                FriendViewModel(friendsRepository) as T
            else ->
                throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}
