package com.replock.app.presentation.stats

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.replock.app.data.local.AppDatabase
import com.replock.app.data.local.UserProgressEntity
import com.replock.app.data.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StreakViewModel(private val repository: ProgressRepository) : ViewModel() {
    val sessions: StateFlow<List<UserProgressEntity>> =
        repository.getAllSessions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _allTimePbEntry = MutableStateFlow<UserProgressEntity?>(null)
    val allTimePbEntry: StateFlow<UserProgressEntity?> = _allTimePbEntry.asStateFlow()

    init {
        viewModelScope.launch {
            _currentStreak.value = repository.getCurrentStreak()
        }
        viewModelScope.launch {
            sessions.collect { list ->
                _allTimePbEntry.value = list.maxByOrNull { it.repsDone }
                _currentStreak.value = repository.getCurrentStreak()
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = AppDatabase.getInstance(context.applicationContext)
                StreakViewModel(ProgressRepository(db.userProgressDao()))
            }
        }
    }
}

private val ColorBg = Color(0xFF07070B)
private val ColorSurface = Color(0xFF0F0F18)
private val ColorCard = Color(0xFF171722)
private val ColorBorder = Color(0xFF1E1E2E)
private val ColorAccentPurple = Color(0xFF8B6FFF)
private val ColorAccentCyan = Color(0xFF29D9C2)
private val ColorAccentGreen = Color(0xFF22D06A)
private val ColorAmber = Color(0xFFFFB74D)
private val ColorDanger = Color(0xFFFF4757)
private val ColorTextPrimary = Color(0xFFF0F0FF)
private val ColorTextMuted = Color(0xFF5E5E78)
private val ColorTextSub = Color(0xFF9090A8)

@Composable
fun StreakScreen(viewModel: StreakViewModel, onBack: () -> Unit) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val streak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val pbEntry by viewModel.allTimePbEntry.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(ColorBg).statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(CircleShape).background(ColorSurface)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ColorTextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("STATS", color = ColorTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W700, letterSpacing = 2.sp)
                }
            }

            item { StreakHero(streak = streak) }

            item {
                Spacer(Modifier.height(20.dp))
                AllTimePbBanner(pbEntry = pbEntry)
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("RECENT SESSIONS", color = ColorTextMuted, fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }

            if (sessions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No sessions yet.\nStart your first push-up session!", color = ColorTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(sessions) { session -> SessionRow(session = session, overallPb = pbEntry?.repsDone ?: 0) }
            }
        }
    }
}

@Composable
private fun StreakHero(streak: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = if (streak > 0) 1.12f else 1f, animationSpec = infiniteRepeatable(animation = tween(900, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "flameScale")

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔥", fontSize = 52.sp, modifier = Modifier.scale(if (streak > 0) flameScale else 1f))
        Spacer(Modifier.height(4.dp))
        Text("$streak", color = ColorTextPrimary, fontSize = 96.sp, fontWeight = FontWeight.W900, letterSpacing = (-4).sp)
        Text(if (streak == 1) "DAY STREAK" else "DAY STREAK", color = if (streak > 0) ColorAccentCyan else ColorTextMuted, fontSize = 13.sp, fontWeight = FontWeight.W700, letterSpacing = 2.sp)
        if (streak == 0) {
            Spacer(Modifier.height(6.dp))
            Text("Complete today's goal to start your streak", color = ColorTextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AllTimePbBanner(pbEntry: UserProgressEntity?) {
    Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(ColorAccentPurple.copy(alpha = 0.18f), ColorAccentCyan.copy(alpha = 0.12f)))).border(1.dp, ColorAccentPurple.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("ALL-TIME PB", color = ColorTextMuted, fontSize = 9.sp, fontWeight = FontWeight.W700, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${pbEntry?.repsDone ?: 0}", color = ColorAccentCyan, fontSize = 36.sp, fontWeight = FontWeight.W900)
                    Spacer(Modifier.width(6.dp))
                    Text("reps", color = ColorTextSub, fontSize = 13.sp, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("🏆", fontSize = 30.sp)
                if (pbEntry != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(ProgressRepository.formatDateLabel(pbEntry.date), color = ColorTextMuted, fontSize = 11.sp)
                    Text(pbEntry.programName.uppercase(), color = ColorAccentPurple.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: UserProgressEntity, overallPb: Int) {
    val wasPb = session.repsDone == overallPb && overallPb > 0
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).clip(RoundedCornerShape(14.dp)).background(ColorCard).border(width = 1.dp, color = if (wasPb) ColorAccentCyan.copy(alpha = 0.35f) else ColorBorder, shape = RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ProgressRepository.formatDateLabel(session.date), color = ColorTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W700)
                if (wasPb) {
                    Spacer(Modifier.width(7.dp))
                    PbBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgramChip(name = session.programName)
                Spacer(Modifier.width(8.dp))
                Text(ProgressRepository.formatDuration(session.durationSecs), color = ColorTextMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${session.repsDone}", color = if (session.completedGoal) ColorAccentGreen else ColorAccentPurple, fontSize = 24.sp, fontWeight = FontWeight.W900)
            Text("reps", color = ColorTextMuted, fontSize = 10.sp)
            if (session.streakDay > 0) {
                Spacer(Modifier.height(2.dp))
                Text("🔥 Day ${session.streakDay}", color = ColorAmber, fontSize = 10.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable
private fun PbBadge() {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ColorAccentCyan.copy(alpha = 0.18f)).border(1.dp, ColorAccentCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text("PB", color = ColorAccentCyan, fontSize = 8.sp, fontWeight = FontWeight.W800, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ProgramChip(name: String) {
    val color = when (name.lowercase()) {
        "easy" -> ColorAccentGreen
        "medium" -> ColorAccentCyan
        "hard" -> ColorAccentPurple
        "extreme" -> ColorDanger
        else -> ColorTextMuted
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(name.uppercase(), color = color, fontSize = 8.sp, fontWeight = FontWeight.W700, letterSpacing = 1.sp)
    }
}
