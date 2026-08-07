package com.tuneo.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.Profile
import com.tuneo.app.data.ProfileRepository
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackgroundDark
import com.tuneo.app.ui.theme.FeedBackgroundLight
import com.tuneo.app.ui.theme.FeedTextPrimaryDark
import com.tuneo.app.ui.theme.FeedTextPrimaryLight
import com.tuneo.app.ui.theme.FeedTextSecondaryDark
import com.tuneo.app.ui.theme.FeedTextSecondaryLight
import kotlinx.coroutines.launch

/**
 * Écran d'édition du profil connecté : pseudo, bio, lien, photo.
 * Upload direct dans le bucket Supabase "avatars" (même convention que SignUpScreen).
 */
@Composable
fun EditProfileScreen(
    profile: Profile,
    onCancel: () -> Unit,
    onSaved: (Profile) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) FeedBackgroundDark else FeedBackgroundLight
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight
    val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileRepository = remember { ProfileRepository() }

    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio ?: "") }
    var link by remember { mutableStateOf(profile.link_url ?: "") }
    var avatarUrl by remember { mutableStateOf(profile.avatar_url) }
    var pickedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) pickedAvatarUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Annuler", tint = primaryColor)
            }
            Text("Modifier le profil", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            TextButton(
                enabled = !isSaving && username.isNotBlank(),
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            pickedAvatarUri?.let { uri ->
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    val uploadedUrl = profileRepository.uploadAvatar(profile.id, bytes)
                                    profileRepository.updateAvatarUrl(profile.id, uploadedUrl)
                                    avatarUrl = uploadedUrl
                                }
                            }
                            profileRepository.updateProfile(
                                userId = profile.id,
                                username = username.trim(),
                                bio = bio.trim().ifBlank { null },
                                linkUrl = link.trim().ifBlank { null }
                            )
                            onSaved(
                                profile.copy(
                                    username = username.trim(),
                                    bio = bio.trim().ifBlank { null },
                                    link_url = link.trim().ifBlank { null },
                                    avatar_url = avatarUrl
                                )
                            )
                        } catch (e: Exception) {
                            errorMessage = "Impossible d'enregistrer les modifications."
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Text(
                    if (isSaving) "..." else "Enregistrer",
                    color = FeedAccentPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(FeedAccentPurple, Color(0xFF2B1A5E))))
                    .clickable { pickImageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val displayModel = pickedAvatarUri ?: avatarUrl
                if (displayModel != null) {
                    AsyncImage(
                        model = displayModel,
                        contentDescription = "Photo de profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FeedAccentPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Changer la photo", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        EditField(label = "Pseudo", value = username, onValueChange = { username = it }, primaryColor = primaryColor, secondaryColor = secondaryColor)
        Spacer(modifier = Modifier.height(18.dp))
        EditField(label = "Bio", value = bio, onValueChange = { bio = it }, primaryColor = primaryColor, secondaryColor = secondaryColor, maxLines = 4)
        Spacer(modifier = Modifier.height(18.dp))
        EditField(label = "Lien", value = link, onValueChange = { link = it }, primaryColor = primaryColor, secondaryColor = secondaryColor)

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Color(0xFFE0455F), fontSize = 13.sp)
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    maxLines: Int = 1
) {
    Column {
        Text(label, color = secondaryColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            maxLines = maxLines,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryColor,
                unfocusedTextColor = primaryColor,
                focusedBorderColor = FeedAccentPurple,
                unfocusedBorderColor = secondaryColor.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
