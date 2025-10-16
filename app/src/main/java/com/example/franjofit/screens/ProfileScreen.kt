package com.example.franjofit.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import coil.compose.AsyncImage
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
@OptIn(ExperimentalMaterial3Api::class)
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
        if (uri != null) pickedImage = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Perfil",
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepBlue
                )
            )

        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(DeepBlue)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Imagen de perfil + botón editar
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
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = email,
                color = White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(30.dp))


            val options = listOf(
                OptionItem("Editar perfil", Icons.Default.Person),
                OptionItem("Dormir", Icons.Default.Bedtime),
                OptionItem("Progreso", Icons.Default.ShowChart),
                OptionItem("Objetivos", Icons.Default.Flag),
                OptionItem("Pasos", Icons.Default.DirectionsWalk),
                OptionItem("Comidas", Icons.Default.Restaurant),
                OptionItem("Ajustes", Icons.Default.Settings),
                OptionItem("Ayuda", Icons.Default.Help)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { item ->
                    ProfileOptionCard(item)
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionCard(item: OptionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: acción futura */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = White.copy(alpha = 0.9f),
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = item.title,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class OptionItem(
    val title: String,
    val icon: ImageVector
)
