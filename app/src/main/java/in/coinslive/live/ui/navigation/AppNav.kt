package `in`.coinslive.live.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.coinslive.live.api.LiveRoomKind
import `in`.coinslive.live.api.WebinarClientRole
import `in`.coinslive.live.auth.AuthSession
import `in`.coinslive.live.deeplink.DeepLinkInbox
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.account.AccountScreen
import `in`.coinslive.live.ui.adhyayan.AdhyayanLobbyScreen
import `in`.coinslive.live.ui.adhyayan.AdhyayanRoomScreen
import `in`.coinslive.live.ui.auth.SignInScreen
import `in`.coinslive.live.ui.create.CreateAdhyayanScreen
import `in`.coinslive.live.ui.create.CreateWebinarScreen
import `in`.coinslive.live.ui.home.HomeScreen
import `in`.coinslive.live.ui.join.JoinHubScreen
import `in`.coinslive.live.ui.webinar.WebinarLobbyScreen
import `in`.coinslive.live.ui.webinar.WebinarRoomScreen

object Routes {
    const val MAIN = "main"
    const val CREATE_WORKSHOP = "create/workshop"
    const val CREATE_ADHYAYAN = "create/adhyayan"

    const val ARG_SLUG = "slug"
    const val ARG_TITLE = "title"
    const val ARG_ROLE = "role"

    const val WEBINAR_LOBBY = "webinar/{$ARG_SLUG}"
    const val WEBINAR_ROOM = "webinar/{$ARG_SLUG}/room?$ARG_ROLE={$ARG_ROLE}"
    const val ADHYAYAN_LOBBY = "adhyayan/{$ARG_SLUG}"
    // Keep title out of the route — query args with odd characters have crashed Navigation.
    const val ADHYAYAN_ROOM = "adhyayan/{$ARG_SLUG}/room"

    fun webinarLobby(slug: String) = "webinar/${Uri.encode(slug)}"

    fun webinarRoom(slug: String, title: String? = null, role: WebinarClientRole): String {
        // Title is intentionally unused in the route (see WEBINAR_ROOM).
        return "webinar/${Uri.encode(slug)}/room?$ARG_ROLE=${role.wire}"
    }

    fun adhyayanLobby(slug: String) = "adhyayan/${Uri.encode(slug)}"

    fun adhyayanRoom(slug: String, title: String? = null): String {
        // Title is intentionally unused in the route (see ADHYAYAN_ROOM).
        return "adhyayan/${Uri.encode(slug)}/room"
    }

    fun lobbyFor(kind: LiveRoomKind, slug: String): String = when (kind) {
        LiveRoomKind.WEBINAR -> webinarLobby(slug)
        LiveRoomKind.ADHYAYAN -> adhyayanLobby(slug)
    }
}

@Composable
fun AppNav(auth: AuthSession) {
    val signedIn by auth.isSignedIn.collectAsState()
    if (signedIn) {
        SignedInNavHost(auth)
    } else {
        SignInScreen(auth)
    }
}

@Composable
private fun SignedInNavHost(auth: AuthSession) {
    val nav = rememberNavController()
    val pending by DeepLinkInbox.pending.collectAsState()

    LaunchedEffect(pending) {
        val request = pending ?: return@LaunchedEffect
        DeepLinkInbox.consume(request)
        nav.navigate(Routes.lobbyFor(request.kind, request.slug)) { launchSingleTop = true }
    }

    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) { MainTabs(auth, nav) }

        composable(Routes.CREATE_WORKSHOP) {
            CreateWebinarScreen(
                onBack = { nav.popBackStack() },
                onCreated = { slug ->
                    nav.navigate(Routes.webinarLobby(slug)) {
                        popUpTo(Routes.CREATE_WORKSHOP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CREATE_ADHYAYAN) {
            CreateAdhyayanScreen(
                onBack = { nav.popBackStack() },
                onCreated = { slug ->
                    nav.navigate(Routes.adhyayanLobby(slug)) {
                        popUpTo(Routes.CREATE_ADHYAYAN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            Routes.WEBINAR_LOBBY,
            arguments = listOf(navArgument(Routes.ARG_SLUG) { type = NavType.StringType }),
        ) { entry ->
            val slug = entry.slug()
            WebinarLobbyScreen(
                onBack = { nav.popBackStack() },
                onEnterRoom = { title, role ->
                    nav.navigate(Routes.webinarRoom(slug, title, role))
                },
            )
        }

        composable(
            Routes.WEBINAR_ROOM,
            arguments = listOf(
                navArgument(Routes.ARG_SLUG) { type = NavType.StringType },
                navArgument(Routes.ARG_ROLE) {
                    type = NavType.StringType
                    defaultValue = WebinarClientRole.HOST.wire
                },
            ),
        ) {
            WebinarRoomScreen(onExit = { nav.popBackStack() })
        }

        composable(
            Routes.ADHYAYAN_LOBBY,
            arguments = listOf(navArgument(Routes.ARG_SLUG) { type = NavType.StringType }),
        ) { entry ->
            val slug = entry.slug()
            AdhyayanLobbyScreen(
                onBack = { nav.popBackStack() },
                onEnterRoom = { title -> nav.navigate(Routes.adhyayanRoom(slug, title)) },
            )
        }

        composable(
            Routes.ADHYAYAN_ROOM,
            arguments = listOf(
                navArgument(Routes.ARG_SLUG) { type = NavType.StringType },
            ),
        ) {
            AdhyayanRoomScreen(onExit = { nav.popBackStack() })
        }
    }
}

private fun NavBackStackEntry.slug(): String =
    arguments?.getString(Routes.ARG_SLUG).orEmpty()

private enum class MainTab(val label: String) {
    LIVE("Live"),
    JOIN("Join"),
    ACCOUNT("Account"),
}

@Composable
private fun MainTabs(auth: AuthSession, nav: NavController) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = MainTab.entries

    CoinsLiveTheme {
        Scaffold(
            containerColor = CoinsLiveColors.Navy,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(containerColor = CoinsLiveColors.Navy, contentColor = Color.White) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        MainTab.LIVE -> Icons.Default.PlayArrow
                                        MainTab.JOIN -> Icons.AutoMirrored.Filled.ArrowForward
                                        MainTab.ACCOUNT -> Icons.Default.Person
                                    },
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = CoinsLiveColors.Brand,
                                indicatorColor = CoinsLiveColors.Brand,
                                unselectedIconColor = CoinsLiveColors.OnDarkMuted,
                                unselectedTextColor = CoinsLiveColors.OnDarkMuted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tabs[selected]) {
                    MainTab.LIVE -> HomeScreen(
                        onOpenWorkshop = { slug -> nav.navigate(Routes.webinarLobby(slug)) },
                        onOpenAdhyayan = { slug -> nav.navigate(Routes.adhyayanLobby(slug)) },
                        onCreateWorkshop = { nav.navigate(Routes.CREATE_WORKSHOP) },
                        onCreateAdhyayan = { nav.navigate(Routes.CREATE_ADHYAYAN) },
                    )

                    MainTab.JOIN -> JoinHubScreen(
                        onJoined = { slug, kind ->
                            selected = 0
                            nav.navigate(Routes.lobbyFor(kind, slug))
                        },
                    )

                    MainTab.ACCOUNT -> AccountScreen(auth)
                }
            }
        }
    }
}
