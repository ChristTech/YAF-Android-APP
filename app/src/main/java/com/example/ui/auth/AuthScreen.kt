package com.example.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.LanguageManager
import com.example.data.model.DepartmentType
import com.example.data.model.UserRole
import com.example.ui.components.ChurchEmblemHeader
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.KingdomEmerald
import com.example.ui.theme.ShieldSilver

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import com.example.ui.components.UserAvatar
import com.example.ui.components.persistProfilePhotoUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        department: DepartmentType,
        preferredLanguage: AppLanguage,
        profilePhotoUri: String?
    ) -> Unit,
    onToggleMode: () -> Unit,
    onQuickLogin: (String, String) -> Unit,
    onGoogleAuthSuccess: (uid: String, fullName: String, email: String, preferredLanguage: AppLanguage) -> Unit = { _, _, _, _ -> },
    onGoogleAuthError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val credentialManager = remember(context) { androidx.credentials.CredentialManager.create(context) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Register fields (streamlined for Youth Fellowship: no cell group or emergency contact)
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(UserRole.YOUTH_MEMBER) }
    var regDepartment by remember { mutableStateOf(DepartmentType.GENERAL) }
    var regPreferredLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
    var regProfilePhotoUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            regProfilePhotoUri = persistProfilePhotoUri(context, uri)
        }
    }

    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var deptDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ChurchEmblemHeader(sizeDp = 80)
            Spacer(modifier = Modifier.height(20.dp))

            // Modern Segmented Pill Tab Selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    val isSignIn = !uiState.isRegisterMode
                    Surface(
                        color = if (isSignIn) FlameRedPrimary else Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (!isSignIn) onToggleMode() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignIn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    val isRegister = uiState.isRegisterMode
                    Surface(
                        color = if (isRegister) FlameRedPrimary else Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (!isRegister) onToggleMode() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Register",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRegister) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Info or Error Alerts
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            uiState.infoMessage?.let { info ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = info,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (!uiState.isRegisterMode) {
                // LOGIN FORM
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sign in to access fellowship programmes & records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = emailOrPhone,
                            onValueChange = { emailOrPhone = it },
                            label = { Text("Email or Phone Number") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_identity_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onLogin(emailOrPhone, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                            enabled = !uiState.isLoading && !isGoogleLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                isGoogleLoading = true
                                GoogleAuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { firebaseUser ->
                                        isGoogleLoading = false
                                        onGoogleAuthSuccess(
                                            firebaseUser.uid,
                                            firebaseUser.displayName ?: "Gideonite Member",
                                            firebaseUser.email ?: "${firebaseUser.uid}@gideonites.org",
                                            regPreferredLanguage
                                        )
                                    },
                                    onAuthError = { errorMsg ->
                                        isGoogleLoading = false
                                        onGoogleAuthError(errorMsg)
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isGoogleLoading = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isGoogleLoading && !uiState.isLoading
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = FlameRedPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign in with Google (Cloud Sync)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                // REGISTRATION FORM
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Create Youth Account",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Join The Gideonites - The Youth Alive Fellowship.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // OPTIONAL PROFILE PICTURE ATTACHMENT
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_profile_photo_section")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.BottomEnd,
                                    modifier = Modifier.clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    UserAvatar(
                                        photoUri = regProfilePhotoUri,
                                        fullName = regFullName.ifBlank { "Gideonite" },
                                        sizeDp = 68
                                    )
                                    Surface(
                                        color = FlameGoldAccent,
                                        shape = CircleShape,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = "Attach Profile Picture",
                                                tint = GlobeBlack,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Profile Picture",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Optional",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (regProfilePhotoUri != null)
                                            "Picture attached and ready for your fellowship profile."
                                        else
                                            "Attach a picture so members and leaders can recognize you.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("register_attach_photo_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (regProfilePhotoUri == null) "Attach Picture" else "Change Picture",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        if (regProfilePhotoUri != null) {
                                            TextButton(
                                                onClick = { regProfilePhotoUri = null },
                                                modifier = Modifier.testTag("register_remove_photo_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove picture",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Remove",
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = regFullName,
                            onValueChange = { regFullName = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_fullname_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Email Address *") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("Phone Number *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_phone_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Create Password (min 6 chars) *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // ACCOUNT ROLE SELECTOR (Member registers freely vs Executive requires Admin approval)
                        Text(
                            text = "Select Fellowship Role *",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isMemberSelected = regRole == UserRole.YOUTH_MEMBER
                            Surface(
                                color = if (isMemberSelected) KingdomEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isMemberSelected) 1.5.dp else 1.dp,
                                    color = if (isMemberSelected) KingdomEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { regRole = UserRole.YOUTH_MEMBER }
                                    .testTag("register_role_member")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌱", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Youth Member",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isMemberSelected) KingdomEmerald else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Register freely • Instant fellowship access",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isExcoSelected = regRole == UserRole.YOUTH_EXECUTIVE
                            Surface(
                                color = if (isExcoSelected) FlameGoldAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isExcoSelected) 1.5.dp else 1.dp,
                                    color = if (isExcoSelected) FlameGoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { regRole = UserRole.YOUTH_EXECUTIVE }
                                    .testTag("register_role_executive")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⭐", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Executive",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isExcoSelected) FlameRedPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Requires Admin approval before login",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (regRole == UserRole.YOUTH_EXECUTIVE) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = FlameGoldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("executive_approval_notice")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = FlameRedPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Executive accounts require approval from the Fellowship Administrator before you can sign in.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // DEPARTMENT SELECTOR
                        ExposedDropdownMenuBox(
                            expanded = deptDropdownExpanded,
                            onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = regDepartment.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Department / Ministry Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = deptDropdownExpanded,
                                onDismissRequest = { deptDropdownExpanded = false }
                            ) {
                                DepartmentType.values().forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept.displayName) },
                                        onClick = {
                                            regDepartment = dept
                                            deptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PREFERRED LANGUAGE SELECTOR (SIGNUP DEFAULT LANGUAGE)
                        Surface(
                            color = FlameRedPrimary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FlameRedPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_language_selector_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Preferred Language (Normal App Language) *",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "This becomes your default fellowship language. You can switch to English anytime using the top bar toggle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppLanguage.values().forEach { lang ->
                                        val isSelected = regPreferredLanguage == lang
                                        androidx.compose.material3.FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                regPreferredLanguage = lang
                                                LanguageManager.setPreferredLanguage(lang)
                                            },
                                            label = {
                                                Text(
                                                    text = "${lang.displayName} (${lang.shortBadge})",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                )
                                            },
                                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = FlameRedPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("signup_lang_${lang.code}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Selected Normal Language: ${regPreferredLanguage.nativeName} • Bible: ${regPreferredLanguage.bibleTranslationCode}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FlameRedPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                onRegister(
                                    regFullName,
                                    regEmail,
                                    regPhone,
                                    regPassword,
                                    regRole,
                                    regDepartment,
                                    regPreferredLanguage,
                                    regProfilePhotoUri
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (regRole == UserRole.YOUTH_EXECUTIVE) "Submit for Admin Approval" else "Complete Registration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
