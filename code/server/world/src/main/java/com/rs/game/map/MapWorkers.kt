package com.rs.game.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object MapWorkers : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism((Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)))
