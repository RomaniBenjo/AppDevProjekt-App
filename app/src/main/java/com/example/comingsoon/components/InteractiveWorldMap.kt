package com.example.comingsoon.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.comingsoon.viewmodels.MapCountry
@Composable
fun InteractiveWorldMap(
    countries: List<MapCountry>,
    countryColors: Map<String, Color>,
    modifier: Modifier = Modifier,
    oceanColor: Color = Color(0xFFC4E8FC), // Soft ocean blue
    defaultCountryColor: Color = Color(0xFFECECEC), // Default unvisited country color
    borderColor: Color = Color(0xFF555555), // Dark gray borders
    borderWidth: Float = 0.3f,             // Border line thickness
    zoomable: Boolean = false,             // Allows zoom and pan when in fullscreen
    onCountrySelected: ((String) -> Unit)? = null
) {
    var scale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom/pan when zoomable is disabled or countries list changes
    LaunchedEffect(zoomable, countries) {
        scale = 1f
        panOffset = Offset.Zero
    }

    Canvas(
        modifier = modifier
            .then(
                if (zoomable) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.aspectRatio(2000f / 857f)
                }
            )
            .clipToBounds() // Prevent rendering map paths outside canvas borders
            .background(oceanColor) // Ocean color is the background of the canvas
            .then(
                if (zoomable) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 8f)
                            val actualZoom = newScale / oldScale
                            
                            // Determine the base scale and centering offsets
                            val baseScale = minOf(size.width / 2000f, size.height / 857f)
                            val mapWidth = 2000f * baseScale
                            val mapHeight = 857f * baseScale
                            val centerXOffset = (size.width - mapWidth) / 2f
                            val centerYOffset = (size.height - mapHeight) / 2f
                            
                            // Calculate raw new panOffset
                            val targetPanOffset = centroid - (centroid - panOffset) * actualZoom + pan
                            
                            // Constrain panOffset so countries stay within the canvas boundaries
                            val actualWidth = mapWidth * newScale
                            val actualHeight = mapHeight * newScale
                            
                            val minPanX = size.width - actualWidth - centerXOffset
                            val maxPanX = -centerXOffset
                            val coercedPanX = if (actualWidth > size.width) {
                                targetPanOffset.x.coerceIn(minPanX, maxPanX)
                            } else {
                                0f
                            }
                            
                            val minPanY = size.height - actualHeight - centerYOffset
                            val maxPanY = -centerYOffset
                            val coercedPanY = if (actualHeight > size.height) {
                                targetPanOffset.y.coerceIn(minPanY, maxPanY)
                            } else {
                                0f
                            }
                            
                            panOffset = Offset(coercedPanX, coercedPanY)
                            scale = newScale
                        }
                    }
                } else {
                    Modifier
                }
            )
            .pointerInput(countries, scale, panOffset, zoomable) {
                detectTapGestures(
                    onDoubleTap = {
                        if (zoomable) {
                            scale = 1f
                            panOffset = Offset.Zero
                        }
                    },
                    onTap = { offset ->
                        val baseScale = minOf(size.width / 2000f, size.height / 857f)
                        val mapWidth = 2000f * baseScale
                        val mapHeight = 857f * baseScale
                        val centerXOffset = (size.width - mapWidth) / 2f
                        val centerYOffset = (size.height - mapHeight) / 2f
                        
                        if (baseScale > 0) {
                            // Translate and scale the click coordinate back to SVG coordinate space
                            val svgX = (offset.x - panOffset.x - centerXOffset) / (baseScale * scale)
                            val svgY = (offset.y - panOffset.y - centerYOffset) / (baseScale * scale)
                            
                            // Find the clicked country (search backwards to check top-most paths first)
                            val clickedCountry = countries.findLast { country ->
                                val androidPath = country.path.asAndroidPath()
                                val rectF = android.graphics.RectF()
                                androidPath.computeBounds(rectF, true)
                                val region = android.graphics.Region()
                                region.setPath(androidPath, android.graphics.Region(
                                    rectF.left.toInt(),
                                    rectF.top.toInt(),
                                    rectF.right.toInt(),
                                    rectF.bottom.toInt()
                                ))
                                region.contains(svgX.toInt(), svgY.toInt())
                            }
                            if (clickedCountry != null) {
                                onCountrySelected?.invoke(clickedCountry.id)
                            }
                        }
                    }
                )
            }
    ) {
        val baseScale = minOf(size.width / 2000f, size.height / 857f)
        val mapWidth = 2000f * baseScale
        val mapHeight = 857f * baseScale
        val centerXOffset = (size.width - mapWidth) / 2f
        val centerYOffset = (size.height - mapHeight) / 2f
        
        translate(panOffset.x + centerXOffset, panOffset.y + centerYOffset) {
            scale(scale, pivot = Offset.Zero) {
                scale(baseScale, pivot = Offset.Zero) {
                    countries.forEach { country ->
                        val fillPayloadColor = countryColors[country.id] ?: defaultCountryColor
                        drawPath(
                            path = country.path,
                            color = fillPayloadColor,
                            style = Fill
                        )
                        drawPath(
                            path = country.path,
                            color = borderColor,
                            style = Stroke(width = borderWidth)
                        )
                    }
                }
            }
        }
    }
}
