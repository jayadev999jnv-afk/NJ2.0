package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun VoiceVisualizer(
    isSpeaking: Boolean,
    isListening: Boolean,
    amplitude: Float = 0f,
    theme: VisualizerTheme = VisualizerTheme.Professional
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_visualizer")
    
    // Theme-based colors with smooth transitions
    val targetPrimaryColor = if (theme == VisualizerTheme.Professional) Color(0xFF00B4D8) else Color(0xFFFF00FF)
    val targetSecondaryColor = if (theme == VisualizerTheme.Professional) Color(0xFF90E0EF) else Color(0xFF00FFFF)
    
    val primaryColor by animateColorAsState(targetValue = targetPrimaryColor, animationSpec = tween(1000), label = "primary_color")
    val secondaryColor by animateColorAsState(targetValue = targetSecondaryColor, animationSpec = tween(1000), label = "secondary_color")

    val targetOrbColor1 = if (theme == VisualizerTheme.Professional) Color(0xFF0077B6) else Color(0xFFFF00FF)
    val targetOrbColor2 = if (theme == VisualizerTheme.Professional) Color(0xFF00B4D8) else Color(0xFF7000FF)
    
    val orbColor1 by animateColorAsState(targetValue = targetOrbColor1, animationSpec = tween(1000), label = "orb_color_1")
    val orbColor2 by animateColorAsState(targetValue = targetOrbColor2, animationSpec = tween(1000), label = "orb_color_2")

    // Smooth the amplitude for visual stability
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isListening || isSpeaking) amplitude.coerceIn(0.1f, 1f) else 0.05f,
        animationSpec = tween(100, easing = LinearOutSlowInEasing),
        label = "amplitude"
    )

    val animationState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (isSpeaking || isListening) {
                    if (theme == VisualizerTheme.Professional) 1000 else 600
                } else 3000, 
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Base Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = (0.2f + animatedAmplitude * 0.3f).coerceIn(0f, 1f)),
                            Color.Transparent
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val baseRadius = size.minDimension / 4f
            val radius = baseRadius + (animatedAmplitude * 40f)
            
            // Draw Multiple Wave Layers
            val layers = if (theme == VisualizerTheme.Professional) 3 else 5
            for (layer in 1..layers) {
                val layerPhase = animationState + (layer * 0.5f)
                val layerAmplitude = (15f + animatedAmplitude * 40f) / (if (theme == VisualizerTheme.Professional) layer else 1)
                
                val points = 80
                val path = androidx.compose.ui.graphics.Path()
                
                for (i in 0..points) {
                    val angle = (i.toFloat() / points) * 2f * Math.PI.toFloat()
                    val wave = sin(angle * (3 + layer) + layerPhase) * layerAmplitude +
                               sin(angle * (2 * layer) - layerPhase * 0.8f) * (layerAmplitude / 2f)
                    
                    val r = radius + wave
                    val x = center.x + r * kotlin.math.cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                
                drawPath(
                    path = path,
                    color = (if (layer % 2 == 0) primaryColor else secondaryColor).copy(alpha = 0.6f / layer),
                    style = Stroke(width = (2.dp / layer).toPx())
                )
            }
            
            // Core pulsing circle
            drawCircle(
                color = secondaryColor,
                radius = baseRadius + (animatedAmplitude * 10f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Inner Glow Orb with subtle pulse
        val orbScale by animateFloatAsState(
            targetValue = if (isSpeaking || isListening) 1.1f + animatedAmplitude * 0.2f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "orb_scale"
        )
        
        Box(
            modifier = Modifier
                .size(60.dp * orbScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(orbColor1, orbColor2)
                    )
                )
        )
    }
}

@Composable
fun DecibelMeter(dbLevel: Float, peakLevel: Float) {
    val animatedDb by animateFloatAsState(
        targetValue = dbLevel,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "db_meter"
    )

    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${animatedDb.toInt()} dB",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
            Text(
                text = "PEAK",
                color = if (peakLevel > 0.8f) Color.Red.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Meter Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            val progress = ((dbLevel + 60f) / 60f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00B4D8), Color(0xFF90E0EF))
                        )
                    )
            )
            
            // Peak indicator line
            val peakProgress = ((20 * kotlin.math.log10(peakLevel.coerceAtLeast(0.001f)) + 60f) / 60f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (104.dp * peakProgress)) // Approximate based on 120dp width - padding
                    .background(if (peakLevel > 0.9f) Color.Red else Color.White)
            )
        }
    }
}
