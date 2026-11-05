package com.meshlink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.meshlink.ui.navigation.NavGraph
import com.meshlink.ui.navigation.Screen
import com.meshlink.ui.theme.MeshLinkTheme

class MainActivity : ComponentActivity() {

    private var targetConversationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MeshApplication
        val container = app.container

        handleIncomingIntent(intent)

        setContent {
            MeshLinkTheme {
                val navController = rememberNavController()

                LaunchedEffect(targetConversationId) {
                    targetConversationId?.let { convId ->
                        navController.navigate(Screen.Chat.createRoute(convId, convId))
                        targetConversationId = null
                    }
                }

                NavGraph(
                    navController = navController,
                    appContainer = container
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val conversationId = intent?.getStringExtra("EXTRA_CONVERSATION_ID")
        if (!conversationId.isNullOrBlank()) {
            targetConversationId = conversationId
        }
    }
}
