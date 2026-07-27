package com.example.comingsoon.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.viewmodels.Friend

@Composable
fun FriendAvatar(
    friend: Friend,
    modifier: Modifier = Modifier
) {
    val fallback = painterResource(friend.image ?: R.drawable.profile_placeholder)
    AsyncImage(
        model = friend.imageUrl,
        contentDescription = appString(R.string.profile_picture_of, friend.name),
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape)
    )
}
