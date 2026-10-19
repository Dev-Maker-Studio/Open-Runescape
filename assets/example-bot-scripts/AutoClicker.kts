import com.darkan.bot.scripts.ScriptCategory
import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.StringConfigItem
import com.darkan.bot.scripts.IntConfigItem
import com.darkan.bot.scripts.BooleanConfigItem
import com.darkan.bot.scripts.OptionsConfigItem

@ScriptDescription(
    name = "Auto Clicker",
    version = "1.0",
    author = "Claude",
    description = "Automatically clicks objects or NPCs with configurable options",
    visible = true,
    category = ScriptCategory.OTHER
)
class AutoClicker : BotScript(), ConfigurableScript {
    private val targetType = OptionsConfigItem(
        name = "Type",
        description = "What to click",
        options = arrayOf("Object", "NPC", "Ground Item"),
        initialValue = "Object"
    )

    private val targetName = StringConfigItem(
        name = "Target Name",
        description = "Name of the object/NPC to click",
        initialValue = "Tree"
    )

    private val actionName = StringConfigItem(
        name = "Action",
        description = "Action to perform (e.g., 'chop', 'talk', 'take')",
        initialValue = "chop"
    )

    private val clickDelay = IntConfigItem(
        name = "Click Delay (ms)",
        description = "Delay between clicks",
        initialValue = 600,
        min = 100,
        max = 10000
    )

    private val waitForAnimation = BooleanConfigItem(
        name = "Wait for Animation",
        description = "Wait for player to stop animating before next click",
        initialValue = true
    )

    private var clickCount = 0
    private var successCount = 0

    override fun onStart() {
        println("=== Auto Clicker Started ===")
        println("Settings:")
        println("  Type: ${targetType.value}")
        println("  Target: ${targetName.value}")
        println("  Action: ${actionName.value}")
        println("  Delay: ${clickDelay.value}ms")
        println("  Wait for animation: ${waitForAnimation.value}")
        clickCount = 0
        successCount = 0
    }

    override suspend fun loop() {
        // Wait if we're animating and that option is enabled
        if (waitForAnimation.value && isAnimating()) {
            delay(100)
            return
        }

        // Perform the click based on type
        val success = when (targetType.value) {
            "Object" -> clickObject()
            "NPC" -> clickNpc()
            "Ground Item" -> clickGroundItem()
            else -> false
        }

        clickCount++
        if (success) {
            successCount++
            println("✓ Click #$clickCount successful (${successCount}/$clickCount = ${(successCount * 100 / clickCount)}% success rate)")
        } else {
            println("✗ Click #$clickCount failed - target not found")
        }

        // Wait before next click
        delay(clickDelay.value)
    }

    private fun clickObject(): Boolean {
        val obj = getClosestObject(targetName.value)
        return if (obj != null) {
            clickObject(obj, actionName.value)
            true
        } else {
            false
        }
    }

    private fun clickNpc(): Boolean {
        val npc = getClosestNPC(targetName.value)
        return if (npc != null) {
            clickNPC(npc, actionName.value)
            true
        } else {
            false
        }
    }

    private fun clickGroundItem(): Boolean {
        val item = findGroundItem(targetName.value)
        return if (item != null) {
            takeGroundItem(item.z, item)
            true
        } else {
            false
        }
    }

    override fun onStop() {
        println("=== Auto Clicker Stopped ===")
        println("Statistics:")
        println("  Total Clicks: $clickCount")
        println("  Successful: $successCount")
        println("  Failed: ${clickCount - successCount}")
        if (clickCount > 0) {
            println("  Success Rate: ${(successCount * 100 / clickCount)}%")
        }
    }
}

// Return the script instance
AutoClicker()
