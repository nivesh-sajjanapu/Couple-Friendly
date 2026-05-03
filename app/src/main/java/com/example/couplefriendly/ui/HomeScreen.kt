package com.example.couplefriendly.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.couplefriendly.data.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var myCode by remember { mutableStateOf("Loading...") }
    var partnerCode by remember { mutableStateOf("") }
    var partnerId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showGoToChatButton by remember { mutableStateOf(false) }

    // Load user data - BUG FIX #1: Removed auto-navigation to chat
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        myCode = doc.getString("partnerCode") ?: ""
                        partnerId = doc.getString("partnerId") ?: ""

                        // BUG FIX #1: Show button to go to chat instead of auto-navigating
                        if (partnerId.isNotEmpty()) {
                            showGoToChatButton = true
                        }
                    } else {
                        // Create new user profile with unique code
                        val code = Random.nextInt(100000, 999999).toString()
                        val userData = hashMapOf(
                            "uid" to uid,
                            "email" to currentUser.email,
                            "partnerCode" to code,
                            "partnerId" to ""
                        )
                        db.collection("users").document(uid).set(userData)
                            .addOnSuccessListener {
                                myCode = code
                                errorMessage = ""
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Database Error: ${e.message}\n\nPlease enable Firestore in Firebase Console"
                                myCode = code // Still show code even if save fails
                            }
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Connection Error: ${e.message}\n\nPlease enable Firestore in Firebase Console"
                    // Generate code anyway so user can see it
                    val code = Random.nextInt(100000, 999999).toString()
                    myCode = code
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚀 Rise") },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, "Sign Out")
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
            Text(
                "Connect with Learning Partner",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Learn together and earn rewards",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Your Referral Code",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        myCode,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Share this code to connect with a partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Have a Referral Code?",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = partnerCode,
                onValueChange = { partnerCode = it },
                label = { Text("Enter Referral Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show "Go to Chat" button if already paired
            if (showGoToChatButton) {
                Button(
                    onClick = {
                        // BUG FIX #3: Save session before navigating
                        currentUser?.uid?.let { uid ->
                            coroutineScope.launch {
                                sessionManager.savePairedSession(uid, partnerId)
                                onNavigateToChat(partnerId)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Chat")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (partnerCode.isEmpty()) {
                        message = "Please enter a code"
                        return@Button
                    }

                    isLoading = true
                    // Find partner by code
                    db.collection("users")
                        .whereEqualTo("partnerCode", partnerCode)
                        .get()
                        .addOnSuccessListener { docs ->
                            if (!docs.isEmpty) {
                                val partnerDoc = docs.documents[0]
                                val partnerUid = partnerDoc.id

                                // Update current user's partnerId
                                currentUser?.uid?.let { uid ->
                                    db.collection("users").document(uid)
                                        .update("partnerId", partnerUid)
                                        .addOnSuccessListener {
                                            // Update partner's partnerId
                                            db.collection("users").document(partnerUid)
                                                .update("partnerId", uid)
                                                .addOnSuccessListener {
                                                    // BUG FIX #3: Save paired session
                                                    coroutineScope.launch {
                                                        sessionManager.savePairedSession(uid, partnerUid)
                                                        isLoading = false
                                                        partnerId = partnerUid
                                                        showGoToChatButton = true
                                                        message = "Connected! Tap 'Go to Chat' to start messaging"
                                                    }
                                                }
                                        }
                                }
                            } else {
                                isLoading = false
                                message = "Invalid code"
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            message = "Error: ${it.message}"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !showGoToChatButton
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Connect with Partner")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ Setup Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    message,
                    color = if (message.contains("Error")) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
