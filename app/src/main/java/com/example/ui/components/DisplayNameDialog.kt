package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayNameDialog(
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Force user to set display name on first launch */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(3.dp, DuoGreen),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon or Logo
                Text("🏆", fontSize = 48.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Welcome to FormFit!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = DuoInk,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter your display name to compete on the live online leaderboard.",
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = DuoInkMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("e.g. Alex Trainer", color = DuoInkMuted) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = DuoInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DuoInk,
                        unfocusedTextColor = DuoInk,
                        focusedContainerColor = DuoSurface1,
                        unfocusedContainerColor = DuoSurface1,
                        focusedBorderColor = DuoGreen,
                        unfocusedBorderColor = DuoBorder,
                        focusedLabelColor = DuoGreen,
                        unfocusedLabelColor = DuoInkMuted,
                        cursorColor = DuoGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input")
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                DuoButton(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.length < 2) {
                            errorMessage = "Name must be at least 2 characters long."
                        } else if (trimmed.length > 20) {
                            errorMessage = "Name must be 20 characters or less."
                        } else {
                            onConfirm(trimmed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "join_leaderboard_button"
                ) {
                    Text(
                        text = "START TRAINING 🚀",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
