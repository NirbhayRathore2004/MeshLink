package com.meshlink.mesh.identity

import android.content.Context
import android.content.SharedPreferences
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class NodeIdentityManager(
    private val context: Context? = null,
    initialNodeId: String? = null,
    initialAlias: String? = null
) {
    private val prefs: SharedPreferences? by lazy {
        try {
            context?.getSharedPreferences("meshlink_identity_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    private val _nodeId = MutableStateFlow(initialNodeId ?: getOrGenerateNodeId())
    val nodeId: StateFlow<String> = _nodeId.asStateFlow()

    private val _nodeAlias = MutableStateFlow(initialAlias ?: getOrGenerateAlias())
    val nodeAlias: StateFlow<String> = _nodeAlias.asStateFlow()

    private fun getOrGenerateNodeId(): String {
        var id = prefs?.getString(KEY_NODE_ID, null)
        if (id.isNullOrBlank()) {
            val shortUuid = UUID.randomUUID().toString().take(8).uppercase()
            id = "NODE-$shortUuid"
            prefs?.edit()?.putString(KEY_NODE_ID, id)?.apply()
            MeshLogger.i(MeshLogger.TAG_MESH, "Generated new local Node ID: $id")
        }
        return id
    }

    private fun getOrGenerateAlias(): String {
        var alias = prefs?.getString(KEY_NODE_ALIAS, null)
        if (alias.isNullOrBlank()) {
            alias = _nodeId.value
            prefs?.edit()?.putString(KEY_NODE_ALIAS, alias)?.apply()
        }
        return alias
    }

    fun setNodeAlias(newAlias: String) {
        val trimmed = newAlias.trim().ifEmpty { _nodeId.value }
        prefs?.edit()?.putString(KEY_NODE_ALIAS, trimmed)?.apply()
        _nodeAlias.value = trimmed
        MeshLogger.i(MeshLogger.TAG_MESH, "Updated Node Alias to: $trimmed")
    }

    fun setCustomIdentity(customId: String, customAlias: String) {
        val cleanId = customId.trim().uppercase()
        val cleanAlias = customAlias.trim()
        prefs?.edit()
            ?.putString(KEY_NODE_ID, cleanId)
            ?.putString(KEY_NODE_ALIAS, cleanAlias)
            ?.apply()
        _nodeId.value = cleanId
        _nodeAlias.value = cleanAlias
        MeshLogger.i(MeshLogger.TAG_MESH, "Switched Node Identity to ID: $cleanId, Alias: $cleanAlias")
    }

    fun setDemoPreset(presetName: String) {
        val presetId = presetName.trim().uppercase()
        setCustomIdentity(presetId, presetId.replace("-", " "))
    }

    companion object {
        private const val KEY_NODE_ID = "pref_node_id"
        private const val KEY_NODE_ALIAS = "pref_node_alias"
    }
}
