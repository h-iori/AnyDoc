package com.ioristudios.anydoc.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppGlow
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSizes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizes = rememberAppSizes()
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AppColors.BrandStrong else AppColors.BorderSubtle,
        animationSpec = tween(durationMillis = AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "searchBorder"
    )
    val glowRadius by animateDpAsState(
        targetValue = if (isFocused) AppGlow.Lg else AppGlow.Sm,
        animationSpec = tween(durationMillis = AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "searchGlowRadius"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) AppColors.SurfaceElevated else AppColors.Surface,
        animationSpec = tween(durationMillis = AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "searchContainer"
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.searchBarHeight)
            .onFocusChanged { isFocused = it.isFocused }
            .neonGlow(
                color = AppColors.Brand.copy(alpha = if (isFocused) AppGlow.StrongAlpha else AppGlow.SubtleAlpha),
                radius = glowRadius,
                shape = MaterialTheme.shapes.medium
            ),
        placeholder = {
            Text("Search documents, files...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = AppColors.BorderStrong)
        },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = AppColors.BrandStrong,
            focusedLeadingIconColor = AppColors.BrandStrong,
            unfocusedLeadingIconColor = AppColors.BorderStrong,
            focusedTrailingIconColor = AppColors.BrandStrong,
            unfocusedTrailingIconColor = AppColors.BorderStrong,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            focusedLabelColor = Color.Transparent,
            unfocusedLabelColor = Color.Transparent
        ),
        singleLine = true
    )
}
