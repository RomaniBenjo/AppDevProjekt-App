package com.example.commingsoon.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.example.commingsoon.ui.theme.AppThemeAssets

@Composable
fun AppHeader(
    title: String,
    onMenuClick: () -> Unit,
    assets: AppThemeAssets
) {
    Box (
        modifier = Modifier.height(150.dp).fillMaxWidth()
    ) {
       // Background with Fade at the end
       Box(
           modifier = Modifier
               .fillMaxSize()
               .padding(bottom = 25.dp)
               .background(MaterialTheme.colorScheme.secondary)
       )
       Box(
           modifier = Modifier
               .fillMaxWidth()
               .height(40.dp)
               .align(Alignment.BottomCenter)
               .offset(y = 15.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp),
            contentScale = ContentScale.FillBounds,
            alpha = 0.9f
        )

        // Content (Title & Burger Menu)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 10.dp)
                .padding(top= 50.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}