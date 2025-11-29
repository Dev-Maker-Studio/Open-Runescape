import com.darkan.bot.scripts.ScriptCategory
import com.darkan.bot.scripts.event.Event
import com.darkan.bot.scripts.event.XPDrop
import com.darkan.bot.utils.Skill

@ScriptDescription(
    name = "Skill Monitor",
    version = "1.0",
    author = "Claude",
    description = "Monitors XP drops and tracks skill progress across all skills",
    visible = true,
    category = ScriptCategory.OTHER
)
class SkillMonitor : BotScript() {

    private val xpGains = mutableMapOf<Skill, Int>()
    private val startTime = System.currentTimeMillis()

    override fun onStart() {
        println("=== Skill Monitor Started ===")
        println("Monitoring all XP gains...")
        println("Current stats:")
        listOf(
            Skill.ATTACK, Skill.STRENGTH, Skill.DEFENSE,
            Skill.MAGIC, Skill.RANGED, Skill.PRAYER,
            Skill.WOODCUTTING, Skill.MINING, Skill.FISHING
        ).forEach { skill ->
            val level = getLevel(skill.ordinal)
            val currentXp = getXp(skill.ordinal)
            println("  ${skill.name}: Level $level (${format(currentXp)} XP)")
        }
    }

    override suspend fun loop() {
        // Just wait - we handle everything in onEvent
        delay(1000)

        // Every 30 seconds, print a summary
        if (System.currentTimeMillis() % 30000 < 1000) {
            printSummary()
        }
    }

    override fun onEvent(event: Event) {
        if (event is XPDrop) {
            // Track the XP gain
            val currentGain = xpGains.getOrDefault(event.skill, 0)
            xpGains[event.skill] = currentGain + event.gainedXp

            // Log the drop
            val totalGain = xpGains[event.skill] ?: 0
            val newLevel = getLevel(event.skill.ordinal)

            println("⬆ ${event.skill.name} +${event.gainedXp} XP (Total: +${format(totalGain)}, Level: $newLevel)")

            // Calculate XP/hour
            val hoursElapsed = (System.currentTimeMillis() - startTime) / 3600000.0
            if (hoursElapsed > 0) {
                val xpPerHour = (totalGain / hoursElapsed).toInt()
                println("   XP/hr: ${format(xpPerHour)}")
            }
        }
    }

    private fun printSummary() {
        if (xpGains.isEmpty()) return

        println("\n=== XP Summary ===")
        xpGains.entries.sortedByDescending { it.value }.forEach { (skill, gainedXp) ->
            val hoursElapsed = (System.currentTimeMillis() - startTime) / 3600000.0
            val xpPerHour = if (hoursElapsed > 0) (gainedXp / hoursElapsed).toInt() else 0
            println("  ${skill.name}: +${format(gainedXp)} XP (${format(xpPerHour)}/hr)")
        }
        println("==================\n")
    }

    override fun onStop() {
        println("=== Skill Monitor Stopped ===")
        printSummary()

        val runtime = (System.currentTimeMillis() - startTime) / 1000
        val hours = runtime / 3600
        val minutes = (runtime % 3600) / 60
        val seconds = runtime % 60
        println("Total runtime: ${hours}h ${minutes}m ${seconds}s")
    }
}

// Return the script instance
SkillMonitor()
