package com.anydoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anydoc.ui.theme.AnyDocTheme
import com.anydoc.ui.theme.CardSurface
import com.anydoc.ui.theme.CoreWhite
import com.anydoc.ui.theme.SecondaryText
import com.anydoc.ui.theme.SurfaceDark
import com.anydoc.ui.theme.rememberHapticFeedback

@Composable
fun AppDrawer(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAboutClick: () -> Unit
) {
    val onTapHaptic = rememberHapticFeedback()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "AnyDoc",
            style = MaterialTheme.typography.headlineSmall,
            color = CoreWhite
        )
        Text(
            text = "Workspace",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
        Spacer(modifier = Modifier.padding(top = 6.dp))
        DrawerItem(
            label = "Home",
            icon = Icons.Outlined.Home,
            onClick = {
                onTapHaptic()
                onHomeClick()
            }
        )
        DrawerItem(
            label = "Search",
            icon = Icons.Outlined.Search,
            onClick = {
                onTapHaptic()
                onSearchClick()
            }
        )
        DrawerItem(
            label = "About",
            icon = Icons.Outlined.Info,
            onClick = {
                onTapHaptic()
                onAboutClick()
            }
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = CoreWhite
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun AppDrawerPreview() {
    AnyDocTheme {
        AppDrawer(onAboutClick = {})
    }
}
