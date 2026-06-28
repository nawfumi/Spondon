package com.spondon.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.BloodRed

data class BottomNavItem(
    val labelKey: String, // key into SpondonStrings
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("home", "home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("communities", "community_list", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem("request_feed", "request_feed", Icons.Filled.Bloodtype, Icons.Outlined.Bloodtype),
    BottomNavItem("findDonor", "find_donor", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("profile", "profile", Icons.Filled.Person, Icons.Outlined.Person),
)

/**
 * Resolves the translated label for a nav item key.
 */
@Composable
private fun resolveLabel(key: String): String {
    val s = S.strings
    return when (key) {
        "home" -> s.home
        "communities" -> s.communities
        "request_feed" -> s.bloodRequests
        "findDonor" -> s.findDonor
        "profile" -> s.profile
        else -> key
    }
}

@Composable
fun SpondonBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val dynamicFontSize = (screenWidth * 10f / 360f).coerceIn(8f, 12f).sp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = BloodRed.copy(alpha = 0.1f),
                spotColor = BloodRed.copy(alpha = 0.15f),
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val label = resolveLabel(item.labelKey)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(item.route) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) BloodRed
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = dynamicFontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (isSelected) BloodRed
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}