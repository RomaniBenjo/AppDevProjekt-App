package com.example.commingsoon.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.commingsoon.ui.theme.AppThemeAssets

@Composable
fun AppHeader(
    title: String,
    onMenuClick: () -> Unit,
    assets: AppThemeAssets
) {
    Box {
       // Background with Fade at the end
       Box(
           modifier = Modifier
               .fillMaxWidth()
               .height(220.dp)
               .background(MaterialTheme.colorScheme.secondary)
       )
       Box(
           modifier = Modifier
               .fillMaxWidth()
               .height(20.dp)
               .background(
                   Brush.verticalGradient(
                       colors = listOf(
                           MaterialTheme.colorScheme.secondary,
                           Color.Transparent
                       )
                   )
               )
       )

        // Graphic at Bottom of Header
        Image(
            painter = painterResource(assets.headerShape),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillBounds,
            alpha = 0.9f
        )

        // Content (Title & Burger Menu)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}