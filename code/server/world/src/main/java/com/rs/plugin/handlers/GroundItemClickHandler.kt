package com.rs.plugin.handlers

import com.rs.plugin.events.GroundItemClickEvent
import java.util.function.Consumer

class GroundItemClickHandler(itemIds: Array<Any>, val options: Array<String>, val isCheckDistance: Boolean, handler: Consumer<GroundItemClickEvent>) :
    PluginHandler<GroundItemClickEvent>(itemIds, handler)
