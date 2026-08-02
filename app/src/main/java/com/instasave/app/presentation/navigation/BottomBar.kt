package com.instasave.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.instasave.app.presentation.theme.HairlineBorder
import com.instasave.app.presentation.theme.PitchBlack
import com.instasave.app.presentation.theme.TextMuted
import com.instasave.app.presentation.theme.TextPrimary

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home),
    BottomNavItem(Screen.Downloads, Icons.Default.Download),
    BottomNavItem(Screen.Login, Icons.Default.Lock),
    BottomNavItem(Screen.Settings, Icons.Default.Settings)
)

@Composable
fun BottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(PitchBlack)
            .border(width = 0.5.dp, color = HairlineBorder),
        containerColor = PitchBlack
    ) {
        BottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.screen.title,
                        tint = if (isSelected) TextPrimary else TextMuted
                    )
                },
                label = {
                    Text(
                        text = item.screen.title,
                        color = if (isSelected) TextPrimary else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
