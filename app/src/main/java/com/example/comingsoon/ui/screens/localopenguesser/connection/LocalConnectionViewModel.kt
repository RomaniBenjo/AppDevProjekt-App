package com.example.comingsoon.ui.screens.localopenguesser.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel

internal class LocalConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = NearbyConnectionManager(application)
    val state = manager.state

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
