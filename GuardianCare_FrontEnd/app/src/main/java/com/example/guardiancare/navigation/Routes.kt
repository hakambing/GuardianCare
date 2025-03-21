package com.example.guardiancare.navigation

/**
 * Navigation Routes
 * ===============
 * This object defines all the navigation routes used within the app's drawer navigation.
 * It provides a centralized place to manage route names, making it easier to maintain
 * and update the app's navigation structure.
 * 
 * These routes are used in the drawer navigation after the user is authenticated.
 * For the root-level navigation (login, register, drawerHome), see GuardianCareApp.kt.
 */

object Routes {
    // Common screens for both user types
    const val Home = "home"               // Main dashboard screen
    const val Profile = "profile"         // User profile screen
    const val Settings = "settings"       // App settings screen
    const val Help = "help"               // Help and support screen
    
    // Authentication
    const val Registration = "registration"    // User registration screen
    
    // Caretaker-specific screens
    const val Caretaker = "caretaker"          // Caretaker dashboard
    const val Chatbot = "chatbot"              // AI assistance for caretakers
    
    // Elderly-specific screens
    const val Elderly = "elderly"              // Elderly dashboard
    const val ElderlyProfile = "elderlyProfile/{elderlyId}"  // Elderly detailed profile screen
    const val FallNotification = "fallNotification"  // Shows fall alerts history
    const val CheckIn = "checkIn"              // Daily check-in for elderly users
}