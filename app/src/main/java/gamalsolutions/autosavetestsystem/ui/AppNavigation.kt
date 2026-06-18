package gamalsolutions.autosavetestsystem.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gamalsolutions.autosavetestsystem.R
import gamalsolutions.autosavetestsystem.database.LogEntry
import gamalsolutions.autosavetestsystem.database.SavedContact
import gamalsolutions.autosavetestsystem.utils.PhoneNumberUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    // Observe active settings to dynamically bind states
    val settingsState by viewModel.systemSettings.collectAsStateWithLifecycle()

    val showBlockOverlay by viewModel.showDeveloperContactBlock.collectAsStateWithLifecycle()
    val totalContacts by viewModel.totalContactsCount.collectAsStateWithLifecycle()

    LaunchedEffect(totalContacts) {
        if (totalContacts >= 100) {
            viewModel.triggerDeveloperBlock()
        }
    }

    // Enforce Right-To-Left (RTL) Layout completely for perfect Arabic rendering
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showBlockOverlay && totalContacts >= 100) {
            DeveloperBlockDialog(
                onDismiss = { viewModel.dismissDeveloperBlock() }
            )
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Header of the Drawer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(20.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.drawer_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation items inside the sidebar
                    DrawerItem(
                        title = stringResource(R.string.nav_home),
                        icon = Icons.Default.Dashboard,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            navController.navigate("dashboard") { launchSingleTop = true }
                        }
                    )
                    DrawerItem(
                        title = stringResource(R.string.nav_contacts),
                        icon = Icons.Default.Contacts,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            navController.navigate("contacts")
                        }
                    )
                    DrawerItem(
                        title = stringResource(R.string.nav_logs),
                        icon = Icons.Default.ReceiptLong,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            navController.navigate("logs")
                        }
                    )
                    DrawerItem(
                        title = stringResource(R.string.nav_export),
                        icon = Icons.Default.Share,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            navController.navigate("export")
                        }
                    )
                    DrawerItem(
                        title = stringResource(R.string.nav_settings),
                        icon = Icons.Default.Settings,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            if (totalContacts >= 100) {
                                viewModel.triggerDeveloperBlock()
                            } else {
                                navController.navigate("settings")
                            }
                        }
                    )
                    DrawerItem(
                        title = stringResource(R.string.nav_permissions),
                        icon = Icons.Default.FactCheck,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            navController.navigate("permissions")
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Secondary info buttons
                    var showPrivacyDialog by remember { mutableStateOf(false) }
                    var showDeveloperDialog by remember { mutableStateOf(false) }

                    DrawerItem(
                        title = stringResource(R.string.nav_privacy),
                        icon = Icons.Default.PrivacyTip,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            showPrivacyDialog = true
                        }
                    )
                    DrawerItem(
                        title = stringResource(R.string.nav_developer),
                        icon = Icons.Default.SupportAgent,
                        onClicked = {
                            scope.launch { drawerState.close() }
                            showDeveloperDialog = true
                        }
                    )

                    Text(
                        text = "${stringResource(R.string.app_version_label)}: ${stringResource(R.string.app_version_value)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    // Dialog rendering
                    if (showPrivacyDialog) {
                        AlertDialog(
                            onDismissRequest = { showPrivacyDialog = false },
                            confirmButton = {
                                Button(onClick = { showPrivacyDialog = false }) {
                                    Text(stringResource(R.string.btn_okay))
                                }
                            },
                            title = { Text(stringResource(R.string.privacy_policy_title), fontWeight = FontWeight.Bold) },
                            text = { Text(stringResource(R.string.privacy_policy_text), fontSize = 15.sp) }
                        )
                    }

                    if (showDeveloperDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeveloperDialog = false },
                            confirmButton = {
                                Button(onClick = { showDeveloperDialog = false }) {
                                    Text(stringResource(R.string.btn_okay))
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = {
                                    val devEmail = "gamalalmaqtary6838@gmail.com"
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(devEmail))
                                        putExtra(Intent.EXTRA_SUBJECT, "رغبة دعم واستفسار / AutoSave CRM")
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "لم نجد تطبيق بريد ملائم لإرسال الرسالة", Toast.LENGTH_SHORT).show()
                                    }
                                    showDeveloperDialog = false
                                }) {
                                    Text(stringResource(R.string.dev_contact_btn))
                                }
                            },
                            title = { Text(stringResource(R.string.nav_developer), fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text(stringResource(R.string.dev_desc), fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(stringResource(R.string.dev_feedback_desc), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("المطور: gamalalmaqtary6838@gmail.com", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }
        ) {
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(navController)
                }
                composable("dashboard") {
                    DashboardScreen(viewModel, navController) {
                        scope.launch { drawerState.open() }
                    }
                }
                composable("contacts") {
                    ContactsScreen(viewModel) {
                        scope.launch { drawerState.open() }
                    }
                }
                composable("logs") {
                    LogsScreen(viewModel) {
                        scope.launch { drawerState.open() }
                    }
                }
                composable("settings") {
                    SettingsScreen(viewModel) {
                        scope.launch { drawerState.open() }
                    }
                }
                composable("export") {
                    ExportScreen(viewModel) {
                        scope.launch { drawerState.open() }
                    }
                }
                composable("permissions") {
                    PermissionsScreen(viewModel) {
                        scope.launch { drawerState.open() }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(title: String, icon: ImageVector, onClicked: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(title, fontWeight = FontWeight.Medium) },
        selected = false,
        onClick = onClicked,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

// --- SCREEN 1: SPLASH SCREEN ---
@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AutoMode,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AutoSave test system",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "بناء قاعدة بيانات العملاء للـ CRM تلقائيًا",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }

    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("dashboard") {
            popUpTo("splash") { inclusive = true }
        }
    }
}

// --- SCREEN 2: HOME DASHBOARD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController, onOpenDrawer: () -> Unit) {
    val totalContacts by viewModel.totalContactsCount.collectAsStateWithLifecycle()
    val lastContact by viewModel.lastSavedContact.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsMap.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshStatusStates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatusStates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Heartbeat status card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isServiceActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.service_status),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isServiceActive) stringResource(R.string.service_status_active) else stringResource(R.string.service_status_inactive),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // Glow indicator
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceActive) Color.Green else Color.Red)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.toggleForegroundService()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServiceActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("service_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isServiceActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isServiceActive) stringResource(R.string.service_status_toggle_off) else stringResource(R.string.service_status_toggle_on))
                        }
                    }
                }
            }

            // Stats grid card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_stats),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.total_saved_contacts),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalContacts",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PeopleAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Last Saved contact card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.last_saved_number),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (lastContact != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lastContact!!.name.take(2),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = lastContact!!.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = lastContact!!.rawNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = getLocalizedSource(context, lastContact!!.source),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.no_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Quick permissions summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.permission_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { navController.navigate("permissions") }) {
                                Icon(Icons.Default.ArrowOutward, contentDescription = "تفاصيل")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PermissionTinyRow(title = "المكالمات والرسائل والملفات", active = permissions["SMS"] == true && permissions["CALLS"] == true && permissions["CONTACTS"] == true)
                            PermissionTinyRow(title = "الحفظ دون تفضيل البطارية", active = permissions["BATTERY_IGNORE"] == true)
                            PermissionTinyRow(title = "الوصول للإشعارات (الواتساب)", active = permissions["NOTIF_LISTENER"] == true)
                            PermissionTinyRow(title = "صلاحية إمكانية الوصول", active = permissions["ACCESSIBILITY"] == true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionTinyRow(title: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (active) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (active) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                color = if (active) Color(0xFF2E7D32) else Color(0xFFC62828),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- SCREEN 3: SAVED CONTACTS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val contacts by viewModel.savedContacts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.contactSearchQuery.collectAsStateWithLifecycle()

    var showEditDialogForContact by remember { mutableStateOf<SavedContact?>(null) }
    var showDeleteDialogForContact by remember { mutableStateOf<SavedContact?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_contacts), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setContactSearchQuery(it) },
                label = { Text(stringResource(R.string.log_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "البحث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_search_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (contacts.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_contacts),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            item.name.take(2),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = item.rawNumber,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${stringResource(R.string.contact_interactions)}: ${item.interactionsCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = getLocalizedSource(context, item.source),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = { showEditDialogForContact = item }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل الاسم", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { showDeleteDialogForContact = item }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
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

    // Dialogue for editing contact name
    if (showEditDialogForContact != null) {
        var tempName by remember { mutableStateOf(showEditDialogForContact!!.name) }
        AlertDialog(
            onDismissRequest = { showEditDialogForContact = null },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.renameContact(showEditDialogForContact!!, tempName)
                            showEditDialogForContact = null
                        }
                    },
                    modifier = Modifier.testTag("dialog_rename_submit")
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogForContact = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = { Text(stringResource(R.string.dialog_edit_title)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.contact_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_rename_input")
                )
            }
        )
    }

    // Dialogue to confirm contact deletion
    if (showDeleteDialogForContact != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialogForContact = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteContact(showDeleteDialogForContact!!)
                        showDeleteDialogForContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_delete_submit")
                ) {
                    Text(stringResource(R.string.btn_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogForContact = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_confirm)) }
        )
    }
}

// --- SCREEN 4: LOGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_logs), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "مسح بالكامل", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLogSearchQuery(it) },
                label = { Text(stringResource(R.string.log_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "البحث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logs_search_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_logs),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs) { log ->
                        val itemColor = when (log.status) {
                            "success" -> Color(0xFFE8F5E9)
                            "updated" -> Color(0xFFE3F2FD)
                            "error" -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val itemTextColor = when (log.status) {
                            "success" -> Color(0xFF2E7D32)
                            "updated" -> Color(0xFF1565C0)
                            "error" -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = itemColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = log.phoneNumber,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = itemTextColor
                                    )
                                    Text(
                                        text = getLocalizedStatusLabel(context, log.status),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = itemTextColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!log.errorMsg.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الخطأ: ${log.errorMsg}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = getLocalizedSource(context, log.source),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ar", "AE")).format(Date(log.timestamp))
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

fun getLocalizedStatusLabel(context: Context, status: String): String {
    return when (status) {
        "success" -> context.getString(R.string.log_op_success)
        "updated" -> context.getString(R.string.log_op_duplicate)
        "ignored_existing" -> context.getString(R.string.log_op_already_saved)
        "error" -> context.getString(R.string.log_op_error)
        else -> status
    }
}

// --- SCREEN 5: SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val settings by viewModel.systemSettings.collectAsStateWithLifecycle()
    val totalContacts by viewModel.totalContactsCount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var editingPrefix by remember { mutableStateOf("") }
    var editingCounter by remember { mutableStateOf("") }

    if (totalContacts >= 100) {
        LaunchedEffect(Unit) {
            viewModel.triggerDeveloperBlock()
        }
    }

    LaunchedEffect(settings) {
        editingPrefix = settings.clientPrefix
        editingCounter = settings.clientCounter.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Section Title name prefix
                item {
                    Text(
                        text = "التسمية التلقائية والعدّادات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column {
                        OutlinedTextField(
                            value = editingPrefix,
                            onValueChange = {
                                editingPrefix = it
                                viewModel.updateClientPrefix(it)
                            },
                            label = { Text(stringResource(R.string.setting_prefix_title)) },
                            placeholder = { Text("عميل") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prefix_prefix_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.setting_prefix_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Column {
                        OutlinedTextField(
                            value = editingCounter,
                            onValueChange = {
                                editingCounter = it
                                val num = it.toIntOrNull()
                                if (num != null) {
                                    viewModel.updateClientCounter(num)
                                }
                            },
                            label = { Text(stringResource(R.string.setting_counter_title)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prefix_counter_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.setting_counter_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item { Divider() }

                // Section Feature toggles
                item {
                    Text(
                        text = "مصادر الرصد والتقاط العملاء",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    SettingsToggleRow(
                        title = stringResource(R.string.setting_calls_active),
                        summary = stringResource(R.string.setting_calls_summary),
                        checked = settings.isCallsEnabled,
                        onCheckedChange = { viewModel.toggleCallsMonitoring(it) }
                    )
                }

                item {
                    SettingsToggleRow(
                        title = stringResource(R.string.setting_sms_active),
                        summary = stringResource(R.string.setting_sms_summary),
                        checked = settings.isSmsEnabled,
                        onCheckedChange = { viewModel.toggleSmsMonitoring(it) }
                    )
                }

                item {
                    SettingsToggleRow(
                        title = stringResource(R.string.setting_whatsapp_active),
                        summary = stringResource(R.string.setting_whatsapp_summary),
                        checked = settings.isWhatsappEnabled,
                        onCheckedChange = { viewModel.toggleWhatsappMonitoring(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// --- SCREEN 6: EXPORT DATA SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    val totalContacts by viewModel.totalContactsCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(exportStatus) {
        if (exportStatus != null) {
            if (exportStatus == "empty") {
                Toast.makeText(context, "قاعدة البيانات فارغة! لا يوجد عملاء للتصدير.", Toast.LENGTH_LONG).show()
            } else if (exportStatus == "error") {
                Toast.makeText(context, context.getString(R.string.export_error), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, context.getString(R.string.export_saved, exportStatus), Toast.LENGTH_LONG).show()
            }
            viewModel.clearExportStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_export), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(110.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "تصدير جهات الاتصال (CRM)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.export_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Total available rows
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("إجمالي الأرقام المتاحة للتصدير", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$totalContacts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.triggerExportContacts() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_csv_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "تحميل")
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_export_csv))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.triggerExportContacts() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_xlsx_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "جدول")
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_export_xlsx))
            }
        }
    }
}

// --- SCREEN 7: PERMISSIONS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val permissions by viewModel.permissionsMap.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshStatusStates()
    }

    // Helper to request Android activity lifecycle for runtime alerts
    val requestPermissionLauncher = rememberLauncherForSystemPermissions {
        viewModel.refreshStatusStates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_permissions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.permissions_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. SMS Permissions
            item {
                PermissionCard(
                    title = "صلاحيات الرسائل SMS",
                    description = "تسمح للتطبيق بقراءة واستخراج الأرقام من رسائل SMS الواردة تلقائيًا وحفظها.",
                    granted = permissions["SMS"] == true,
                    onGrant = {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.RECEIVE_SMS,
                                android.Manifest.permission.READ_SMS
                            )
                        )
                    }
                )
            }

            // 2. Call Logs Permissions
            item {
                PermissionCard(
                    title = "صلاحية رصد المكالمات (Call Log & State)",
                    description = "مهم لتمكين محرك الحفظ من رصد المكالمات المفقودة والمنتهية للعملاء الجدد بالخلفية.",
                    granted = permissions["CALLS"] == true,
                    onGrant = {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.READ_CALL_LOG,
                                android.Manifest.permission.READ_PHONE_STATE
                            )
                        )
                    }
                )
            }

            // 3. Contacts Permission
            item {
                PermissionCard(
                    title = "صلاحيات جهات الاتصال (Contacts Provider)",
                    description = "تسمح لتطبيق AutoSave بالتحقق مما إذا كان ملقى الاتصال مسجلاً، وحفظه باسم CRM جديد ومباشر بالخلفية.",
                    granted = permissions["CONTACTS"] == true,
                    onGrant = {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.READ_CONTACTS,
                                android.Manifest.permission.WRITE_CONTACTS
                            )
                        )
                    }
                )
            }

            // 4. Notification Access Permission (Special Intent)
            item {
                PermissionCard(
                    title = "إذن قراءة إشعارات النظام (WhatsApp)",
                    description = stringResource(R.string.permission_notif_listener_desc),
                    granted = permissions["NOTIF_LISTENER"] == true,
                    onGrant = {
                        try {
                            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "لم نتمكن من فتح شاشة الإعدادات بشكل تلقائي.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // 5. Accessibility Service Permission (Special Intent)
            item {
                PermissionCard(
                    title = "خدمة إمكانية الوصول (Accessibility Service)",
                    description = stringResource(R.string.permission_accessibility_desc),
                    granted = permissions["ACCESSIBILITY"] == true,
                    onGrant = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, "لم نتمكن من فتح شاشة إعدادات إمكانية الوصول.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // 6. Battery optimization ignore (Special Intent)
            item {
                PermissionCard(
                    title = "تجاهل تحسينات البطارية لتطبيق الخلفية",
                    description = stringResource(R.string.permission_battery_desc),
                    granted = permissions["BATTERY_IGNORE"] == true,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "يرجى البحث يدويًا والقيام بتجاهل تحسين البطارية للتطبيق ليعمل بشكل مرن.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCard(title: String, description: String, granted: Boolean, onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (granted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (granted) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                        color = if (granted) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.btn_grant))
                }
            }
        }
    }
}

// Custom launcher adapter for runtime permissions mapping
@Composable
fun rememberLauncherForSystemPermissions(onStateChanged: () -> Unit): androidx.activity.result.ActivityResultLauncher<Array<String>> {
    val context = LocalContext.current
    return androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        onStateChanged()
    }
}

fun getLocalizedSource(context: Context, source: String): String {
    return when (source) {
        "incoming_call" -> context.getString(R.string.source_incoming_call)
        "outgoing_call" -> context.getString(R.string.source_outgoing_call)
        "sms" -> context.getString(R.string.source_sms)
        "whatsapp" -> context.getString(R.string.source_whatsapp)
        "accessibility" -> context.getString(R.string.source_accessibility)
        else -> source
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperBlockDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل التجربة",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "انتهت فترة التجربة المجانية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "لقد وصلت إلى الحد الأقصى للنظام التجريبي (100 عميل مسجل).\n\nلمواصلة تجربة النظام وتفعيله بدون أي قيود، يرجى التواصل مع المطور فوراً لتفعيل الترخيص الخاص بك.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "وسائل التواصل المتاحة للتفعيل السريع:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable contact list to ensure all devices display perfectly
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ContactPlatformCard(
                            name = "رابط واتساب المباشر",
                            detail = "+967 774 440 982",
                            icon = Icons.Default.Chat,
                            iconColor = Color(0xFF25D366),
                            onClick = { openLink(context, "https://wa.me/774440982") }
                        )
                    }
                    item {
                        ContactPlatformCard(
                            name = "اتصال هاتفي مباشر",
                            detail = "+967 774 440 982",
                            icon = Icons.Default.Phone,
                            iconColor = Color(0xFF2E7D32),
                            onClick = { makePhoneCall(context, "+967774440982") }
                        )
                    }
                    item {
                        ContactPlatformCard(
                            name = "مراسلة عبر تيليجرام",
                            detail = "@Gamalalhwish",
                            icon = Icons.Default.Send,
                            iconColor = Color(0xFF0088cc),
                            onClick = { openLink(context, "https://t.me/Gamalalhwish") }
                        )
                    }
                    item {
                        ContactPlatformCard(
                            name = "الملف الشخصي على لينكد إن",
                            detail = "Gamal Alhwish",
                            icon = Icons.Default.Language,
                            iconColor = Color(0xFF0077B5),
                            onClick = { openLink(context, "https://www.linkedin.com/in/gamal-alhwish") }
                        )
                    }
                    item {
                        ContactPlatformCard(
                            name = "الحساب الشخصي على فيسبوك",
                            detail = "جمال الحويش",
                            icon = Icons.Default.Public,
                            iconColor = Color(0xFF1877F2),
                            onClick = { openLink(context, "https://www.facebook.com/jmal.alhwysh.2025") }
                        )
                    }
                    item {
                        ContactPlatformCard(
                            name = "متابعة أو مراسلة عبر إكس (تويتر)",
                            detail = "@alhwysh787472",
                            icon = Icons.Default.Share,
                            iconColor = Color(0xFF1DA1F2),
                            onClick = { openLink(context, "https://x.com/alhwysh787472?s=09") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إغلاق للتصفح فقط", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { openLink(context, "https://wa.me/774440982") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تواصل مباشر واتساب", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactPlatformCard(
    name: String,
    detail: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun openLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح الرابط. الرجاء نسخ الرابط للتواصل.", Toast.LENGTH_SHORT).show()
    }
}

fun makePhoneCall(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح لوحة الاتصال.", Toast.LENGTH_SHORT).show()
    }
}
