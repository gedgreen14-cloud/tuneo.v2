package com.tuneo.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.AuthRepository
import com.tuneo.app.data.Profile
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackground
import com.tuneo.app.ui.theme.FeedPillBackground
import com.tuneo.app.ui.theme.FeedTextPrimary
import com.tuneo.app.ui.theme.FeedTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: (Profile) -> Unit,
    onGoToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> avatarUri = uri }

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
        Text(
            "Crée ton compte Tuneo",
            color = FeedTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Partage ce que tu écoutes avec tes amis.",
            color = FeedTextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Photo de profil
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(FeedPillBackground)
                    .clickable { photoLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Photo de profil",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Ajouter une photo",
                        tint = FeedTextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        TuneoTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = "Pseudo",
            icon = Icons.Default.Person
        )

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
                    errorMessage = "Renseigne un pseudo et un mot de passe."
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "Le mot de passe doit faire au moins 6 caractères."
                    return@Button
                }
                errorMessage = null
                isLoading = true
                scope.launch {
                    val avatarBytes = avatarUri?.let { uri ->
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                    }
                    val result = authRepository.signUp(username.trim(), password, avatarBytes)
                    isLoading = false
                    result.onSuccess {
                        val profile = authRepository.currentProfile()
                        if (profile != null) onSignedUp(profile)
                    }.onFailure { e ->
                        errorMessage = e.message ?: "Une erreur est survenue."
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
                Text("Créer mon compte", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Déjà un compte ?", color = FeedTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Se connecter",
                color = FeedAccentPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onGoToLogin() }
            )
        }
    }
}

@Composable
fun TuneoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = FeedTextSecondary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = FeedTextSecondary) },
        visualTransformation = visualTransformation,
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FeedTextPrimary,
            unfocusedTextColor = FeedTextPrimary,
            focusedContainerColor = FeedPillBackground,
            unfocusedContainerColor = FeedPillBackground,
            focusedBorderColor = FeedAccentPurple,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = FeedAccentPurple
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
