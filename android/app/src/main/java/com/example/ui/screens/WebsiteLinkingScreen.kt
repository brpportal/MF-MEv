package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.RegistrationStatus
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.LinkingViewModel

@Composable
fun WebsiteLinkingScreen(
    status: RegistrationStatus,
    linkingViewModel: LinkingViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onPairingSubmit: (String) -> Unit = { linkingViewModel.pairApp(it) }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val pairingError by linkingViewModel.pairingError.collectAsState()
    val isPairing by linkingViewModel.isPairing.collectAsState()

    var emailInput by remember { mutableStateOf(currentUser?.email ?: "") }
    var pairingCode by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Navy900),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Connect to MF MPanel",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Verify your Officer Email and 6-digit pairing code with the Supabase backend to activate this device.",
                fontSize = 13.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(8.dp))

            when (status) {
                RegistrationStatus.CHECKING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = BluePrimary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Checking verification status with Supabase...", fontSize = 13.sp, color = Slate600)
                }

                RegistrationStatus.WAITING_CONFIRMATION -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = BluePrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Waiting for Admin Approval",
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your device has been paired. Please confirm activation in the MF MPanel web portal.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                RegistrationStatus.ACCESS_REVOKED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Access Revoked",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "This device has been deactivated by the administrator.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // If currentUser email is empty or needs confirmation
                            if (currentUser == null || currentUser?.email.isNullOrBlank()) {
                                Text(
                                    text = "OFFICER EMAIL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = {
                                        emailInput = it
                                        linkingViewModel.clearError()
                                    },
                                    placeholder = { Text("officer@agency.gov", color = Slate400) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Slate500)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Slate50,
                                        unfocusedContainerColor = Slate50,
                                        focusedTextColor = Navy900,
                                        unfocusedTextColor = Navy900
                                    )
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "6-DIGIT PAIRING CODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Navy900
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .clickable {
                                            val clip = clipboardManager.getText()?.text.orEmpty()
                                            val digits = clip.filter { it in '0'..'9' }.take(6)
                                            if (digits.isNotBlank()) {
                                                pairingCode = digits
                                                linkingViewModel.clearError()
                                                Toast.makeText(context, "Pasted $digits", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Paste Code",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 6 Separate boxes for pairing code
                            SixDigitCodeInput(
                                code = pairingCode,
                                onCodeChange = {
                                    pairingCode = it
                                    linkingViewModel.clearError()
                                },
                                focusRequester = focusRequester,
                                onCompleted = { fullCode ->
                                    val targetEmail = emailInput.ifBlank { currentUser?.email.orEmpty() }
                                    linkingViewModel.pairApp(fullCode, targetEmail)
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            AnimatedVisibility(visible = pairingError != null) {
                                pairingError?.let { err ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Error",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = err,
                                                color = Color(0xFF991B1B),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val targetEmail = emailInput.ifBlank { currentUser?.email.orEmpty() }
                                    if (pairingCode.trim().length == 6) {
                                        linkingViewModel.pairApp(pairingCode.trim(), targetEmail)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("verify_pairing_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                enabled = pairingCode.trim().length == 6 && !isPairing
                            ) {
                                if (isPairing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying with Supabase...")
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verify & Link Device", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { authViewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("linking_sign_out_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out Officer", fontSize = 13.sp, color = Slate600)
            }
        }
    }

}
