package com.example.guardiancare

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.navigation.DrawerItem
import com.example.guardiancare.navigation.Routes
import com.example.guardiancare.ui.screens.CaretakerScreen
import com.example.guardiancare.ui.screens.CheckInScreen
import com.example.guardiancare.ui.screens.ElderlyScreen
import com.example.guardiancare.ui.screens.ElderlyProfileScreen
import com.example.guardiancare.ui.screens.FallNotificationScreen
import com.example.guardiancare.ui.screens.HelpScreen
import com.example.guardiancare.ui.screens.HomeScreen
import com.example.guardiancare.ui.screens.ProfileScreen
import com.example.guardiancare.ui.screens.SettingsScreen
import com.example.guardiancare.utils.BleManager
import com.example.guardiancare.utils.NotificationHelper
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDrawerScreen(rootNavController: NavController? = null) {
    // The drawer's state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // A child navController for the screens inside the drawer
    val childNavController = rememberNavController()

    // Get user role from SessionManager
    val context = LocalContext.current
    val sessionManager = SessionManager.getInstance(context)
    val userType = sessionManager.getUserType() ?: "elderly"

    // Set up BleManager & NotificationHelper
    val bleManager = remember { BleManager.getInstance(context) }
    val notificationHelper = remember { NotificationHelper.getInstance(context) }

    // Generate navigation items based on user role
    val navItems = if (userType == "caretaker") {
        // Member/Caretaker navigation
        listOf(
            DrawerItem(Routes.Home, stringResource(R.string.home), Icons.Filled.Home),
            DrawerItem(Routes.Elderly, stringResource(R.string.elderly), Icons.Filled.Person),
            DrawerItem(Routes.FallNotification, stringResource(R.string.fall_notifications), Icons.Filled.Warning),
            DrawerItem(Routes.Profile, stringResource(R.string.profile), Icons.Filled.AccountCircle),
            DrawerItem(Routes.Help, stringResource(R.string.help), Icons.Filled.Info),
            DrawerItem(Routes.Settings, stringResource(R.string.settings), Icons.Filled.Settings),
        )
    } else {
        // Elderly navigation
        listOf(
            DrawerItem(Routes.Home, stringResource(R.string.home), Icons.Filled.Home),
            DrawerItem(Routes.CheckIn, stringResource(R.string.check_in), Icons.AutoMirrored.Filled.Assignment),
            DrawerItem(Routes.Profile, stringResource(R.string.profile), Icons.Filled.AccountCircle), 
            DrawerItem(Routes.Help, stringResource(R.string.help), Icons.Filled.Info),
            DrawerItem(Routes.Settings, stringResource(R.string.settings), Icons.Filled.Settings),
        )
    }

    // Track which item is "selected"
    var selectedRoute by remember { mutableStateOf(Routes.Home) }

    // Navigation drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(if (userType == "caretaker") R.string.caretaker_dashboard else R.string.elderly_dashboard),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                navItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        selected = (selectedRoute == item.route),
                        onClick = {
                            selectedRoute = item.route
                            // Navigate in the child nav controller
                            childNavController.navigate(item.route) {
                                // Pop back stack to avoid building up backstack
                                popUpTo(Routes.Home) { inclusive = false }
                            }
                            // Close the drawer
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                
                // Add logout option at the bottom
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.logout)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
                    selected = false,
                    onClick = {
                        // Clear session and go back to login
                        sessionManager.logout()
                        
                        // Navigate back to login screen using root nav controller
                        rootNavController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        // Main page content
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.guardian_care)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open Drawer")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = childNavController,
                startDestination = Routes.Home,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Main screens
                composable(Routes.Home) { HomeScreen(childNavController) }
                composable(Routes.Profile) { ProfileScreen() }
                composable(Routes.Settings) { SettingsScreen(childNavController) }
                composable(Routes.Help) { HelpScreen() }
                
                // Role-specific screens
                composable(Routes.Caretaker) { CaretakerScreen() }
                composable(Routes.Elderly) { ElderlyScreen(navController = childNavController) }
                composable(
                    route = Routes.ElderlyProfile,
                    arguments = listOf(
                        navArgument("elderlyId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val elderlyId = backStackEntry.arguments?.getString("elderlyId")
                    if (elderlyId != null) {
                        ElderlyProfileScreen(navController = childNavController, elderlyId = elderlyId)
                    }
                }
                composable(Routes.CheckIn) { CheckInScreen() }
                composable(Routes.FallNotification) { FallNotificationScreen(navController = childNavController) }
            }
        }
    }
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
