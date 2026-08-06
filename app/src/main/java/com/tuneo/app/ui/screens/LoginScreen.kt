package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuneo.app.data.AuthRepository
import com.tuneo.app.data.Profile
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackground
import com.tuneo.app.ui.theme.FeedTextPrimary
import com.tuneo.app.ui.theme.FeedTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignedIn: (Profile) -> Unit,
    onGoToSignUp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FeedBackground)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = FeedTextPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Content de te revoir", color = FeedTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Connecte-toi pour partager ce que tu écoutes.", color = FeedTextSecondary, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(28.dp))

        TuneoTextField(value = username, onValueChange = { username = it }, placeholder = "Pseudo", icon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(14.dp))
        TuneoTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mot de passe",
            icon = Icons.Default.Lock,
            visualTransformation = PasswordVisualTransformation()
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Color(0xFFE0455F), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Renseigne ton pseudo et ton mot de passe."
                    return@Button
                }
                errorMessage = null
                isLoading = true
                scope.launch {
                    val result = authRepository.signIn(username.trim(), password)
                    isLoading = false
                    result.onSuccess {
                        val profile = authRepository.currentProfile()
                        if (profile != null) onSignedIn(profile)
                    }.onFailure { e ->
                        errorMessage = "Pseudo ou mot de passe incorrect."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FeedAccentPurple)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Se connecter", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Pas encore de compte ?", color = FeedTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "S'inscrire",
                color = FeedAccentPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onGoToSignUp() }
            )
        }
    }
}
