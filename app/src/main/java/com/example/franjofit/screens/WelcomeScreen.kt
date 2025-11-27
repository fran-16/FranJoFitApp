package com.example.franjofit.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.franjofit.R
import com.example.franjofit.ui.components.GradientBackground
import com.example.franjofit.ui.components.PrimaryButton
import com.example.franjofit.ui.components.Spacing
import com.example.franjofit.ui.components.TextButtonWhite
import com.example.franjofit.ui.components.TitleLogo

@Composable
fun WelcomeScreen(
    onSignUp: () -> Unit,
    onLogin: () -> Unit
) {

    Box(Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.fondowelcome),
            contentDescription = "Welcome Background Image",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Crop
        )


        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
        ) { TitleLogo() }

        AnimatedVisibility(
            visible = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            enter = fadeIn(animationSpec = tween(700)) +
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(700)
                    )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

                PrimaryButton(text = "Registrarse", onClick = onSignUp)
                Spacer(Modifier.height(Spacing.md))

                TextButtonWhite(text = "Iniciar sesión", onClick = onLogin)
            }
        }
    }
}
