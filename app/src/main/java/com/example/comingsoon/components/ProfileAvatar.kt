package com.example.comingsoon.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import com.example.comingsoon.R
import com.example.comingsoon.viewmodels.Profile

@Composable
fun ProfileAvatar(
    profile: Profile,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val fallback = painterResource(profile.image ?: R.drawable.profile_placeholder)

    AsyncImage(
        model = profile.imageUrl,
        contentDescription = contentDescription,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape)
    )
}
