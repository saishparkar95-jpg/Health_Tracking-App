package com.healthtrackai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val steps: Int,
    val avatarEmoji: String,
    val isCurrentUser: Boolean = false
)

@Composable
fun ChallengesScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isCollegeChallengeJoined by remember { mutableStateOf(true) }

    val leaderboard = listOf(
        LeaderboardUser(1, "${healthState.user.name.ifBlank { "Alex Rivera" }} (You)", 58450, "👑", true),
        LeaderboardUser(2, "Jordan Taylor", 52340, "🏃"),
        LeaderboardUser(3, "Maya Patel", 48920, "⚡"),
        LeaderboardUser(4, "Liam Vance", 45210, "👟"),
        LeaderboardUser(5, "Elena Rostova", 42100, "🌟")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Challenges & Leaderboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Community wellness quests & friendly competitions", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Streak Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmberAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AmberAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${healthState.currentStreakDays}-Day Wellness Streak!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AmberAccent))
                            Text(text = "Complete today's daily habits to keep your streak alive.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface))
                        }
                    }
                }
            }

            // 2. Active Featured Challenge Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(10.dp), color = EmeraldPrimary.copy(alpha = 0.15f)) {
                                Text(
                                    text = "🏆 Featured College Challenge",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "142 joined", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Campus 50,000 Step Quest", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Walk 50,000 steps this week. Safe, private aggregated leaderboard.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isCollegeChallengeJoined = !isCollegeChallengeJoined },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCollegeChallengeJoined) EmeraldPrimary else CyanAccent)
                        ) {
                            Text(text = if (isCollegeChallengeJoined) "Joined (Active on Leaderboard) ✓" else "Join Challenge")
                        }
                    }
                }
            }

            // 3. Leaderboard
            item {
                Text(
                    text = "COMMUNITY STEP LEADERBOARD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                leaderboard.forEach { u ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (u.isCurrentUser) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        border = if (u.isCurrentUser) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "#${u.rank}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (u.rank == 1) AmberAccent else MaterialTheme.colorScheme.onSurface))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(text = u.avatarEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = u.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (u.isCurrentUser) FontWeight.Bold else FontWeight.Medium))
                            }
                            Text(text = "${u.steps.formatNumber()} steps", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (u.isCurrentUser) EmeraldPrimary else MaterialTheme.colorScheme.onSurface))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun Int.formatNumber(): String {
    return java.text.NumberFormat.getIntegerInstance().format(this)
}
