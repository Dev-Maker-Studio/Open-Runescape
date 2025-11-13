package com.jagex.game.runetek5.js5.request

/**
 *
 * @author Brainpower - 02/06/2020
 * Usage: import static com.jagex.game.runetek5.network.js5.RequestStatus.*;
 *
 * Represents the RequestStatus of a #RemoteFileWorker's request
 *
 */
class RequestStatus {
    companion object {
        const val REGULAR: Int = -1
        const val PRIORITY: Int = 0
        const val RECEIVED: Int = 1
    }
}
