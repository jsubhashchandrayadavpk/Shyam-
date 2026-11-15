package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CounterPreferences
import com.example.data.ReminderSettings
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.KumkumMaroon
import com.example.ui.theme.OutlineWarm
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronDeep
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TulsiContainer
import com.example.ui.theme.TulsiGreen

@Composable
fun HomeScreen(
    counterPrefs: CounterPreferences,
    reminderSettings: ReminderSettings,
    onOpenNaamJap: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenApkDownload: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = CreamBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF3DF),
                            CreamBackground,
                            Color(0xFFFFF8EC)
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Banner
                HeroDevotionalHeader(
                    todayJap = counterPrefs.todayJapCount,
                    malaCount = counterPrefs.malaCount,
                    reminderActive = reminderSettings.enabled && !reminderSettings.paused
                )

                Text(
                    text = "मुख्य सेवाएं",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )

                // Option 1: श्री राधा नाम जप
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 5 }
                    )
                ) {
                    DevotionalHomeOptionCard(
                        title = stringResource(R.string.home_card_jap_title),
                        subtitle = stringResource(R.string.home_card_jap_desc),
                        badgeText = "कुल जप: ${counterPrefs.totalJapCount} • माला: ${counterPrefs.malaCount}",
                        icon = Icons.Rounded.SelfImprovement,
                        accentColor = SaffronPrimary,
                        iconContainerColor = SaffronContainer,
                        testTag = "home_card_naam_jap",
                        onClick = onOpenNaamJap
                    )
                }

                // Option 2: श्री राधा नाम जप रिमाइंडर
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 4 }
                    )
                ) {
                    val reminderBadge = when {
                        reminderSettings.enabled && !reminderSettings.paused ->
                            "सक्रिय • प्रत्येक ${reminderSettings.intervalMinutes} मिनट"
                        reminderSettings.enabled && reminderSettings.paused ->
                            "विराम पर (Paused)"
                        else -> "अभी बंद है"
                    }
                    DevotionalHomeOptionCard(
                        title = stringResource(R.string.home_card_reminder_title),
                        subtitle = stringResource(R.string.home_card_reminder_desc),
                        badgeText = reminderBadge,
                        icon = Icons.Rounded.NotificationsActive,
                        accentColor = GoldAccent,
                        iconContainerColor = GoldContainer,
                        badgeBackground = if (reminderSettings.enabled && !reminderSettings.paused) {
                            TulsiContainer
                        } else {
                            GoldContainer
                        },
                        badgeTextColor = if (reminderSettings.enabled && !reminderSettings.paused) {
                            TulsiGreen
                        } else {
                            InkSecondary
                        },
                        testTag = "home_card_reminder",
                        onClick = onOpenReminder
                    )
                }

                // Option 3: SHYAM APK डाउनलोड करें
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    DevotionalHomeOptionCard(
                        title = stringResource(R.string.home_card_download_title),
                        subtitle = stringResource(R.string.home_card_download_desc),
                        badgeText = "संस्करण जांचें व इंस्टॉल करें",
                        icon = Icons.Rounded.SystemUpdate,
                        accentColor = KumkumMaroon,
                        iconContainerColor = Color(0xFFFFE5DF),
                        testTag = "home_card_apk_download",
                        onClick = onOpenApkDownload
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "॥ राधे राधे • सर्वदा श्री राधा-श्याम के चरणों में ॥",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroDevotionalHeader(
    todayJap: Long,
    malaCount: Long,
    reminderActive: Boolean
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_vrindavan_banner),
                contentDescription = "श्री वृंदावन धाम भक्ति चित्र",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x662B1204),
                                Color(0xCC3C1604)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.shyam_launcher_fg),
                        contentDescription = "SHYAM ऐप लोगो",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = "SHYAM • श्याम",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "॥ श्री राधा नाम जप साधना ॥",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFE5B8)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroStatPill(
                        label = "आज: $todayJap",
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "माला: $malaCount",
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = if (reminderActive) "रिमाइंडर: चालू" else "रिमाइंडर: बंद",
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x55FFFFFF),
        border = BorderStroke(0.75.dp, Color(0x88FFE4B5))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun DevotionalHomeOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    iconContainerColor: Color,
    badgeBackground: Color = SaffronContainer.copy(alpha = 0.7f),
    badgeTextColor: Color = SaffronDeep,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        border = BorderStroke(1.dp, OutlineWarm),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = iconContainerColor,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBackground
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = "खोलें",
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
