package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LinkingRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

enum class AuthStep {
    EMAIL,
    PASSCODE
}

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        delay(1000)
        val linkingRepo = LinkingRepository(context)
        val isActivated = linkingRepo.isActivated()
        if (currentUser != null && isActivated) {
            onNavigateToDashboard()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, Color(0xFF5B21B6))
                        )
                    )
                    .shadow(16.dp, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "MF MPanel Evidence",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Prominent "Powered by Al Mudafioon" badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PurpleLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Powered by Al Mudafioon",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurpleLight
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = PurpleLight,
                strokeWidth = 3.dp
            )
        }
    }
}

/**
 * Single-Prompt 2-Step Login & Passcode Verification Screen
 * Displays "Powered by Al Mudafioon" prominently.
 * Prompts: "Enter your email address" -> "Continue" -> "Enter 6-digit passcode" -> Access Application
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var currentStep by remember { mutableStateOf(AuthStep.EMAIL) }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var codeValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    // If user is already authenticated and activated, navigate directly
    LaunchedEffect(currentUser) {
        if (currentUser != null && LinkingRepository(context).isActivated()) {
            onLoginSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo Box
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, Color(0xFF4C1D95))
                        )
                    )
                    .shadow(12.dp, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Prominent "Powered by Al Mudafioon" badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.6f)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PurpleLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Powered by Al Mudafioon",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurpleLight
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MF MPanel Evidence",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (currentStep == AuthStep.EMAIL)
                    "Enter your email address to get started"
                else
                    "Enter your 6-digit passcode to access the workspace",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // In-place animated transition between Step 1 (Email) and Step 2 (Passcode)
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState == AuthStep.PASSCODE) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "auth_step_transition"
            ) { step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        when (step) {
                            AuthStep.EMAIL -> {
                                // Step 1: Email Address
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STEP 1 OF 2",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = PurpleLight
                                    )
                                    Text(
                                        text = "Identification",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Enter your email address",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        emailError = null
                                        authViewModel.clearError()
                                    },
                                    placeholder = {
                                        Text("officer@agency.gov", color = Color(0xFF64748B), fontSize = 14.sp)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = PurpleLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            val trimmed = email.trim()
                                            if (trimmed.isNotBlank() && trimmed.contains("@") && trimmed.contains(".")) {
                                                currentStep = AuthStep.PASSCODE
                                            } else {
                                                emailError = "Please enter a valid email address"
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant,
                                        focusedIndicatorColor = PurplePrimary,
                                        unfocusedIndicatorColor = DarkBorder
                                    )
                                )

                                emailError?.let { err ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = err,
                                        color = Color(0xFFF87171),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(22.dp))

                                val isEmailValid = email.trim().contains("@") && email.trim().contains(".")

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        val trimmed = email.trim()
                                        if (trimmed.isNotBlank() && trimmed.contains("@") && trimmed.contains(".")) {
                                            emailError = null
                                            currentStep = AuthStep.PASSCODE
                                        } else {
                                            emailError = "Please enter a valid email address"
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("continue_email_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PurplePrimary,
                                        disabledContainerColor = DarkSurfaceVariant
                                    ),
                                    enabled = isEmailValid
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Continue",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            AuthStep.PASSCODE -> {
                                // Step 2: 6-Digit Code / Password
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            currentStep = AuthStep.EMAIL
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = PurpleLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "STEP 2 OF 2",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = PurpleLight
                                        )
                                    }

                                    TextButton(
                                        onClick = { currentStep = AuthStep.EMAIL },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "Change Email",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate400
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Enter 6-digit passcode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Code for ${email.trim()}",
                                    fontSize = 12.sp,
                                    color = PurpleLight
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PASSCODE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = Slate400
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceVariant)
                                            .clickable {
                                                val clipText = clipboardManager.getText()?.text.orEmpty()
                                                val digits = clipText.filter { it.isDigit() }.take(6)
                                                if (digits.isNotBlank()) {
                                                    codeValue = TextFieldValue(digits, TextRange(digits.length))
                                                    authViewModel.clearError()
                                                    Toast.makeText(context, "Pasted $digits", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "No 6-digit code in clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = PurpleLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Paste Code",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleLight
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 6 Boxes Input
                                SixDigitCodeInput(
                                    code = codeValue.text,
                                    onCodeChange = { newDigits ->
                                        codeValue = TextFieldValue(newDigits, TextRange(newDigits.length))
                                        authViewModel.clearError()
                                    },
                                    focusRequester = focusRequester,
                                    onCompleted = { fullCode ->
                                        focusManager.clearFocus()
                                        authViewModel.verifyAndAccessWithPairingCode(
                                            email = email.trim(),
                                            code = fullCode
                                        ) {
                                            onLoginSuccess()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                AnimatedVisibility(
                                    visible = errorMessage != null,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    errorMessage?.let { error ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                                            border = BorderStroke(1.dp, Color(0xFF7F1D1D))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF87171),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = error,
                                                    color = Color(0xFFFECACA),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val isCodeReady = codeValue.text.length == 6

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        authViewModel.verifyAndAccessWithPairingCode(
                                            email = email.trim(),
                                            code = codeValue.text.trim()
                                        ) {
                                            onLoginSuccess()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("verify_and_access_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PurplePrimary,
                                        disabledContainerColor = DarkSurfaceVariant
                                    ),
                                    enabled = isCodeReady && !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verifying...", fontWeight = FontWeight.Bold, color = Color.White)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Continue",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Web Portal Link (for new users or registration)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mf-panel.vercel.app/"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Web Portal",
                    tint = PurpleLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign up or manage keys at mf-panel.vercel.app",
                    fontSize = 12.sp,
                    color = PurpleLight,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "By continuing, you certify that you agree to the Terms of Service and Privacy Policy. Powered by Al Mudafioon.",
                fontSize = 11.sp,
                color = Slate400,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 6 Separate Modern Input Boxes for 6-Digit Pairing / Passcode.
 */
@Composable
fun SixDigitCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onCompleted: (String) -> Unit = {}
) {
    val cleanCode = code.filter { it.isDigit() }.take(6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
            },
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = cleanCode,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(6)
                onCodeChange(digitsOnly)
                if (digitsOnly.length == 6) {
                    onCompleted(digitsOnly)
                }
            },
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (cleanCode.length == 6) {
                        onCompleted(cleanCode)
                    }
                }
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until 6) {
                val char = cleanCode.getOrNull(index)?.toString() ?: ""
                val isFocused = index == cleanCode.length && cleanCode.length < 6
                val isFilled = index < cleanCode.length

                SingleCodeBox(
                    digit = char,
                    isFocused = isFocused,
                    isFilled = isFilled,
                    onClick = { focusRequester.requestFocus() }
                )
            }
        }
    }
}

@Composable
private fun SingleCodeBox(
    digit: String,
    isFocused: Boolean,
    isFilled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isFocused -> PurpleLight
        isFilled -> PurplePrimary
        else -> DarkBorder
    }

    val borderWidth = if (isFocused) 2.5.dp else if (isFilled) 2.dp else 1.5.dp

    val containerColor = when {
        isFocused -> Color(0xFF1E1B4B)
        isFilled -> DarkSurfaceVariant
        else -> DarkSurface
    }

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                BorderStroke(borderWidth, borderColor),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (digit.isNotEmpty()) {
            Text(
                text = digit,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        } else if (isFocused) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PurpleLight)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF475569))
            )
        }
    }
}
