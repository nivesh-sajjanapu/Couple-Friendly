package com.example.couplefriendly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.couplefriendly.data.SessionManager
import com.example.couplefriendly.ui.AuthScreen
import com.example.couplefriendly.ui.ChatScreen
import com.example.couplefriendly.ui.HomeScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CoupleFriendlyApp()
                }
            }
        }
    }
}

@Composable
fun CoupleFriendlyApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // BUG FIX #3: Check for saved session to determine start destination
    val startDestination = remember {
        runBlocking {
            val hasPaired = sessionManager.hasPairedSession.first()
            val partnerId = sessionManager.partnerId.first()

            when {
                currentUser == null -> "auth"
                hasPaired && !partnerId.isNullOrEmpty() -> "chat/$partnerId"
                else -> "home"
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    // BUG FIX #1: Always go to home screen after login (never skip to chat)
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToChat = { partnerId ->
                    // BUG FIX #2: Navigate to chat but keep home in back stack
                    navController.navigate("chat/$partnerId")
                },
                onLogout = {
                    // BUG FIX #3: Clear session on logout
                    coroutineScope.launch {
                        sessionManager.clearSession()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = "chat/{partnerId}",
            arguments = listOf(navArgument("partnerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
            ChatScreen(
                partnerId = partnerId,
                onNavigateBack = {
                    // BUG FIX #2: Go back to home screen (passcode screen)
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
    }
}