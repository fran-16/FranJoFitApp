package com.example.franjofit.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.franjofit.data.UserRepository
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val displayName = user?.displayName ?: "Usuario"
    val email = user?.email ?: "correo@ejemplo.com"

    var remotePhotoUrl by remember { mutableStateOf<String?>(user?.photoUrl?.toString()) }
    var localPreview by remember { mutableStateOf<Uri?>(null) }
    var subiendo by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // traer photoUrl guardado en Firestore (si existe)
    LaunchedEffect(Unit) {
        UserRepository.getUserProfileOrNull()?.photoUrl?.let { remotePhotoUrl = it }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            localPreview = uri
            scope.launch {
                try {
                    subiendo = true
                    val url = UserRepository.uploadProfilePhoto(uri)
                    UserRepository.savePhotoUrl(url)
                    remotePhotoUrl = url
                    snack.showSnackbar("Foto actualizada")
                } catch (e: Exception) {
                    localPreview = null
                    snack.showSnackbar("Error al subir: ${e.message}")
                } finally {
                    subiendo = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Perfil", color = White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DeepBlue)
            )
        },
        snackbarHost = { SnackbarHost(snack) }
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
            // Foto + editar
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.BottomEnd) {
                val fotoMod = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(2.dp, White.copy(0.6f), CircleShape)

                when {
                    localPreview != null -> AsyncImage(localPreview, "Foto", fotoMod)
                    remotePhotoUrl != null -> AsyncImage(remotePhotoUrl, "Foto", fotoMod)
                    else -> Icon(Icons.Default.AccountCircle, "Sin foto", tint = White.copy(0.85f), modifier = fotoMod)
                }

                IconButton(
                    onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, White, CircleShape)
                ) { Icon(Icons.Default.Edit, contentDescription = "Editar foto", tint = White) }
            }

            if (subiendo) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            Text(displayName, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(email, color = White.copy(0.8f), fontSize = 14.sp)

            Spacer(Modifier.height(30.dp))

            val opciones = listOf(
                OpcionPerfil("Editar perfil", Icons.Default.Person),
                OpcionPerfil("Dormir", Icons.Default.Bedtime),
                OpcionPerfil("Progreso", Icons.Default.ShowChart),
                OpcionPerfil("Objetivos", Icons.Default.Flag),
                OpcionPerfil("Pasos", Icons.Default.DirectionsWalk),
                OpcionPerfil("Comidas", Icons.Default.Restaurant),
                OpcionPerfil("Ajustes", Icons.Default.Settings),
                OpcionPerfil("Ayuda", Icons.Default.Help)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                opciones.forEach { TarjetaOpcionPerfil(it) }
            }
        }
    }
}

@Composable
private fun TarjetaOpcionPerfil(item: OpcionPerfil) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White.copy(0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = item.titulo, tint = White.copy(0.9f), modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Text(item.titulo, color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

data class OpcionPerfil(val titulo: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
