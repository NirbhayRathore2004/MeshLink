package com.meshlink.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.meshlink.di.AppContainer
import com.meshlink.ui.chat.ChatScreen
import com.meshlink.ui.chat.ChatViewModel
import com.meshlink.ui.demo.DemoScreen
import com.meshlink.ui.demo.DemoViewModel
import com.meshlink.ui.devices.DevicesScreen
import com.meshlink.ui.devices.DevicesViewModel
import com.meshlink.ui.home.HomeScreen
import com.meshlink.ui.home.HomeViewModel
import com.meshlink.ui.messages.MessagesScreen
import com.meshlink.ui.messages.MessagesViewModel
import com.meshlink.ui.network.NetworkTopologyScreen
import com.meshlink.ui.network.NetworkViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(appContainer)
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                onNavigateToDevices = { navController.navigate(Screen.Devices.route) },
                onNavigateToTopology = { navController.navigate(Screen.NetworkTopology.route) },
                onNavigateToDemo = { navController.navigate(Screen.Demo.route) }
            )
        }

        composable(Screen.Messages.route) {
            val viewModel: MessagesViewModel = viewModel(
                factory = MessagesViewModel.Factory(appContainer)
            )
            MessagesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { peerId, peerAlias ->
                    navController.navigate(Screen.Chat.createRoute(peerId, peerAlias))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType },
                navArgument("peerAlias") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("peerId") ?: "", "UTF-8")
            val peerAlias = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("peerAlias") ?: "", "UTF-8")

            val viewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(peerId, peerAlias, appContainer)
            )
            ChatScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Devices.route) {
            val viewModel: DevicesViewModel = viewModel(
                factory = DevicesViewModel.Factory(appContainer)
            )
            DevicesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenChat = { nodeId, alias ->
                    navController.navigate(Screen.Chat.createRoute(nodeId, alias))
                }
            )
        }

        composable(Screen.NetworkTopology.route) {
            val viewModel: NetworkViewModel = viewModel(
                factory = NetworkViewModel.Factory(appContainer)
            )
            NetworkTopologyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Demo.route) {
            val viewModel: DemoViewModel = viewModel(
                factory = DemoViewModel.Factory(appContainer)
            )
            DemoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
