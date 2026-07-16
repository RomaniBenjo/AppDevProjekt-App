package com.example.commingsoon.ui.screens.localopenguesser.connection

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.example.commingsoon.R

internal class LocalConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = NearbyConnectionManager(application)
    val state = manager.state

    init {
        manager.setLocalName(
            Build.MODEL.ifBlank { application.getString(R.string.local_guesser_android_player) }
        )
    }

    fun setLocalName(name: String) = manager.setLocalName(name)
    fun hasGooglePlayServices() = manager.hasGooglePlayServices()
    fun host() = manager.startHosting()
    fun join() = manager.startJoining()
    fun connect(endpoint: NearbyEndpoint) = manager.requestConnection(endpoint)
    fun accept() = manager.acceptPendingConnection()
    fun reject() = manager.rejectPendingConnection()
    fun sendTestMessage(message: String) = manager.sendTestMessage(message)
    fun startGame(settings: LocalGameSettings) = manager.startGame(settings)
    fun setGuess(latitude: Double, longitude: Double) = manager.setGuess(latitude, longitude)
    fun continueAfterRound() = manager.continueAfterRound()
    fun stopSearching() = manager.stopSearching()
    fun disconnect() = manager.disconnect()

    override fun onCleared() {
        manager.close()
        super.onCleared()
    }
}
