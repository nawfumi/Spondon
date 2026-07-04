package com.spondon.app.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.common.formatDisplay
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.CommunityRole
import com.spondon.app.core.domain.model.User
import com.spondon.app.core.domain.model.UserRole
import com.spondon.app.core.ui.components.AvailabilityIndicator
import com.spondon.app.core.ui.components.BloodGroupBadge
import com.spondon.app.core.ui.components.RoleBadge
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.PendingAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpondonInfoScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.spondonState.collectAsState()
    val hideSensitiveData by viewModel.hideSensitiveData.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadSpondonCommunity() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    val isCommunityAdmin = state.currentUserRole == CommunityRole.ADMIN ||
            state.currentUserPlatformRole == UserRole.SUPER_ADMIN

    val tabTitles = buildList {
        add("Members")     // 0 - Members
        add("About")     // 1 - About
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Info",
                        fontWeight = FontWeight.Bold,
                    )
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BloodRed)
                }
            }
            state.community != null -> {
                val community = state.community!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = state.selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = BloodRed,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            if (state.selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                                    color = BloodRed,
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        },
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = state.selectedTab == index,
                                onClick = { viewModel.setSpondonTab(index) },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (state.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (state.selectedTab == index) BloodRed
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }

                    // Tab Content
                    when (state.selectedTab) {
                        0 -> {
                            // Members Tab
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                // Search bar
                                item(key = "member_search") {
                                    OutlinedTextField(
                                        value = state.memberSearchQuery,
                                        onValueChange = { viewModel.updateSpondonMemberSearchQuery(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        placeholder = { Text("Search...") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        trailingIcon = {
                                            if (state.memberSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.updateSpondonMemberSearchQuery("") }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        singleLine = true,
                                    )
                                }

                                // Member count header
                                item(key = "member_count") {
                                    val filteredCount = state.members.count { member ->
                                        state.memberSearchQuery.isBlank() ||
                                                member.name.contains(state.memberSearchQuery, ignoreCase = true) ||
                                                member.bloodGroup.contains(state.memberSearchQuery, ignoreCase = true)
                                    }
                                    Text(
                                        "$filteredCount members",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                val filteredMembers = state.members.filter { member ->
                                    state.memberSearchQuery.isBlank() ||
                                            member.name.contains(state.memberSearchQuery, ignoreCase = true) ||
                                            member.bloodGroup.contains(state.memberSearchQuery, ignoreCase = true)
                                }

                                if (filteredMembers.isEmpty()) {
                                    item(key = "members_empty") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                if (state.memberSearchQuery.isNotBlank()) "No member found"
                                                else "No members",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredMembers, key = { it.uid }) { user ->
                                        SpondonInfoMemberRow(
                                            user = user,
                                            community = community,
                                            viewModel = viewModel,
                                            isAdmin = isCommunityAdmin,
                                            hideSensitiveData = hideSensitiveData,
                                            onProfileClick = {
                                                navController.navigate("donor_profile/${user.uid}")
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // About Tab
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item(key = "about") {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        // Description card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Info,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        "Details",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    community.description.ifEmpty {
                                                        "স্পন্দন — the official community of the Spondon platform. " +
                                                                "Every user is automatically a member. Admin posts announcements, " +
                                                                "news, and updates here."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    lineHeight = 22.sp,
                                                )
                                            }
                                        }

                                        // Info card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Shield,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        "About",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Spacer(Modifier.height(12.dp))

                                                SpondonInfoAboutRow(
                                                    icon = Icons.Outlined.Shield,
                                                    label = "Type",
                                                    value = "Official Platform Community",
                                                    chipColor = AvailableGreen,
                                                    chipBg = AvailableGreen.copy(alpha = 0.1f),
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                SpondonInfoAboutRow(
                                                    icon = Icons.Outlined.People,
                                                    label = "Members",
                                                    value = "Everyone (Auto-join)",
                                                    chipColor = MaterialTheme.colorScheme.primary,
                                                    chipBg = MaterialTheme.colorScheme.primaryContainer,
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                SpondonInfoAboutRow(
                                                    icon = Icons.Outlined.CalendarMonth,
                                                    label = "Created at",
                                                    value = community.createdAt?.formatDisplay() ?: "—",
                                                    chipColor = MaterialTheme.colorScheme.secondary,
                                                    chipBg = MaterialTheme.colorScheme.secondaryContainer,
                                                )
                                            }
                                        }

                                        // Community rules card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Gavel,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        "কমিউনিটি নিয়ম",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                val rules = listOf(
                                                    "সবাইকে সম্মান করুন",
                                                    "মিথ্যা তথ্য দেবেন না",
                                                    "রক্তদানে উৎসাহিত করুন",
                                                    "প্রতারণামূলক কাজ থেকে বিরত থাকুন",
                                                )
                                                rules.forEachIndexed { index, rule ->
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Text(
                                                            "${index + 1}.",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BloodRed,
                                                        )
                                                        Text(
                                                            rule,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

// ─── Member Row ──────────────────────────────────────────────────

@Composable
private fun SpondonInfoMemberRow(
    user: User,
    community: Community,
    viewModel: CommunityViewModel,
    isAdmin: Boolean,
    hideSensitiveData: Boolean = false,
    onProfileClick: () -> Unit,
) {
    val role = when {
        community.adminIds.contains(user.uid) -> CommunityRole.ADMIN
        community.moderatorIds.contains(user.uid) -> CommunityRole.MODERATOR
        else -> CommunityRole.MEMBER
    }

    val isSelf = user.uid == viewModel.fetchCurrentUserId()
    var showMenu by remember { mutableStateOf(false) }
    val isSuperAdmin = user.role == UserRole.SUPER_ADMIN

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = !isSuperAdmin, onClick = onProfileClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BloodRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                if (user.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BloodRed,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSuperAdmin) "Platform Admin 🛡️" else user.name.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (role != CommunityRole.MEMBER) {
                        Spacer(Modifier.width(6.dp))
                        RoleBadge(role = role)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (user.bloodGroup.isNotEmpty()) {
                        BloodGroupBadge(bloodGroup = user.bloodGroup)
                    }
                    if (!viewModel.shouldHideForUser(user.uid)) {
                        AvailabilityIndicator(
                            isAvailable = viewModel.isUserAvailable(user),
                            daysRemaining = viewModel.getDaysUntilAvailable(user),
                        )
                    }
                }
            }

            // Admin actions menu or simple chevron
            if (isAdmin && !isSelf && !isSuperAdmin) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Member actions",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        if (role == CommunityRole.MEMBER) {
                            DropdownMenuItem(
                                text = { Text("Make Sub-Admin") },
                                onClick = {
                                    viewModel.promoteSpondonMember(user.uid, CommunityRole.MODERATOR)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = PendingAmber,
                                    )
                                },
                            )
                        }
                        if (role == CommunityRole.MODERATOR) {
                            DropdownMenuItem(
                                text = { Text("Promote to Admin") },
                                onClick = {
                                    viewModel.promoteSpondonMember(user.uid, CommunityRole.ADMIN)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = BloodRed,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Sub-Admin") },
                                onClick = {
                                    viewModel.demoteSpondonMember(user.uid)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            onClick = {
                                onProfileClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── About Row ──────────────────────────────────────────────────

@Composable
private fun SpondonInfoAboutRow(
    icon: ImageVector,
    label: String,
    value: String,
    chipColor: Color,
    chipBg: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = chipColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = chipBg,
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = chipColor,
            )
        }
    }
}
