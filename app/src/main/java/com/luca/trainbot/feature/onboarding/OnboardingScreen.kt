package com.luca.trainbot.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luca.trainbot.core.data.OnboardingStore
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val emoji: String,
)

private val pages = listOf(
    OnboardingPage(
        title = "Antrenează",
        subtitle = "Învață AI-ul tău cu poze. Cu cât mai multe, cu atât mai bine!",
        icon = Icons.Default.Psychology,
        emoji = "🧠",
    ),
    OnboardingPage(
        title = "Testează",
        subtitle = "Vezi cât de bine recunoaște ce îi arăți.",
        icon = Icons.Default.Visibility,
        emoji = "🔍",
    ),
    OnboardingPage(
        title = "Descoperă AI-ul",
        subtitle = "Descoperă cum funcționează inteligența artificială și vorbește cu bot-ul tău!",
        icon = Icons.Default.AutoAwesome,
        emoji = "✨",
    ),
)

/**
 * First-launch onboarding carousel — mirrors iOS OnboardingView.
 * Shows 3 slides (Antrenează / Testează / Descoperă AI). Persists "seen" flag via [OnboardingStore].
 */
@Composable
fun OnboardingScreen(
    onboardingStore: OnboardingStore,
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { index ->
            OnboardingPageContent(page = pages[index])
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                val color by animateColorAsState(
                    targetValue = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    label = "indicator_color",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        // CTA button
        Button(
            onClick = {
                scope.launch {
                    if (pagerState.currentPage < pages.size - 1) {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    } else {
                        onboardingStore.markSeen()
                        onFinish()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(bottom = 40.dp)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        ) {
            Text(
                text = if (pagerState.currentPage < pages.size - 1) "Mai departe" else "Începe!",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon circle with gradient — mirrors iOS MascotView placeholder
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = page.emoji, style = MaterialTheme.typography.displayLarge)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
