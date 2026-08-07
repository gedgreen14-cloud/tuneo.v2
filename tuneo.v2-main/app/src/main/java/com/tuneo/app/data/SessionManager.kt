package com.tuneo.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * État de connexion partagé entre les écrans (bouton partage, feed, stories...).
 * S'appuie sur AuthRepository ; expose l'état sous forme de state Compose.
 */
class SessionManager(private val authRepository: AuthRepository = AuthRepository()) {

    var isAuthenticated by mutableStateOf(false)
        private set
    var currentProfile by mutableStateOf<Profile?>(null)
        private set

    fun refresh(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val userId = authRepository.currentUserId
            isAuthenticated = userId != null
            currentProfile = if (userId != null) authRepository.currentProfile() else null
        }
    }

    fun onSignedIn(profile: Profile, scope: CoroutineScope) {
        isAuthenticated = true
        currentProfile = profile
    }

    fun signOut(scope: CoroutineScope, onDone: () -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            authRepository.signOut()
            isAuthenticated = false
            currentProfile = null
            onDone()
        }
    }
}
