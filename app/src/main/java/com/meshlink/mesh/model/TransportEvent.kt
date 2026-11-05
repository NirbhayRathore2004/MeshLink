package com.meshlink.mesh.model

sealed class TransportEvent {
    data class PeerDiscovered(
        val endpointId: String,
        val endpointName: String,
        val serviceId: String
    ) : TransportEvent()

    data class PeerLost(
        val endpointId: String
    ) : TransportEvent()

    data class ConnectionInitiated(
        val endpointId: String,
        val endpointName: String,
        val authenticationDigits: String
    ) : TransportEvent()

    data class PeerConnected(
        val endpointId: String,
        val endpointName: String
    ) : TransportEvent()

    data class PeerDisconnected(
        val endpointId: String
    ) : TransportEvent()

    data class DataReceived(
        val endpointId: String,
        val bytes: ByteArray
    ) : TransportEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DataReceived
            return endpointId == other.endpointId && bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = endpointId.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : TransportEvent()
}
