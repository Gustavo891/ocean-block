package one_block.libs.instance

import java.util.concurrent.ScheduledExecutorService

val ScheduledExecutor: ScheduledExecutorService get() = ScheduledExecutorInstance

lateinit var ScheduledExecutorInstance: ScheduledExecutorService
