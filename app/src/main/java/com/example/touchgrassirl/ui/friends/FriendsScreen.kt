package com.example.touchgrassirl.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.repository.PendingRequestInfo
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.ui.challenges.ChallengesScreen
import com.example.touchgrassirl.ui.components.GlassCard
import com.example.touchgrassirl.ui.components.StatPill
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.SoftSage
import com.example.touchgrassirl.ui.theme.SunGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    socialRepository: SocialRepository,
    myProfileId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: FriendsViewModel = viewModel(
        factory = FriendsViewModel.Factory(socialRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddFriend by remember { mutableStateOf(false) }
    var giftFriend by remember { mutableStateOf<FriendEntity?>(null) }
    var selectedFriend by remember { mutableStateOf<FriendEntity?>(null) }
    var showGifts by remember { mutableStateOf(false) }
    var showChallenges by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${state.friends.size} friends · Tap to view stats",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (state.pendingRequests.isNotEmpty()) {
                    Text(
                        text = "Pending Requests",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.pendingRequests.forEach { request ->
                        PendingRequestCard(
                            request = request,
                            onAccept = { viewModel.acceptRequest(request.uid) },
                            onDecline = { viewModel.declineRequest(request.uid) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (state.friends.isEmpty()) {
                    EmptyFriendsState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            GiftsReceivedCard(
                                socialRepository = socialRepository,
                                onClick = { showGifts = true },
                            )
                        }
                        item {
                            ChallengesCard(
                                onClick = { showChallenges = true },
                            )
                        }
                        items(state.friends, key = { it.profileId }) { friend ->
                            FriendCard(
                                friend = friend,
                                onGift = { giftFriend = friend },
                                onClick = { selectedFriend = friend },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddFriend = true },
            containerColor = ForestGreen,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add friend")
        }

        if (showAddFriend) {
            ModalBottomSheet(
                onDismissRequest = { showAddFriend = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                AddFriendSheet(
                    onAddFriend = { profileId ->
                        viewModel.sendFriendRequest(profileId)
                        showAddFriend = false
                    },
                )
            }
        }

        giftFriend?.let { friend ->
            GiftSheet(
                friend = friend,
                socialRepository = socialRepository,
                onDismiss = { giftFriend = null },
                onGiftSent = { giftFriend = null },
            )
        }

        selectedFriend?.let { friend ->
            FriendDetailScreen(
                friend = friend,
                socialRepository = socialRepository,
                onBack = { selectedFriend = null },
                onFriendRemoved = { selectedFriend = null },
            )
        }

        if (showGifts) {
            GiftsReceivedScreen(
                socialRepository = socialRepository,
                onBack = { showGifts = false },
            )
        }

        if (showChallenges) {
            ChallengesScreen(
                socialRepository = socialRepository,
                onBack = { showChallenges = false },
            )
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendEntity,
    modifier: Modifier = Modifier,
    onGift: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SoftSage, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${friend.totalOutdoorMinutes}m · ${friend.currentStreak}d streak · Lv ${friend.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onGift) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = "Send gift",
                    tint = SunGold,
                )
            }
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: PendingRequestInfo,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        backgroundColor = SunGold.copy(alpha = 0.08f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SunGold.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Wants to be friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onAccept) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Accept",
                    tint = ForestGreen,
                )
            }
            IconButton(onClick = onDecline) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Decline",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddFriendSheet(
    onAddFriend: (String) -> Unit,
) {
    var profileId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = "Add Friend",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your friend's profile ID to send a friend request.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = profileId,
            onValueChange = { profileId = it.uppercase() },
            label = { Text("Profile ID (e.g. GRASS-A7X9K2)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onAddFriend(profileId.trim()) },
            enabled = profileId.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Request", modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyFriendsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "\uD83D\uDC65",
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No friends yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap + to add a friend by their profile ID",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GiftsReceivedCard(
    socialRepository: SocialRepository,
    onClick: () -> Unit,
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDF81",
                fontSize = 24.sp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gifts Received",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "View gifts from friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View gifts",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun ChallengesCard(
    onClick: () -> Unit,
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDFC6",
                fontSize = 24.sp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Challenges",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Compete with friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View challenges",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}
