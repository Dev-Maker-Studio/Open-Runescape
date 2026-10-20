package com.jagex.game.runetek5.client.login

import com.jagex.game.runetek5.entity.EntityNode
import com.jagex.game.runetek5.graphics.model.particle.ParticleNode
import com.jagex.game.runetek5.utils.Utils

class LoginStateUpdate(
    @JvmField val username: String,
    actionType: Int
) : EntityNode() {
    @JvmField
    val time: Int = (Utils.time() / 1000L).toInt()

    @JvmField
    val actionType: Short = actionType.toShort()
}
