package com.example.comingsoon.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.comingsoon.data.AppPreferenceRepository
import com.example.comingsoon.language.AppLanguageViewModel
import com.example.comingsoon.friends.FriendsRepository
import com.example.comingsoon.ui.theme.AppThemeViewModel
import kotlin.jvm.java

class AppViewModelFactory (
    private val repository: AppPreferenceRepository,
    private val friendsRepository: FriendsRepository,
    private val context: Context
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
                FriendViewModel(friendsRepository, context.applicationContext) as T
            else ->
                throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}
