package com.example.ui.counter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CounterBackgroundTheme
import com.example.data.CounterDisplayStyle
import com.example.data.CounterPreferences
import com.example.data.MantraPresets
import com.example.ui.theme.CreamCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronDeep
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TwilightCard
import com.example.ui.theme.TwilightCreamText
import com.example.ui.theme.TwilightGold
import com.example.ui.theme.TwilightSaffron
import com.example.ui.theme.TwilightSubText
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class CounterPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val cardColor: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val ringTrack: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaamJapCounterScreen(
    state: CounterPreferences,
    malaCompletedSignal: Boolean,
    onBack: () -> Unit,
    onTapJap: () -> Unit,
    onUndoJap: () -> Unit,
    onResetCounter: (Boolean) -> Unit,
    onSelectPresetMantra: (String) -> Unit,
    onSetCustomMantra: (String) -> Unit,
    onSetMalaTarget: (Int) -> Unit,
    onSetDailyTarget: (Int) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onSetBackgroundTheme: (CounterBackgroundTheme) -> Unit,
    onSetDisplayStyle: (CounterDisplayStyle) -> Unit
) {
    val palette = rememberCounterPalette(state.backgroundTheme)
    val bgTop by animateColorAsState(palette.bgTop, label = "bgTop")
    val bgBottom by animateColorAsState(palette.bgBottom, label = "bgBottom")

    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val tapScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = palette.primaryText,
                    navigationIconContentColor = palette.primaryText,
                    actionIconContentColor = palette.primaryText
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("counter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "वापस जाएं"
                        )
                    }
                },
                title = {
                    Text(
                        text = "श्री राधा नाम जप",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { onSetSoundEnabled(!state.soundEnabled) },
                        modifier = Modifier.testTag("counter_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.soundEnabled) {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            },
                            contentDescription = if (state.soundEnabled) "ध्वनि बंद करें" else "ध्वनि चालू करें"
                        )
                    }
                    IconButton(
                        onClick = { onSetVibrationEnabled(!state.vibrationEnabled) },
                        modifier = Modifier.testTag("counter_vibration_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = "कंपन चालू या बंद करें",
                            tint = if (state.vibrationEnabled) palette.accent else palette.secondaryText.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(
                        onClick = { showCustomizeSheet = true },
                        modifier = Modifier.testTag("counter_customize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "कस्टमाइज़ करें"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CounterStatCard(
                        label = "कुल जप",
                        value = state.totalJapCount.toString(),
                        palette = palette,
                        testTag = "total_jap_value",
                        modifier = Modifier.weight(1f)
                    )
                    CounterStatCard(
                        label = "पूर्ण माला (${state.malaTarget})",
                        value = state.malaCount.toString(),
                        palette = palette,
                        testTag = "mala_count_value",
                        modifier = Modifier.weight(1f)
                    )
                    CounterStatCard(
                        label = "आज का जप",
                        value = state.todayJapCount.toString(),
                        palette = palette,
                        testTag = "today_jap_value",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Daily Target Progress Bar
                val dailyProgress by animateFloatAsState(
                    targetValue = (state.todayJapCount.toFloat() / state.dailyTarget.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f),
                    label = "dailyProgress"
                )
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.cardColor),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "दैनिक लक्ष्य: ${state.todayJapCount} / ${state.dailyTarget}",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primaryText
                            )
                            Text(
                                text = "${(dailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accent
                            )
                        }
                        LinearProgressIndicator(
                            progress = { dailyProgress },
                            color = palette.accent,
                            trackColor = palette.ringTrack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                // Mala Completion Celebration Banner
                AnimatedVisibility(
                    visible = malaCompletedSignal,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GoldContainer,
                        border = BorderStroke(1.dp, GoldAccent)
                    ) {
                        Text(
                            text = "🌸 1 माला पूर्ण हुई! राधे राधे 🙏",
                            style = MaterialTheme.typography.titleMedium,
                            color = SaffronDeep,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f, fill = false))

                // Central Naam Jap Counting Orb
                CentralJapButton(
                    mantraText = state.activeMantraText,
                    currentBeads = state.currentMalaBeads,
                    malaTarget = state.malaTarget,
                    displayStyle = state.displayStyle,
                    palette = palette,
                    scale = tapScale.value,
                    onTap = {
                        onTapJap()
                        coroutineScope.launch {
                            tapScale.snapTo(0.93f)
                            tapScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                )

                Text(
                    text = "मनका प्रगति: ${state.currentMalaBeads} / ${state.malaTarget}",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.secondaryText,
                    modifier = Modifier.testTag("current_mala_beads_text")
                )

                Spacer(modifier = Modifier.weight(0.5f, fill = false))

                // Bottom Action Row: Undo + Reset + Customize
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onUndoJap,
                        enabled = state.totalJapCount > 0L,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("counter_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "एक कम करें",
                            tint = if (state.totalJapCount > 0L) palette.accent else palette.secondaryText.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "पूर्ववत",
                            color = if (state.totalJapCount > 0L) palette.primaryText else palette.secondaryText.copy(alpha = 0.4f)
                        )
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        enabled = state.totalJapCount > 0L || state.todayJapCount > 0L,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("counter_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "रीसेट करें",
                            tint = palette.accent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "रीसेट",
                            color = palette.primaryText
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("काउंटर रीसेट पुष्टि", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    "क्या आप केवल आज का जप रीसेट करना चाहते हैं या संपूर्ण कुल जप व माला संख्या को शून्य करना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetCounter(false)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier.testTag("confirm_reset_all_button")
                ) {
                    Text("संपूर्ण रीसेट")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onResetCounter(true)
                            showResetDialog = false
                        },
                        modifier = Modifier.testTag("confirm_reset_today_button")
                    ) {
                        Text("केवल आज का")
                    }
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("रद्द करें")
                    }
                }
            }
        )
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CounterCustomizationSheet(
                state = state,
                onSelectPresetMantra = onSelectPresetMantra,
                onSetCustomMantra = onSetCustomMantra,
                onSetMalaTarget = onSetMalaTarget,
                onSetDailyTarget = onSetDailyTarget,
                onSetSoundEnabled = onSetSoundEnabled,
                onSetVibrationEnabled = onSetVibrationEnabled,
                onSetBackgroundTheme = onSetBackgroundTheme,
                onSetDisplayStyle = onSetDisplayStyle,
                onClose = { showCustomizeSheet = false }
            )
        }
    }
}

@Composable
private fun CounterStatCard(
    label: String,
    value: String,
    palette: CounterPalette,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardColor),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CentralJapButton(
    mantraText: String,
    currentBeads: Int,
    malaTarget: Int,
    displayStyle: CounterDisplayStyle,
    palette: CounterPalette,
    scale: Float,
    onTap: () -> Unit
) {
    val progress = (currentBeads.toFloat() / malaTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "malaRing")
    val infiniteTransition = rememberInfiniteTransition(label = "lotusSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mandalaRotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(276.dp)
            .scale(scale)
    ) {
        if (displayStyle == CounterDisplayStyle.LOTUS_MANDALA) {
            Image(
                painter = painterResource(id = R.drawable.img_lotus_mandala),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(268.dp)
                    .clip(CircleShape)
                    .rotate(rotation)
                    .alpha(0.32f)
            )
        }

        // Outer Mala / Progress Ring
        Canvas(modifier = Modifier.size(266.dp)) {
            val strokePx = if (displayStyle == CounterDisplayStyle.MINIMAL_CLASSIC) 6.dp.toPx() else 12.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = palette.ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(palette.accent, GoldBright, palette.accent)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (displayStyle == CounterDisplayStyle.MALA_RING) {
                val beadCount = 27
                val radius = (size.minDimension - strokePx) / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                for (i in 0 until beadCount) {
                    val angleRad = Math.toRadians((-90.0 + (360.0 / beadCount) * i))
                    val bx = cx + (radius * cos(angleRad)).toFloat()
                    val by = cy + (radius * sin(angleRad)).toFloat()
                    val filled = (i.toFloat() / beadCount.toFloat()) <= animatedProgress
                    drawCircle(
                        color = if (filled) Color.White else palette.accent.copy(alpha = 0.25f),
                        radius = if (filled) 3.dp.toPx() else 2.dp.toPx(),
                        center = Offset(bx, by)
                    )
                }
            }
        }

        // Main Tap Circle - single exclusive click handler (1 tap = 1 count)
        Surface(
            onClick = onTap,
            shape = CircleShape,
            shadowElevation = 10.dp,
            border = BorderStroke(2.dp, GoldBright),
            color = Color.Transparent,
            modifier = Modifier
                .size(214.dp)
                .testTag("counter_tap_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.92f),
                                SaffronDeep
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = mantraText,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$currentBeads",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFFFFEAB8),
                        modifier = Modifier.testTag("central_beads_value")
                    )
                    Text(
                        text = "स्पर्श करें",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CounterCustomizationSheet(
    state: CounterPreferences,
    onSelectPresetMantra: (String) -> Unit,
    onSetCustomMantra: (String) -> Unit,
    onSetMalaTarget: (Int) -> Unit,
    onSetDailyTarget: (Int) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onSetBackgroundTheme: (CounterBackgroundTheme) -> Unit,
    onSetDisplayStyle: (CounterDisplayStyle) -> Unit,
    onClose: () -> Unit
) {
    var customMantraInput by remember(state.customMantra) { mutableStateOf(state.customMantra) }
    var malaTargetInput by remember(state.malaTarget) { mutableStateOf(state.malaTarget.toString()) }
    var dailyTargetInput by remember(state.dailyTarget) { mutableStateOf(state.dailyTarget.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "नाम जप कस्टमाइज़ेशन",
            style = MaterialTheme.typography.titleLarge,
            color = InkPrimary
        )

        // 1. Mantra selection
        Text("नाम / मंत्र चुनें", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MantraPresets.options.forEach { preset ->
                val selected = !state.isCustomMantraActive && state.selectedMantra == preset
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPresetMantra(preset) },
                    label = { Text(preset) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SaffronContainer,
                        selectedLabelColor = SaffronDeep
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customMantraInput,
                onValueChange = { customMantraInput = it },
                label = { Text("कस्टम नाम लिखें") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_mantra_input")
            )
            Button(
                onClick = { onSetCustomMantra(customMantraInput) },
                enabled = customMantraInput.isNotBlank(),
                modifier = Modifier.testTag("apply_custom_mantra_button")
            ) {
                Text("सेट")
            }
        }

        // 2. Mala Target
        Text("माला लक्ष्य (मानक: 108)", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(27, 54, 108, 216).forEach { target ->
                FilterChip(
                    selected = state.malaTarget == target,
                    onClick = {
                        malaTargetInput = target.toString()
                        onSetMalaTarget(target)
                    },
                    label = { Text("$target मनके") }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = malaTargetInput,
                onValueChange = { value -> malaTargetInput = value.filter { it.isDigit() } },
                label = { Text("कस्टम माला संख्या") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    malaTargetInput.toIntOrNull()?.takeIf { it > 0 }?.let(onSetMalaTarget)
                }
            ) {
                Text("लागू करें")
            }
        }

        // 3. Daily Target
        Text("दैनिक लक्ष्य", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(108, 540, 1080, 2160).forEach { target ->
                FilterChip(
                    selected = state.dailyTarget == target,
                    onClick = {
                        dailyTargetInput = target.toString()
                        onSetDailyTarget(target)
                    },
                    label = { Text("$target जप") }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = dailyTargetInput,
                onValueChange = { value -> dailyTargetInput = value.filter { it.isDigit() } },
                label = { Text("कस्टम दैनिक लक्ष्य") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    dailyTargetInput.toIntOrNull()?.takeIf { it > 0 }?.let(onSetDailyTarget)
                }
            ) {
                Text("लागू करें")
            }
        }

        // 4. Sound & Vibration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("जप ध्वनि (Chime)", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.soundEnabled, onCheckedChange = onSetSoundEnabled)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("कंपन (Vibration)", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.vibrationEnabled, onCheckedChange = onSetVibrationEnabled)
        }

        // 5. Background Theme
        Text("पृष्ठभूमि रंग थीम", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CounterBackgroundTheme.entries.forEach { theme ->
                FilterChip(
                    selected = state.backgroundTheme == theme,
                    onClick = { onSetBackgroundTheme(theme) },
                    label = { Text(theme.displayNameHindi) }
                )
            }
        }

        // 6. Display Style
        Text("काउंटर दृश्य शैली", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CounterDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = state.displayStyle == style,
                    onClick = { onSetDisplayStyle(style) },
                    label = { Text(style.displayNameHindi) }
                )
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("पूर्ण")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun rememberCounterPalette(theme: CounterBackgroundTheme): CounterPalette = when (theme) {
    CounterBackgroundTheme.SAFFRON_CREAM -> CounterPalette(
        bgTop = Color(0xFFFFF1D6),
        bgBottom = Color(0xFFFFFBF2),
        cardColor = CreamCard,
        primaryText = InkPrimary,
        secondaryText = InkSecondary,
        accent = SaffronPrimary,
        ringTrack = Color(0xFFF3DEC0)
    )
    CounterBackgroundTheme.SHYAM_TWILIGHT -> CounterPalette(
        bgTop = Color(0xFF28160C),
        bgBottom = Color(0xFF170C06),
        cardColor = TwilightCard,
        primaryText = TwilightCreamText,
        secondaryText = TwilightSubText,
        accent = TwilightGold,
        ringTrack = Color(0xFF4D3121)
    )
    CounterBackgroundTheme.LOTUS_IVORY -> CounterPalette(
        bgTop = Color(0xFFFFEDF0),
        bgBottom = Color(0xFFFFFAF8),
        cardColor = Color.White,
        primaryText = Color(0xFF3E1821),
        secondaryText = Color(0xFF7B4854),
        accent = Color(0xFFC84B6B),
        ringTrack = Color(0xFFF7D5DE)
    )
    CounterBackgroundTheme.SACRED_SANDALWOOD -> CounterPalette(
        bgTop = Color(0xFFF5E6C8),
        bgBottom = Color(0xFFFFF8EC),
        cardColor = Color(0xFFFFFDF8),
        primaryText = Color(0xFF31200E),
        secondaryText = Color(0xFF6B5030),
        accent = GoldAccent,
        ringTrack = Color(0xFFE7D4B0)
    )
}
