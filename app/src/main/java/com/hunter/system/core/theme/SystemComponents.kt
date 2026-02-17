package com.hunter.system.core.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════
// SYSTEM WINDOW — The iconic blue bordered panels from Solo Leveling
// ═══════════════════════════════════════════════════════════════════

@Composable
fun SystemWindow(
        modifier: Modifier = Modifier,
        title: String? = null,
        glow: Boolean = false,
        content: @Composable () -> Unit
) {
  val glowAlpha by
          if (glow) {
            val transition = rememberInfiniteTransition(label = "glow")
            transition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.35f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                            ),
                    label = "glowAlpha"
            )
          } else {
            remember { mutableStateOf(0.08f) }
          }

  Box(
          modifier =
                  modifier.fillMaxWidth()
                          .background(
                                  Brush.linearGradient(
                                          colors =
                                                  listOf(
                                                          Color(0xF0, 0x00, 0x14, 0x28)
                                                                  .copy(alpha = 0.92f),
                                                          Color(0xFF, 0x00, 0x0A, 0x19)
                                                                  .copy(alpha = 0.95f)
                                                  )
                                  )
                          )
                          .border(1.dp, SystemBorder)
                          .drawBehind {
                            // Corner accents
                            val cornerSize = 12.dp.toPx()
                            val strokeWidth = 2.dp.toPx()
                            val accentColor = SystemPrimary

                            // Top-left
                            drawLine(
                                    accentColor,
                                    Offset(0f, 0f),
                                    Offset(cornerSize, 0f),
                                    strokeWidth
                            )
                            drawLine(
                                    accentColor,
                                    Offset(0f, 0f),
                                    Offset(0f, cornerSize),
                                    strokeWidth
                            )
                            // Top-right
                            drawLine(
                                    accentColor,
                                    Offset(size.width, 0f),
                                    Offset(size.width - cornerSize, 0f),
                                    strokeWidth
                            )
                            drawLine(
                                    accentColor,
                                    Offset(size.width, 0f),
                                    Offset(size.width, cornerSize),
                                    strokeWidth
                            )
                            // Bottom-left
                            drawLine(
                                    accentColor,
                                    Offset(0f, size.height),
                                    Offset(cornerSize, size.height),
                                    strokeWidth
                            )
                            drawLine(
                                    accentColor,
                                    Offset(0f, size.height),
                                    Offset(0f, size.height - cornerSize),
                                    strokeWidth
                            )
                            // Bottom-right
                            drawLine(
                                    accentColor,
                                    Offset(size.width, size.height),
                                    Offset(size.width - cornerSize, size.height),
                                    strokeWidth
                            )
                            drawLine(
                                    accentColor,
                                    Offset(size.width, size.height),
                                    Offset(size.width, size.height - cornerSize),
                                    strokeWidth
                            )

                            // Scan lines
                            val lineSpacing = 4.dp.toPx()
                            var y = 0f
                            while (y < size.height) {
                              drawRect(
                                      color = SystemPrimary.copy(alpha = 0.015f),
                                      topLeft = Offset(0f, y + 2.dp.toPx()),
                                      size = Size(size.width, 2.dp.toPx())
                              )
                              y += lineSpacing
                            }

                            // Outer glow
                            drawRect(
                                    color = SystemPrimary.copy(alpha = glowAlpha),
                                    topLeft = Offset(-2f, -2f),
                                    size = Size(size.width + 4f, size.height + 4f),
                                    style = Stroke(width = 1f)
                            )
                          }
  ) {
    Column {
      if (title != null) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .drawBehind {
                                  drawLine(
                                          color = SystemBorder,
                                          start = Offset(0f, size.height),
                                          end = Offset(size.width, size.height),
                                          strokeWidth = 1.dp.toPx()
                                  )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
          // Status indicator dot
          Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = SystemPrimary, radius = size.minDimension / 2)
          }
          Spacer(Modifier.width(8.dp))
          Text(
                  text = title,
                  style = MaterialTheme.typography.titleMedium,
                  color = SystemPrimary,
                  letterSpacing = 3.sp
          )
        }
      }
      Box(modifier = Modifier.padding(16.dp)) { content() }
    }
  }
}

// ═══════════════════════════════════════════════════════════════════
// STEP PROGRESS RING — Circular progress with gradient and glow
// ═══════════════════════════════════════════════════════════════════

@Composable
fun StepProgressRing(
        currentSteps: Int,
        goalSteps: Int,
        modifier: Modifier = Modifier,
        ringSize: Dp = 220.dp,
        strokeWidth: Dp = 8.dp
) {
  val percentage = (currentSteps.toFloat() / goalSteps).coerceIn(0f, 1f)
  val animatedPercentage by
          animateFloatAsState(
                  targetValue = percentage,
                  animationSpec = tween(durationMillis = 800),
                  label = "ringProgress"
          )

  // Glow pulse when near completion
  val glowTransition = rememberInfiniteTransition(label = "ringGlow")
  val glowPulse by
          glowTransition.animateFloat(
                  initialValue = 0.3f,
                  targetValue = if (percentage > 0.8f) 0.8f else 0.4f,
                  animationSpec =
                          infiniteRepeatable(
                                  animation = tween(1500),
                                  repeatMode = RepeatMode.Reverse
                          ),
                  label = "glowPulse"
          )

  Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val ringStroke = strokeWidth.toPx()
      val radius = (size.minDimension - ringStroke) / 2
      val center = Offset(size.width / 2, size.height / 2)

      // Background track
      drawCircle(
              color = SystemPrimary.copy(alpha = 0.08f),
              radius = radius,
              center = center,
              style = Stroke(width = ringStroke)
      )

      // Progress arc with gradient
      val sweepAngle = 360f * animatedPercentage
      rotate(-90f, center) {
        drawArc(
                brush =
                        Brush.sweepGradient(
                                colors =
                                        listOf(
                                                SystemPrimary,
                                                SystemPrimaryDark,
                                                SystemAccent,
                                                SystemPrimary
                                        )
                        ),
                startAngle = 0f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
        )
      }

      // Glow effect on the progress end point
      if (animatedPercentage > 0.01f) {
        val angle = Math.toRadians((-90.0 + sweepAngle))
        val endX = center.x + radius * cos(angle).toFloat()
        val endY = center.y + radius * sin(angle).toFloat()
        drawCircle(
                color = SystemPrimary.copy(alpha = glowPulse),
                radius = ringStroke * 2,
                center = Offset(endX, endY)
        )
      }
    }

    // Center text
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
              text = "%,d".format(currentSteps),
              style = MaterialTheme.typography.displayMedium,
              color = SystemTextPrimary,
              textAlign = TextAlign.Center
      )
      Text(
              text = "/ %,d steps".format(goalSteps),
              style = MaterialTheme.typography.labelMedium,
              color = SystemTextMuted,
              textAlign = TextAlign.Center
      )
      if (percentage >= 1f) {
        Text(
                text = "COMPLETE",
                style = MaterialTheme.typography.labelLarge,
                color = SystemSuccess,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════════════
// GLITCH TEXT — Text with periodic glitch/distortion effect
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlitchText(
        text: String,
        modifier: Modifier = Modifier,
        style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
        color: Color = SystemPrimary
) {
  var isGlitching by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(Random.nextLong(2000, 5000))
      isGlitching = true
      delay(150)
      isGlitching = false
    }
  }

  Box(modifier = modifier) {
    // Main text
    Text(text = text, style = style, color = color)
    // Glitch overlay layers
    if (isGlitching) {
      Text(
              text = text,
              style = style,
              color = Color(0xFF00D4FF).copy(alpha = 0.7f),
              modifier = Modifier.padding(start = 2.dp)
      )
      Text(
              text = text,
              style = style,
              color = Color(0xFFFF0040).copy(alpha = 0.5f),
              modifier = Modifier.padding(end = 2.dp)
      )
    }
  }
}

// ═══════════════════════════════════════════════════════════════════
// PARTICLE BACKGROUND — Floating particles in the background
// ═══════════════════════════════════════════════════════════════════

private data class Particle(
        var x: Float,
        var y: Float,
        val speed: Float,
        val alpha: Float,
        val size: Float
)

@Composable
fun ParticleBackground(
        modifier: Modifier = Modifier,
        particleCount: Int = 15,
        color: Color = SystemPrimary
) {
  val particles = remember {
    List(particleCount) {
      Particle(
              x = Random.nextFloat(),
              y = Random.nextFloat(),
              speed = Random.nextFloat() * 0.0005f + 0.0001f,
              alpha = Random.nextFloat() * 0.3f + 0.05f,
              size = Random.nextFloat() * 2f + 0.5f
      )
    }
  }

  val transition = rememberInfiniteTransition(label = "particles")
  val time by
          transition.animateFloat(
                  initialValue = 0f,
                  targetValue = 1f,
                  animationSpec =
                          infiniteRepeatable(
                                  animation = tween(20000, easing = LinearEasing),
                                  repeatMode = RepeatMode.Restart
                          ),
                  label = "particleTime"
          )

  Canvas(modifier = modifier.fillMaxSize()) {
    particles.forEach { p ->
      val y = (p.y + time * p.speed * 5000) % 1f
      drawCircle(
              color = color.copy(alpha = p.alpha),
              radius = p.size.dp.toPx(),
              center = Offset(p.x * size.width, y * size.height)
      )
    }
  }
}

// ═══════════════════════════════════════════════════════════════════
// STATUS BAR — Quest status with animated indicator
// ═══════════════════════════════════════════════════════════════════

@Composable
fun QuestStatusBar(
        status: String,
        modifier: Modifier = Modifier,
        color: Color = QuestActive,
        showPulse: Boolean = true
) {
  val pulseTransition = rememberInfiniteTransition(label = "statusPulse")
  val pulseAlpha by
          pulseTransition.animateFloat(
                  initialValue = 0.4f,
                  targetValue = 1f,
                  animationSpec =
                          infiniteRepeatable(
                                  animation = tween(1000),
                                  repeatMode = RepeatMode.Reverse
                          ),
                  label = "pulse"
          )

  Row(
          modifier =
                  modifier.fillMaxWidth()
                          .background(color.copy(alpha = 0.08f))
                          .border(1.dp, color.copy(alpha = 0.3f))
                          .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
  ) {
    if (showPulse) {
      Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = color.copy(alpha = pulseAlpha), radius = size.minDimension / 2)
      }
      Spacer(Modifier.width(10.dp))
    }
    Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            letterSpacing = 2.sp
    )
  }
}
