package com.example.franjofit.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.White
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen() {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val initialPhoto = user?.photoUrl
    val displayName = user?.displayName ?: "Usuario"
    val email = user?.email ?: "correo@ejemplo.com"

    var pickedImage by remember { mutableStateOf<Uri?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImage = uri

        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlue)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.BottomEnd) {
                val borderModifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(2.dp, White.copy(alpha = 0.6f), CircleShape)

                when {
                    pickedImage != null -> {
                        AsyncImage(
                            model = pickedImage,
                            contentDescription = "Foto de perfil",
                            modifier = borderModifier
                        )
                    }
                    initialPhoto != null -> {
                        AsyncImage(
                            model = initialPhoto,
                            contentDescription = "Foto de perfil",
                            modifier = borderModifier
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Sin foto",
                            tint = White.copy(alpha = 0.85f),
                            modifier = borderModifier
                        )
                    }
                }


                IconButton(
                    onClick = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar foto",
                        tint = White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
