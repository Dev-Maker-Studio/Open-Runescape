package com.rs.plugin.handlers

import com.rs.plugin.events.IFOnGroundItemEvent
import java.util.function.Consumer

class InterfaceOnGroundItemHandler(val fromInterfaceIds: IntArray, val fromComponents: IntArray, val itemIds: IntArray?, handler: Consumer<IFOnGroundItemEvent>) :
    PluginHandler<IFOnGroundItemEvent>(arrayOf<Any>(), handler)
