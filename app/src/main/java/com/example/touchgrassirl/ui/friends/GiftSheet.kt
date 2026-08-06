package com.example.touchgrassirl.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.SoftSage
import com.example.touchgrassirl.ui.theme.SunGold

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GiftSheet(
    friend: FriendEntity,
    socialRepository: SocialRepository,
    onDismiss: () -> Unit,
    onGiftSent: () -> Unit,
) {
    var selectedGift by remember { mutableStateOf<Pair<String, String>?>(null) }
    var message by remember { mutableStateOf("") }
    var showConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        if (showConfirmation) {
            GiftConfirmation(
                giftName = selectedGift?.second ?: "",
                friendName = friend.displayName,
                onDone = {
                    showConfirmation = false
                    onGiftSent()
                    onDismiss()
                },
            )
        } else {
            GiftPicker(
                friend = friend,
                selectedGift = selectedGift,
                message = message,
                socialRepository = socialRepository,
                onSelectGift = { selectedGift = it },
                onMessageChange = { message = it },
                onSend = {
                    showConfirmation = true
                },
            )
        }
    }
}

@Composable
private fun GiftPicker(
    friend: FriendEntity,
    selectedGift: Pair<String, String>?,
    message: String,
    socialRepository: SocialRepository,
    onSelectGift: (Pair<String, String>) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = "Send a gift",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Send something special to ${friend.displayName}!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(SocialRepository.GIFT_TYPES) { (type, label) ->
                GiftOption(
                    label = label,
                    isSelected = selectedGift?.first == type,
                    onClick = { onSelectGift(type to label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Add a message (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                selectedGift?.let { (giftType, _) ->
                    scope.launch {
                        socialRepository.sendGift(
                            toProfileId = friend.profileId,
                            giftType = giftType,
                            message = message,
                        )
                        onSend()
                    }
                }
            },
            enabled = selectedGift != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Send Gift", modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GiftOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ForestGreen else SoftSage.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun GiftConfirmation(
    giftName: String,
    friendName: String,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SunGold.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\uD83C\uDF81", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gift Sent!",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$giftName sent to $friendName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Done", modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
