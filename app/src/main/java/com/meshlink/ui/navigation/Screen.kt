package com.meshlink.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Messages : Screen("messages")
    object Chat : Screen("chat/{peerId}/{peerAlias}") {
        fun createRoute(peerId: String, peerAlias: String): String {
            val encodedPeerId = java.net.URLEncoder.encode(peerId, "UTF-8")
            val encodedAlias = java.net.URLEncoder.encode(peerAlias, "UTF-8")
            return "chat/$encodedPeerId/$encodedAlias"
        }
    }
    object Devices : Screen("devices")
    object NetworkTopology : Screen("network_topology")
    object Demo : Screen("demo")
}
