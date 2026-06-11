package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CommentEntity
import com.example.data.ImageEntity
import com.example.viewmodel.GalleryViewModel
import com.example.viewmodel.UserSession
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryApp(viewModel: GalleryViewModel) {
    val context = LocalContext.current
    var currentRoute by remember { mutableStateOf("welcome") } // welcome, home, detail, profile, admin
    val darkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val session by viewModel.currentUserSession.collectAsStateWithLifecycle()

    // Listen to ViewModel Toasts securely
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Force redirect to welcome if logged out
    LaunchedEffect(session) {
        if (session == null && currentRoute != "welcome") {
            currentRoute = "welcome"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute != "welcome") {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "home" || currentRoute == "detail",
                            onClick = { currentRoute = "home" },
                            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery Feed") },
                            label = { Text("Explore", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_explore")
                        )
                        NavigationBarItem(
                            selected = currentRoute == "profile",
                            onClick = { currentRoute = "profile" },
                            icon = { Icon(Icons.Default.Person, contentDescription = "My Profile") },
                            label = { Text("Profile", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_profile")
                        )
                        if (session?.role == "admin") {
                            NavigationBarItem(
                                selected = currentRoute == "admin",
                                onClick = { currentRoute = "admin" },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Area") },
                                label = { Text("Admin", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_admin")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    "welcome" -> WelcomeScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { currentRoute = "home" }
                    )
                    "home" -> GalleryHomeScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { currentRoute = "detail" },
                        onNavigateToProfile = { currentRoute = "profile" }
                    )
                    "detail" -> ImageDetailScreen(
                        viewModel = viewModel,
                        onBack = { currentRoute = "home" }
                    )
                    "profile" -> UserProfileScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { currentRoute = "detail" }
                    )
                    "admin" -> AdminPanelScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { currentRoute = "detail" }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. WELCOME SCREEN (Auth, Signup, Google & Guest Mode)
// ==========================================
@Composable
fun WelcomeScreen(
    viewModel: GalleryViewModel,
    onLoginSuccess: () -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showAdminPassDialog by remember { mutableStateOf(false) }
    var adminPassCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Decorative Logo Accent
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "HD Gallery Hub Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "HD Gallery Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Uncompressed High-Definition Photo Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Tab Switcher between Login & Sign Up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { isSignUpMode = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_login"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                            contentColor = if (!isSignUpMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        elevation = null,
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("LOGIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    Button(
                        onClick = { isSignUpMode = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_signup"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                            contentColor = if (isSignUpMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        elevation = null,
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("SIGN UP", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isSignUpMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_signup_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (isSignUpMode) {
                            viewModel.register(name, email, password) { ok ->
                                if (ok) onLoginSuccess()
                            }
                        } else {
                            viewModel.login(email, password) { ok ->
                                if (ok) onLoginSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_auth_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isSignUpMode) "CREATE SECURE ACCOUNT" else "SECURE SIGN IN",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    Text("  OR  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                }

                // Google Sign In Mock Button
                Button(
                    onClick = {
                        // Mock Google Sign-In with predefined user
                        viewModel.loginWithGoogleMock("user@gallery.com", "Tapan Mondal")
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_signin_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        // Google account connection display
                        Text("Connect with Google ID", fontWeight = FontWeight.Bold)
                    }
                }

                // Guest Mode Connection
                OutlinedButton(
                    onClick = {
                        viewModel.loginAsGuest()
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("guest_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter as Guest Mode", fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Developer/Admin Hidden Entry Triggers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { showAdminPassDialog = true },
                        modifier = Modifier.testTag("secure_admin_lock")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Hidden Admin Authorization Key",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Admin Passcode authorization Dialog
    if (showAdminPassDialog) {
        Dialog(onDismissRequest = { showAdminPassDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Developer Console Link", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        "Input your confidential Developer authorization bypass key code to access registered dashboard logs.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = adminPassCodeInput,
                        onValueChange = { adminPassCodeInput = it },
                        label = { Text("Administrative Code") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_code_input"),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAdminPassDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (adminPassCodeInput == viewModel.predefinedAdminCode) {
                                    // Log in as preconfigured Admin account
                                    viewModel.loginWithGoogleMock("admin@gallery.com", "Admin Pro")
                                    showAdminPassDialog = false
                                    onLoginSuccess()
                                } else {
                                    viewModel.showToast("Bypass denied! Incorrect Authorization Code.")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_code_submit")
                        ) {
                            Text("Authorize")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. DASHBOARD / FEED SCREEN (Pinterest dual columns)
// ==========================================
@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel,
    onNavigateToDetail: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val images by viewModel.filteredImages.collectAsStateWithLifecycle()
    val categorySelected by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val session by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showUploadDialog by remember { mutableStateOf(false) }

    // Predefined Category Listing
    val categories = listOf("All", "Nature", "Animals", "Cars", "Gaming", "Technology", "Sports", "Wallpapers", "Art")

    Scaffold(
        floatingActionButton = {
            if (session != null && !session!!.email.startsWith("guest_")) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    modifier = Modifier.testTag("fab_upload"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Upload Photo")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant Hub Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HD Gallery Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            // Hidden Admin Activation key by clicking logo
                            viewModel.loginWithGoogleMock("admin@gallery.com", "Admin Pro")
                        }
                    )
                    Text(
                        text = "Pixel Perfect, High Fidelity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Interactive session indicators
                TextButton(
                    onClick = { onNavigateToProfile() },
                    modifier = Modifier.testTag("header_profile_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session?.name ?: "Guest Session",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search input field
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_field"),
                placeholder = { Text("Search tags, categories, or keywords...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Seek")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Horizontal pill category selector
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = categorySelected == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategory.value = category },
                        label = { Text(category, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("tag_pill_$category")
                    )
                }
            }

            // Empty State
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No photographic results found",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Try seeking empty search keywords or select alternative category hubs like Wallpapers, Cars, or Art.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                        )
                    }
                }
            } else {
                // Pinterest Dual-Column Masonry structure
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val col1 = images.filterIndexed { idx, _ -> idx % 2 == 0 }
                        val col2 = images.filterIndexed { idx, _ -> idx % 2 != 0 }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            col1.forEach { item ->
                                PhotoMasonryCard(item, viewModel, onNavigateToDetail)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            col2.forEach { item ->
                                PhotoMasonryCard(item, viewModel, onNavigateToDetail)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp)) // Extra padding underneath for comfortable FAB clearance
                }
            }
        }
    }

    // Custom upload photo Dialog
    if (showUploadDialog) {
        var upTitle by remember { mutableStateOf("") }
        var upDesc by remember { mutableStateOf("") }
        var upUrl by remember { mutableStateOf("") }
        var upCategory by remember { mutableStateOf("Nature") }
        var upTags by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showUploadDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .shadow(12.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Publish HD Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Paste an Uncompressed direct web link. We fetch original quality metadata safely.", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)

                    OutlinedTextField(
                        value = upTitle,
                        onValueChange = { upTitle = it },
                        label = { Text("Photo Title *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_title"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upDesc,
                        onValueChange = { upDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = upUrl,
                        onValueChange = { upUrl = it },
                        label = { Text("Image URL *") },
                        placeholder = { Text("https://images.unsplash.com/...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_url"),
                        singleLine = true
                    )

                    // Autocomplete or selection presets
                    Text("Preset Mock URLs (Tap to select):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(
                            "Unsplash 1" to "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?q=80&w=1200",
                            "Unsplash 2" to "https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=1200",
                            "Unsplash 3" to "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=1200"
                        )
                        presets.forEach { pair ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { upUrl = pair.second }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(pair.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    // Category dropdown row (simulated for simplicity)
                    Text("Select Category *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presetCategories = categories.filter { it != "All" }
                        items(presetCategories) { cat ->
                            val isChosen = upCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                    .clickable { upCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(cat, fontWeight = FontWeight.Bold, color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = upTags,
                        onValueChange = { upTags = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("hd, wallpapers, nature") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUploadDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                if (upTitle.isNotBlank() && upUrl.isNotBlank()) {
                                    viewModel.uploadNewPhoto(upTitle, upDesc, upUrl, upCategory, upTags)
                                    showUploadDialog = false
                                } else {
                                    viewModel.showToast("Required details are missing!")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_submit_btn")
                        ) {
                            Text("Publish Now")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PINTEREST STYLE REUSABLE PHOTO CARD
// ==========================================
@Composable
fun PhotoMasonryCard(
    image: ImageEntity,
    viewModel: GalleryViewModel,
    onNavigateToDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                viewModel.activeDetailImage.value = image
                viewModel.checkUserInteractivity(image.id)
                viewModel.loadComments(image.id)
                onNavigateToDetail()
            }
            .testTag("photo_card_${image.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            // Uncompressed aspect loaded smoothly
            AsyncImage(
                model = image.imageUrl,
                contentDescription = image.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 320.dp)
            )

            // Category tag floating badge top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = image.category,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stats gradient overlay bottom row
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = image.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "by ${image.authorName}",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                            Text(" ${image.likeCount}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. INTERACTIVE HIGH-RES DETAIL VIEW (Pinch Zoom, Comments Feed)
// ==========================================
@Composable
fun ImageDetailScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val image by viewModel.activeDetailImage.collectAsStateWithLifecycle()
    val comments by viewModel.activeImageComments.collectAsStateWithLifecycle()
    val isLiked by viewModel.isLikedByCurrentUser.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSavedByCurrentUser.collectAsStateWithLifecycle()
    val session by viewModel.currentUserSession.collectAsStateWithLifecycle()

    var commentText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Pinch-to-zoom reactive variables
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (image == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High polish header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Exit Detailed view")
            }
            Text(
                text = image!!.title,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            // Admin Panel quick moderator tool
            if (session?.role == "admin") {
                IconButton(
                    onClick = {
                        viewModel.adminRemoveInappropriateImage(image!!.id)
                        onBack()
                    },
                    modifier = Modifier.testTag("admin_delete_photo")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Inappropriate Mark", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = { viewModel.toggleSave(image!!.id) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save photo",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Main detailed layout with interactive image and comments scroll
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Interactive Pinch to Zoom box window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = image!!.imageUrl,
                    contentDescription = image!!.description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                // Dynamic Pinch Helper Badge Overlay
                if (scale > 1f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                scale = 1f
                                offset = Offset.Zero
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Reset Zoom (${"%.1f".format(scale)}x)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "Pinch to Zoom",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    )
                }
            }

            // Stats Actions Row Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes Section icon
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .clickable { viewModel.toggleLike(image!!.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like counter",
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${image!!.likeCount} Likes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Downloads triggers
                Button(
                    onClick = { viewModel.downloadImage(image!!) },
                    modifier = Modifier.testTag("download_action_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Original Quality Download")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Original (${image!!.pixelSize})")
                }

                // Sharing intent link
                IconButton(
                    onClick = { viewModel.shareImage(context, image!!) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // High polish informational details card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Publisher Studio", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(image!!.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Category Cluster", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(image!!.category, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    if (image!!.description.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        Text("Story behind the lens", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text(image!!.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                    // Raw specifications metadata grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Original Pixel Grid", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(image!!.pixelSize, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Uncompressed Weight", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(image!!.fileSize, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Historic Downloads", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text("${image!!.downloadCount} downloads", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Tags section chips
            if (image!!.tags.isNotBlank()) {
                Text(
                    "Search keywords mapping",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val parts = image!!.tags.split(",")
                    items(parts) { part ->
                        if (part.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("#$part", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // Comments Feed Section
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Conversations (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Comments card stack list
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Be the first to share details!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comments.forEach { comment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(viewModel.formatTimestamp(comment.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    if (session != null && (session!!.email == comment.authorEmail || session!!.role == "admin")) {
                                        IconButton(
                                            onClick = { viewModel.deleteComment(comment.id, image!!.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Purge Comment", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(comment.commentText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Posting a new comment
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write interactive comment...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.postComment(image!!.id, commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("comment_submit"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Publish Comment")
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ==========================================
// 4. USER PROFILE SCREEN (Dynamic Theme switcher, Saved stats grids)
// ==========================================
@Composable
fun UserProfileScreen(
    viewModel: GalleryViewModel,
    onNavigateToDetail: () -> Unit
) {
    val session by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val myUploads by viewModel.getUploadedImages().collectAsStateWithLifecycle(emptyList<ImageEntity>())
    val mySaves by viewModel.getSavedImages().collectAsStateWithLifecycle(emptyList<ImageEntity>())

    var showingBookmarksTab by remember { mutableStateOf(false) }

    if (session == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Upper decorative header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, Color.Transparent)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User Avatar initials
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (session!!.name.isNotBlank()) session!!.name.take(2).uppercase() else "HD"
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = session!!.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (session!!.email.startsWith("guest_")) "Guest Connection Mode" else session!!.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                if (session!!.role == "admin") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("SYSTEM ADMINISTRATOR", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Preference styling items list
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Customize Interface Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isDarkTheme) "Deep Charcoal Dark Mode" else "Clean Slate Light Theme",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.isDarkTheme.value = it },
                        modifier = Modifier.testTag("theme_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                // Log out action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.logout() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Terminate Active Account Session", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Tab selection switcher between my uploads and my bookmarks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Button(
                onClick = { showingBookmarksTab = false },
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_tab_uploads"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showingBookmarksTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!showingBookmarksTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("My Uploads (${myUploads.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { showingBookmarksTab = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_tab_saves"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showingBookmarksTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (showingBookmarksTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("Saved Photos (${mySaves.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        val displayList = if (showingBookmarksTab) mySaves else myUploads

        // Staggered grid representation list for profile
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (showingBookmarksTab) Icons.Default.BookmarkBorder else Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showingBookmarksTab) "Zero bookmarks saved yet." else "You haven't uploaded images.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            // Unsplash Pinterest-like grid for profile items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val col1 = displayList.filterIndexed { idx, _ -> idx % 2 == 0 }
                val col2 = displayList.filterIndexed { idx, _ -> idx % 2 != 0 }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    col1.forEach { item ->
                        PhotoMasonryCard(item, viewModel, onNavigateToDetail)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    col2.forEach { item ->
                        PhotoMasonryCard(item, viewModel, onNavigateToDetail)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// ==========================================
// 5. SYSTEM ADMINISTRATOR DECK PANEL SCREEN (Total Users, Suspend tools, Analytics)
// ==========================================
@Composable
fun AdminPanelScreen(
    viewModel: GalleryViewModel,
    onNavigateToDetail: () -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val stats by viewModel.adminDashboardStats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High polish admin titles
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Admin Hub Access",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "System moderation, user accounts and telemetry database logs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        // TELEMETRY METRIC DASHBOARD CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Analytics Status Registry",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total users Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Users", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${stats.first} Users", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Total uploads Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Photo Uploads", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${stats.second} HD Pics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Total downloads Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HD Downloads Meter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${stats.third} DLs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // USER ACCOUNT CONTROL AND MODERATION DATABASE
        Text(
            text = "User Accounts Database (${users.size} profiles)",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            users.forEach { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    if (user.role == "admin") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Admin", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }

                            // Suspended toggle state badge
                            if (user.isSuspended) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Suspended", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))

                        // Stats counters row for this user
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Sign-up Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(viewModel.formatTimestamp(user.signupDate), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Published Count", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("${user.uploadCount} posts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Downloads Stat", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("${user.downloadCount} files", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Management action buttons (Suspend, Purge account)
                        if (user.role != "admin") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Suspension switcher
                                TextButton(
                                    onClick = { viewModel.adminSuspendUser(user.email, !user.isSuspended) },
                                    modifier = Modifier.testTag("admin_suspend_${user.email}")
                                ) {
                                    Text(
                                        text = if (user.isSuspended) "Uplift Suspension" else "Suspend Account",
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.isSuspended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Purge permanent account
                                OutlinedButton(
                                    onClick = { viewModel.adminDeleteUser(user.email) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("admin_delete_user_${user.email}")
                                ) {
                                    Text("Delete Account", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Option removed — direct fillMaxWidth is utilized
