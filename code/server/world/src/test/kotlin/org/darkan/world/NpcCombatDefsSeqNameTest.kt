package org.darkan.world

import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions
import org.darkan.core.JsonFileManager
import org.darkan.core.gameval.Gameval
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The combat defs animation fields load from either form the tree keeps: a seq id, or the
 * sequence's gameval name (`SeqGamevalAdapter`, which `NPCCombatDefinitions` wires onto its four
 * anim fields). Runs against the real catalogs and skips itself when seq.json is not there, the
 * same guard GamevalTest uses; the third test also skips until the tree has a name in it.
 */
class NpcCombatDefsSeqNameTest {

    @Test
    fun `animation fields load from names and from ids`() {
        if (!Gameval.has(Gameval.SEQ)) return
        val named = Gameval.entries(Gameval.SEQ).entries.firstOrNull { it.value.matches(Regex("[a-z0-9_]+")) } ?: return
        val def = JsonFileManager.fromJSONString(
            """{"attackAnim":"${named.value}","defenceAnim":${named.key},"deathAnim":"${named.value}"}""",
            NPCCombatDefinitions::class.java,
        )
        assertEquals(named.key, def.getAttackEmote())
        assertEquals(named.key, def.getDefenceEmote())
        assertEquals(named.key, def.getDeathEmote())
    }

    @Test
    fun `an unresolvable animation name loads as minus one`() {
        if (!Gameval.has(Gameval.SEQ)) return
        val def = JsonFileManager.fromJSONString(
            """{"attackAnim":"no_such_seq_name"}""",
            NPCCombatDefinitions::class.java,
        )
        assertEquals(-1, def.getAttackEmote())
    }

    @Test
    fun `the written tree loads through the real loader`() {
        if (!Gameval.has(Gameval.SEQ)) return
        val root = File("data/npcs/combatdefs")
        if (!root.isDirectory) return
        // A name, not a quoted id and not a placeholder string - both of those resolve to nothing.
        val field = Regex("\"(attackAnim|defenceAnim|deathAnim|respawnAnim)\"\\s*:\\s*\"(?!seq_\\d+\")([a-z0-9_]+)\"")
        val (file, fieldName, seqName) = root.walkTopDown().asSequence()
            .filter { it.isFile && it.extension == "json" }
            .firstNotNullOfOrNull { f -> field.find(f.readText())?.let { Triple(f, it.groupValues[1], it.groupValues[2]) } }
            ?: return
        val def = assertNotNull(JsonFileManager.loadJsonFile(file, NPCCombatDefinitions::class.java))
        val loaded = when (fieldName) {
            "attackAnim" -> def.getAttackEmote()
            "defenceAnim" -> def.getDefenceEmote()
            "deathAnim" -> def.getDeathEmote()
            else -> def.getRespawnAnim()
        }
        assertEquals(Gameval.requireId(Gameval.SEQ, seqName), loaded)
    }
}