package `in`.coinslive.live.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.coinslive.live.BuildConfig
import `in`.coinslive.live.auth.AuthSession
import `in`.coinslive.live.config.AppConfig
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.theme.SpaceGroteskFamily
import `in`.coinslive.live.ui.components.CoinsLiveTopBar
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.util.CrashLog
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(auth: AuthSession) {
    val context = LocalContext.current
    val signedIn by auth.isSignedIn.collectAsState()
    val scope = rememberCoroutineScope()

    var confirmDelete by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var crashStack by remember { mutableStateOf(CrashLog.read(context)) }

    CoinsLiveTheme(paper = true) {
        Scaffold(
            containerColor = CoinsLiveColors.Canvas,
            topBar = { CoinsLiveTopBar(title = "Account", onDark = false) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CoinsLiveColors.Navy),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "CL",
                            color = Color.White,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (signedIn) "Signed in" else "Not signed in",
                            style = MaterialTheme.typography.titleMedium,
                            color = CoinsLiveColors.Ink,
                        )
                        Text(
                            "CoinsLive Live · ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoinsLiveColors.OnLightMuted,
                        )
                    }
                }

                SettingsCard {
                    SettingsRow("API", AppConfig.apiHost)
                    HorizontalDivider(color = CoinsLiveColors.Border)
                    SettingsRow("Version", BuildConfig.VERSION_NAME)
                    HorizontalDivider(color = CoinsLiveColors.Border)
                    SettingsRow("Product", "Workshops and Adhyayan video calls")
                }

                if (signedIn) {
                    SettingsCard {
                        TextButton(
                            onClick = { scope.launch { auth.signOut() } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Sign out",
                                color = CoinsLiveColors.LiveText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        HorizontalDivider(color = CoinsLiveColors.Border)
                        TextButton(
                            onClick = { confirmDelete = true },
                            enabled = !isDeleting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Delete account",
                                color = CoinsLiveColors.LiveText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (isDeleting) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Deleting account…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        ErrorText(deleteError, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        Text(
                            "Deletes your CoinsLive account. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoinsLiveColors.OnLightMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }

                crashStack?.let { stack ->
                    SettingsCard {
                        Text(
                            "Last failure (internal)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp),
                        )
                        Text(
                            stack.take(2500),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Row(modifier = Modifier.padding(8.dp)) {
                            TextButton(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("crash", stack))
                                },
                            ) { Text("Copy stack", fontWeight = FontWeight.SemiBold) }
                            TextButton(
                                onClick = {
                                    CrashLog.clear(context)
                                    crashStack = null
                                },
                            ) { Text("Clear") }
                        }
                    }
                }
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
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CoinsLiveColors.Border, RoundedCornerShape(12.dp)),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CoinsLiveColors.OnLightMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = CoinsLiveColors.Ink,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
