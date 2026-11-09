package `in`.coinslive.live.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.coinslive.live.BuildConfig
import `in`.coinslive.live.auth.AuthSession
import `in`.coinslive.live.config.AppConfig
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.ui.components.CoinsLiveCard
import `in`.coinslive.live.ui.components.CoinsLiveTopBar
import `in`.coinslive.live.ui.components.CoinsLiveWordmark
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.ui.components.LabeledRow
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(auth: AuthSession) {
    val signedIn by auth.isSignedIn.collectAsState()
    val scope = rememberCoroutineScope()

    var confirmDelete by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = CoinsLiveColors.Navy,
        topBar = { CoinsLiveTopBar(title = "Account") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CoinsLiveWordmark(onDark = true)

            Section("Session") {
                LabeledRow("Status", if (signedIn) "Signed in" else "Not signed in")
                if (signedIn) {
                    TextButton(onClick = { scope.launch { auth.signOut() } }) {
                        Text("Sign out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (signedIn) {
                Section("Danger zone") {
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !isDeleting,
                    ) {
                        Text(
                            "Delete account",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (isDeleting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Deleting account…", fontSize = 13.sp, color = CoinsLiveColors.OnLightSecondary)
                        }
                    }
                    ErrorText(deleteError)
                    Text(
                        "Deletes your CoinsLive account and signs you out here. This cannot be undone.",
                        fontSize = 13.sp,
                        color = CoinsLiveColors.OnLightMuted,
                    )
                }
            }

            Section("App") {
                LabeledRow("API", AppConfig.apiHost)
                LabeledRow("Version", BuildConfig.VERSION_NAME)
                Text(
                    "Workshops and Adhyayan video calls.",
                    fontSize = 13.sp,
                    color = CoinsLiveColors.OnLightSecondary,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "Your CoinsLive account and profile will be deleted. " +
                        "Session history for this account will no longer be available.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleteError = null
                    isDeleting = true
                    scope.launch {
                        try {
                            auth.deleteAccount()
                        } catch (e: Exception) {
                            deleteError = e.message ?: "Could not delete your account."
                        } finally {
                            isDeleting = false
                        }
                    }
                }) { Text("Delete account", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = CoinsLiveColors.OnDarkAccent, fontWeight = FontWeight.SemiBold)
        CoinsLiveCard { content() }
    }
}
