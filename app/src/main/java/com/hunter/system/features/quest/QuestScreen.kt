package com.hunter.system.features.quest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunter.system.core.database.TaskEntity
import com.hunter.system.core.database.TaskStatus
import com.hunter.system.core.sensors.StepProvider
import com.hunter.system.core.sensors.StepSource
import com.hunter.system.core.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun QuestScreen(viewModel: QuestViewModel = hiltViewModel()) {
  val tasks by viewModel.tasks.collectAsStateWithLifecycle()
  val history by viewModel.completedHistory.collectAsStateWithLifecycle()
  var newTaskText by remember { mutableStateOf("") }
  var showAddField by remember { mutableStateOf(false) }
  var showHistory by remember { mutableStateOf(false) }
  var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
  var editText by remember { mutableStateOf("") }
  val stepSource by viewModel.stepSource.collectAsStateWithLifecycle()
  val hasHcPermission by viewModel.hasHealthConnectPermission.collectAsStateWithLifecycle()

  val context = androidx.compose.ui.platform.LocalContext.current

  val hcPermissionLauncher = rememberLauncherForActivityResult(
      PermissionController.createRequestPermissionResultContract()
  ) { viewModel.forceSync() }

  val requestHealthConnectPermission: () -> Unit = {
    hcPermissionLauncher.launch(StepProvider.REQUIRED_PERMISSIONS)
  }

  Box(modifier = Modifier.fillMaxSize().background(SystemBlack)) {
    ParticleBackground()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
      Spacer(Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          GlitchText(
              text = "DAILY QUESTS",
              style = MaterialTheme.typography.headlineMedium.copy(
                  color = SystemBlue, fontWeight = FontWeight.Black, letterSpacing = 4.sp
              )
          )
          Text(
              text = LocalDate.now().let {
                "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, " +
                "${it.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${it.dayOfMonth}"
              },
              style = MaterialTheme.typography.bodyMedium,
              color = SystemBlue.copy(alpha = 0.6f)
          )
        }
        IconButton(onClick = { showHistory = true }) {
          Icon(Icons.Default.History, contentDescription = "History", tint = SystemBlue.copy(alpha = 0.7f))
        }
        FilledIconButton(
            onClick = { showAddField = !showAddField },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (showAddField) SystemBlue else SystemBlue.copy(alpha = 0.2f),
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(40.dp),
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add quest", modifier = Modifier.size(20.dp))
        }
      }

      AnimatedVisibility(visible = showAddField, enter = expandVertically(), exit = shrinkVertically()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
              value = newTaskText, onValueChange = { newTaskText = it },
              placeholder = { Text("New quest...", color = Color.White.copy(alpha = 0.3f)) },
              modifier = Modifier.weight(1f), singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SystemBlue, unfocusedBorderColor = SystemBlue.copy(alpha = 0.3f),
                  cursorColor = SystemBlue, focusedTextColor = Color.White, unfocusedTextColor = Color.White,
              ),
              shape = RoundedCornerShape(12.dp),
          )
          Spacer(Modifier.width(8.dp))
          FilledIconButton(
              onClick = { viewModel.addTask(newTaskText); newTaskText = ""; showAddField = false },
              enabled = newTaskText.isNotBlank(),
              colors = IconButtonDefaults.filledIconButtonColors(
                  containerColor = SystemBlue, contentColor = Color.White,
                  disabledContainerColor = SystemBlue.copy(alpha = 0.2f),
              ),
              shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp),
          ) { Icon(Icons.Default.Send, contentDescription = "Submit", modifier = Modifier.size(18.dp)) }
        }
      }

      Spacer(Modifier.height(12.dp))

      LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks, key = { it.id }) { task ->
          val isPending = task.status == TaskStatus.PENDING
          val isStepTask = task.stepGoal != null
          val canSwipe = isPending && !isStepTask

          SwipeableCard(
              swipeRightAction = if (canSwipe) ({ viewModel.completeTask(task.id) }) else null,
              swipeLeftAction = if (canSwipe) ({ viewModel.discardTask(task.id) }) else null,
              swipeRightColor = Color(0xFF00FF88),
              swipeLeftColor = Color(0xFFFF4444),
              swipeRightIcon = Icons.Default.Check,
              swipeLeftIcon = Icons.Default.Close,
          ) {
            TaskCardContent(
                task = task,
                carryoverDays = viewModel.carryoverDays(task),
                stepSource = if (isStepTask) stepSource else null,
                hasHcPermission = hasHcPermission,
                onForceSync = { viewModel.forceSync() },
                onRequestPermission = requestHealthConnectPermission,
                onTap = {
                  if (isPending && !task.isDefault && !isStepTask) {
                    editingTask = task; editText = task.description
                  }
                },
                onEdit = { editingTask = task; editText = task.description },
                onDelete = { viewModel.deleteTask(task.id) },
                onMoveUp = { viewModel.moveTaskUp(task.id) },
                onMoveDown = { viewModel.moveTaskDown(task.id) },
                onComplete = { viewModel.completeTask(task.id) },
                onDiscard = { viewModel.discardTask(task.id) },
            )
          }
        }

        if (tasks.isEmpty()) {
          item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
              Text("No quests yet.\nTap + to add a quest.", color = Color.White.copy(alpha = 0.4f),
                  style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
          }
        }
      }
    }
  }

  if (editingTask != null) {
    AlertDialog(
        onDismissRequest = { editingTask = null },
        confirmButton = { TextButton(onClick = { viewModel.editTask(editingTask!!.id, editText); editingTask = null }) { Text("SAVE", color = SystemBlue) } },
        dismissButton = { TextButton(onClick = { editingTask = null }) { Text("CANCEL", color = Color.Gray) } },
        title = { Text("EDIT QUEST", color = SystemBlue, letterSpacing = 2.sp) },
        text = {
          OutlinedTextField(value = editText, onValueChange = { editText = it }, singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SystemBlue, unfocusedBorderColor = SystemBlue.copy(alpha = 0.3f),
                  cursorColor = SystemBlue, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
              modifier = Modifier.fillMaxWidth())
        },
        containerColor = Color(0xFF0A0A14).copy(alpha = 0.95f),
    )
  }

  if (showHistory) {
    AlertDialog(
        onDismissRequest = { showHistory = false },
        confirmButton = { TextButton(onClick = { showHistory = false }) { Text("CLOSE", color = SystemBlue) } },
        title = { Text("QUEST HISTORY", color = SystemBlue, fontWeight = FontWeight.Bold, letterSpacing = 3.sp) },
        text = {
          if (history.isEmpty()) {
            Text("No completed quests yet.", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
          } else {
            val sortedDates = history.keys.sortedDescending()
            val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)) {
              sortedDates.forEach { date ->
                Text(date.format(formatter), style = MaterialTheme.typography.labelMedium,
                    color = SystemBlue.copy(alpha = 0.7f), fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                history[date]?.forEach { task ->
                  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00FF88).copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(task.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                  }
                }
              }
            }
          }
        },
        containerColor = Color(0xFF0A0A14).copy(alpha = 0.95f),
    )
  }
}

// ─── Custom swipe card ─────────────────────────────────────────────────

@Composable
private fun SwipeableCard(
    swipeRightAction: (() -> Unit)?,
    swipeLeftAction: (() -> Unit)?,
    swipeRightColor: Color,
    swipeLeftColor: Color,
    swipeRightIcon: ImageVector?,
    swipeLeftIcon: ImageVector?,
    content: @Composable () -> Unit,
) {
  val offsetX = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  var cardWidth by remember { mutableIntStateOf(1) }
  val threshold = cardWidth * 0.5f

  Box(
      modifier = Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .onSizeChanged { cardWidth = it.width },
  ) {
    // Background behind the card
    if (offsetX.value != 0f) {
      val isRight = offsetX.value > 0
      val bgColor = if (isRight) swipeRightColor else swipeLeftColor
      val icon = if (isRight) swipeRightIcon else swipeLeftIcon
      val alignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd
      val progress = (offsetX.value.absoluteValue / threshold).coerceIn(0f, 1f)

      Box(
          modifier = Modifier.matchParentSize()
              .background(bgColor.copy(alpha = progress * 0.4f))
              .padding(horizontal = 20.dp),
          contentAlignment = alignment,
      ) {
        icon?.let { Icon(it, contentDescription = null, tint = bgColor, modifier = Modifier.size(28.dp)) }
      }
    }

    // Card content with offset
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(swipeRightAction, swipeLeftAction) {
              if (swipeRightAction == null && swipeLeftAction == null) return@pointerInput
              detectHorizontalDragGestures(
                  onDragEnd = {
                    scope.launch {
                      when {
                        offsetX.value > threshold && swipeRightAction != null -> {
                          offsetX.animateTo(cardWidth.toFloat(), tween(200))
                          swipeRightAction()
                          offsetX.snapTo(0f)
                        }
                        offsetX.value < -threshold && swipeLeftAction != null -> {
                          offsetX.animateTo(-cardWidth.toFloat(), tween(200))
                          swipeLeftAction()
                          offsetX.snapTo(0f)
                        }
                        else -> offsetX.animateTo(0f, spring())
                      }
                    }
                  },
                  onDragCancel = { scope.launch { offsetX.animateTo(0f, spring()) } },
                  onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    val newVal = offsetX.value + dragAmount
                    val clamped = when {
                      swipeRightAction != null && swipeLeftAction != null -> newVal.coerceIn(-cardWidth.toFloat(), cardWidth.toFloat())
                      swipeRightAction != null -> newVal.coerceIn(0f, cardWidth.toFloat())
                      swipeLeftAction != null -> newVal.coerceIn(-cardWidth.toFloat(), 0f)
                      else -> 0f
                    }
                    scope.launch { offsetX.snapTo(clamped) }
                  }
              )
            }
    ) {
      content()
    }
  }
}

// ─── Task card content with long press menu ────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCardContent(
    task: TaskEntity,
    carryoverDays: Int,
    stepSource: StepSource? = null,
    hasHcPermission: Boolean = false,
    onForceSync: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onComplete: () -> Unit,
    onDiscard: () -> Unit,
) {
  val isCompleted = task.status == TaskStatus.COMPLETED
  val isDiscarded = task.status == TaskStatus.DISCARDED
  val isPending = task.status == TaskStatus.PENDING
  val isStepTask = task.stepGoal != null
  val isDefault = task.isDefault
  var showMenu by remember { mutableStateOf(false) }

  val gradient = carryoverGradient(carryoverDays)

  Box(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(gradient)
          .background(Color.White.copy(alpha = 0.05f))
          .combinedClickable(onClick = onTap, onLongClick = { showMenu = true })
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      if (carryoverDays > 0 && isPending) {
        Text("Carried over $carryoverDays day${if (carryoverDays > 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall, color = carryoverColor(carryoverDays),
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
      }

      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val statusIcon = when {
          isCompleted -> "✓"; isDiscarded -> "✗"; isStepTask -> "○"; else -> "□"
        }
        Text(statusIcon, style = MaterialTheme.typography.titleMedium,
            color = when {
              isCompleted -> Color(0xFF00FF88); isDiscarded -> Color(0xFFFF4444).copy(alpha = 0.5f)
              else -> SystemBlue.copy(alpha = 0.6f)
            }, modifier = Modifier.width(24.dp))

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(task.description,
              style = MaterialTheme.typography.bodyLarge.copy(
                  textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None),
              color = when {
                isCompleted -> Color(0xFF00FF88).copy(alpha = 0.7f)
                isDiscarded -> Color.White.copy(alpha = 0.3f)
                else -> Color.White
              }, fontWeight = if (isStepTask) FontWeight.Bold else FontWeight.Normal)

          if (isStepTask && isPending) {
            Spacer(Modifier.height(6.dp))
            val progress = (task.stepsCompleted.toFloat() / (task.stepGoal ?: 1)).coerceIn(0f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically) {
              LinearProgressIndicator(progress = { progress },
                  modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                  color = SystemBlue, trackColor = SystemBlue.copy(alpha = 0.15f))
              Spacer(Modifier.width(8.dp))
              Text("%,d / %,d".format(task.stepsCompleted, task.stepGoal),
                  style = MaterialTheme.typography.labelSmall, color = SystemBlue.copy(alpha = 0.8f))
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (stepSource != null) {
                val sourceLabel = when (stepSource) {
                  StepSource.HEALTH_CONNECT -> "Samsung Health"
                  StepSource.SENSOR -> "Phone sensor"
                  StepSource.NONE -> "Not synced"
                }
                Text("Source: $sourceLabel", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f))
              }

              Spacer(Modifier.weight(1f))

              if (!hasHcPermission) {
                TextButton(onClick = onRequestPermission, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)) {
                  Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Connect Health", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
                }
              }

              TextButton(onClick = onForceSync, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                  modifier = Modifier.height(28.dp)) {
                Icon(Icons.Default.Sync, null, tint = SystemBlue.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sync", style = MaterialTheme.typography.labelSmall, color = SystemBlue.copy(alpha = 0.7f))
              }
            }
          }
          if (isStepTask && isCompleted) {
            Text("%,d steps done".format(task.stepsCompleted),
                style = MaterialTheme.typography.labelSmall, color = Color(0xFF00FF88).copy(alpha = 0.5f))
          }
        }
      }
    }

    // Long press context menu
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = Color(0xFF1A1A2E)) {
      if (isPending && !isDefault && !isStepTask) {
        DropdownMenuItem(text = { Text("Edit", color = Color.White) }, onClick = { showMenu = false; onEdit() },
            leadingIcon = { Icon(Icons.Default.Edit, null, tint = SystemBlue) })
      }
      if (isPending) {
        DropdownMenuItem(text = { Text("Move Up", color = Color.White) }, onClick = { showMenu = false; onMoveUp() },
            leadingIcon = { Icon(Icons.Default.ArrowUpward, null, tint = SystemBlue) })
        DropdownMenuItem(text = { Text("Move Down", color = Color.White) }, onClick = { showMenu = false; onMoveDown() },
            leadingIcon = { Icon(Icons.Default.ArrowDownward, null, tint = SystemBlue) })
      }
      if (isPending && !isStepTask) {
        DropdownMenuItem(text = { Text("Complete", color = Color(0xFF00FF88)) }, onClick = { showMenu = false; onComplete() },
            leadingIcon = { Icon(Icons.Default.Check, null, tint = Color(0xFF00FF88)) })
        DropdownMenuItem(text = { Text("Discard", color = Color(0xFFFF6B35)) }, onClick = { showMenu = false; onDiscard() },
            leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF6B35)) })
      }
      if (!isDefault) {
        DropdownMenuItem(text = { Text("Delete", color = Color(0xFFFF4444)) }, onClick = { showMenu = false; onDelete() },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF4444)) })
      }
    }
  }
}
