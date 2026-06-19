package com.luca.trainbot.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.luca.trainbot.BuildConfig
import com.luca.trainbot.core.data.AuthRepository
import com.luca.trainbot.core.data.AuthState
import com.luca.trainbot.core.data.OnboardingStore
import kotlinx.coroutines.launch

/**
 * Settings screen — mirrors iOS SettingsView.
 * Sections: Cont, Tutorial, Legal, Despre.
 * Logout calls POST /auth/logout then clears tokens.
 */
@Composable
fun SettingsScreen(
    authRepository: AuthRepository,
    onboardingStore: OnboardingStore,
    onLogout: () -> Unit,
    onShowOnboarding: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authState by authRepository.authState.collectAsState(initial = AuthState.Unauthenticated)
    val userName = (authState as? AuthState.Authenticated)?.userName ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Setări",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        // --- Cont section ---
        SettingsSection(title = "Cont") {
            if (userName.isNotBlank()) {
                SettingsRow(
                    icon = Icons.Default.Person,
                    label = "Conectat ca",
                    value = userName,
                )
            }
        }

        // --- Tutorial section ---
        SettingsSection(title = "Tutorial") {
            SettingsClickableRow(
                icon = Icons.Default.School,
                label = "Afișează din nou tutorialul",
                onClick = {
                    scope.launch {
                        onboardingStore.resetOnboarding()
                        onShowOnboarding()
                    }
                },
            )
        }

        // --- Legal section ---
        SettingsSection(title = "Legal") {
            SettingsClickableRow(
                icon = Icons.Default.PrivacyTip,
                label = "Politică de confidențialitate",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://trainbot.moldluca.tech/privacy.html"))
                    context.startActivity(intent)
                },
            )
            SettingsClickableRow(
                icon = Icons.Default.Gavel,
                label = "Termeni și Condiții",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://trainbot.moldluca.tech/terms.html"))
                    context.startActivity(intent)
                },
            )
        }

        // --- Despre section ---
        SettingsSection(title = "Despre") {
            SettingsRow(
                icon = Icons.Default.Info,
                label = "Versiune",
                value = "TrainBot v${BuildConfig.VERSION_NAME}",
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logout button
        Button(
            onClick = {
                scope.launch {
                    authRepository.logout()
                    onLogout()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(
                text = "Deconectează-te",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsClickableRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
