package com.example.guardiancare.navigation

/**
 * Drawer Navigation Item
 * ====================
 * This data class represents an item in the app's navigation drawer.
 * It contains all the information needed to display and handle navigation
 * for a drawer menu item.
 * 
 * Used in conjunction with Routes.kt to define the app's navigation structure.
 * The MainDrawerScreen uses these items to build the navigation drawer UI.
 */

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single item in the navigation drawer
 * 
 * @param route The navigation route this item leads to (from Routes.kt)
 * @param label The display name shown in the drawer
 * @param icon The icon displayed next to the label
 */
data class DrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)