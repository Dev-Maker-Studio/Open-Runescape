package com.rs.plugin.handlers

import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import com.rs.plugin.events.EmoteEvent
import java.util.function.Consumer

class EmoteHandler(emotes: Array<out Emote>, handler: Consumer<EmoteEvent>) : PluginHandler<EmoteEvent>(arrayOf<Any>(*emotes), handler)
