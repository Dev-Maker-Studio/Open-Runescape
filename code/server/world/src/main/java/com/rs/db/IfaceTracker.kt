package com.rs.db

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.InterfaceManager.Sub
import org.darkan.core.JsonFileManager
import org.darkan.core.Utils
import org.darkan.core.gameval.Gameval
import java.util.concurrent.ConcurrentHashMap

class IfaceTracker(private val player: Player) {
    private class Comp {
        var text: String? = null
        var hidden: Boolean? = null
        var item: IntArray? = null
        var model: Int? = null
        var graphic: Int? = null
        var events: IntArray? = null
        var pin = false
    }

    private val comps = ConcurrentHashMap<Int, Comp>()
    private val through = ConcurrentHashMap<Int, Boolean>()
    private var seen = 0L
    private var last = ""
    private var lastFlush = 0L
    private var dirty = false
    private var live = false
    private var n = 0L

    private fun comp(i: Int, c: Int) = comps.getOrPut(i shl 16 or c, ::Comp).also { dirty = true }

    fun text(i: Int, c: Int, v: String) { comp(i, c).text = v }
    fun hidden(i: Int, c: Int, v: Boolean) { comp(i, c).hidden = v }
    fun item(i: Int, c: Int, id: Int, amount: Int) { comp(i, c).item = intArrayOf(id, amount) }
    fun model(i: Int, c: Int, v: Int) { comp(i, c).model = v }
    fun graphic(i: Int, c: Int, v: Int) { comp(i, c).graphic = v }
    fun events(i: Int, c: Int, from: Int, to: Int, settings: Int) { comp(i, c).events = intArrayOf(from, to, settings) }

    fun opened(parent: Int, id: Int, clickThrough: Boolean) {
        through[parent] = clickThrough
        closed(id)
    }

    fun closed(id: Int) {
        comps.keys.removeIf { it ushr 16 == id }
        dirty = true
    }

    fun reset() {
        comps.clear()
        through.clear()
    }

    private val user get() = player.username

    private fun section(json: String?) = runCatching { JsonParser.parseString(json).asJsonObject.getAsJsonObject("interfaces") }.getOrNull()

    private fun obj(vararg p: Pair<String, Any?>) = JsonObject().also { o ->
        p.forEach { (k, v) ->
            when (v) {
                is Number -> o.addProperty(k, v)
                is Boolean -> o.addProperty(k, v)
                is String -> o.addProperty(k, v)
                is JsonObject -> o.add(k, v)
            }
        }
    }

    private fun compName(i: Int, c: Int) = Gameval.rawEntries(Gameval.COMPONENT)["$i:$c"]

    private fun state(id: Int): JsonObject? {
        val out = JsonObject()
        comps.entries.filter { it.key ushr 16 == id }.sortedBy { it.key }.forEach { (uid, s) ->
            val c = uid and 0xFFFF
            out.add(c.toString(), obj(
                "name" to compName(id, c), "text" to s.text, "hidden" to s.hidden, "model" to s.model, "graphic" to s.graphic,
                "item" to s.item?.let { obj("id" to it[0], "amount" to it[1]) },
                "events" to s.events?.let { obj("from" to it[0], "to" to it[1], "settings" to it[2]) },
                "pin" to s.pin.takeIf { it },
            ))
        }
        return out.takeIf { it.size() > 0 }
    }

    fun snapshot(): JsonObject {
        val im = player.interfaceManager
        val open = com.google.gson.JsonArray()
        im.opened().entries.sortedBy { it.key }.forEach { (uid, id) ->
            val pi = uid ushr 16
            val pc = uid and 0xFFFF
            val o = obj(
                "id" to id, "name" to Gameval.rawName(Gameval.INTERFACE, id),
                "parent" to obj("interface" to pi, "interfaceName" to Gameval.rawName(Gameval.INTERFACE, pi), "component" to pc, "componentName" to compName(pi, pc)),
                "clickThrough" to (through[uid] ?: true),
            )
            Sub.forHash(uid)?.let { o.addProperty("slot", it.name) }
            state(id)?.let { o.add("components", it) }
            open.add(o)
        }
        return JsonObject().apply {
            add("top", obj("id" to im.topInterface, "name" to Gameval.rawName(Gameval.INTERFACE, im.topInterface)))
            add("open", open)
        }
    }

    private fun parent(o: JsonObject): Pair<Int, Int>? {
        val im = player.interfaceManager
        o.get("slot")?.asString?.let { n -> Sub.entries.firstOrNull { it.name == n }?.let { return im.defaultTopInterface() to it.getComponent(player.resizeable()) } }
        o.getAsJsonObject("parent")?.let { return it.get("interface").asInt to it.get("component").asInt }
        return im.defaultTopInterface() to Sub.CENTRAL.getComponent(player.resizeable())
    }

    private fun comps(id: Int, o: JsonObject, all: Boolean) {
        val p = player.packets
        o.getAsJsonObject("components")?.entrySet()?.forEach { (k, v) ->
            val c = k.toIntOrNull() ?: return@forEach
            val s = v.asJsonObject
            val pin = s.get("pin")?.asBoolean == true
            if (!all && !pin) return@forEach
            val t = comps[id shl 16 or c]
            s.get("text")?.asString?.takeIf { it != t?.text }?.let { p.setIFText(id, c, it) }
            s.get("hidden")?.asBoolean?.takeIf { it != t?.hidden }?.let { p.setIFHidden(id, c, it) }
            s.get("model")?.asInt?.takeIf { it != t?.model }?.let { p.setIFModel(id, c, it) }
            s.get("graphic")?.asInt?.takeIf { it != t?.graphic }?.let { p.setIFGraphic(id, c, it) }
            s.getAsJsonObject("item")?.let { intArrayOf(it.get("id").asInt, it.get("amount")?.asInt ?: 1) }?.takeIf { !it.contentEquals(t?.item) }?.let { p.setIFItem(id, c, it[0], it[1]) }
            s.getAsJsonObject("events")?.let { intArrayOf(it.get("from").asInt, it.get("to").asInt, it.get("settings").asInt) }?.takeIf { !it.contentEquals(t?.events) }
                ?.let { p.setIFEvents(org.darkan.core.model.IFEvents(id, c, it[0], it[1], it[2])) }
            if (pin) comp(id, c).pin = true
        }
    }

    private fun apply(section: JsonObject, login: Boolean) {
        val im = player.interfaceManager
        val max = Utils.getInterfaceDefinitionsSize()
        section.getAsJsonArray("open")?.forEach { e -> runCatching {
            val o = e.asJsonObject
            val id = o.get("id")?.asInt ?: return@forEach
            if (max > 0 && id > max) return@forEach
            val (pi, pc) = parent(o) ?: return@forEach
            val fresh = im.opened()[pi shl 16 or pc] != id
            if (o.get("open")?.asBoolean == false) {
                if (!fresh) im.removeSubSpecific(pi, pc)
                return@forEach
            }
            if (fresh) {
                im.removeInterface(id)
                im.sendSubSpecific(o.get("clickThrough")?.asBoolean ?: (o.get("slot")?.asString != Sub.CENTRAL.name), pi, pc, id)
            }
            comps(id, o, !login || fresh)
        } }
        dirty = true
    }

    fun login() {
        if (!PlayerFiles.enabled) return
        live = true
        section(PlayerFiles.read(user))?.let { apply(it, true) }
        seen = PlayerFiles.stamp(user)
    }

    fun tick() {
        if (!live || n++ % 2L != 0L) return
        val stamp = PlayerFiles.stamp(user)
        if (stamp != seen) {
            seen = stamp
            section(PlayerFiles.read(user))?.let { apply(it, false) }
        }
        if (dirty && System.currentTimeMillis() - lastFlush > 2500) flush()
    }

    private fun flush() {
        if (PlayerFiles.stamp(user) != seen) return
        dirty = false
        lastFlush = System.currentTimeMillis()
        val snap = snapshot()
        val text = snap.toString()
        if (text == last) return
        val doc = runCatching { JsonParser.parseString(PlayerFiles.read(user)).asJsonObject }.getOrNull() ?: return
        doc.add("interfaces", snap)
        PlayerFiles.write(user, JsonFileManager.getGSON().toJson(doc))
        last = text
        seen = PlayerFiles.stamp(user)
    }

    fun inject(json: String): String {
        val doc = JsonParser.parseString(json).asJsonObject
        val keep = live && PlayerFiles.stamp(user) == seen
        val sec = (if (keep) runCatching { snapshot() }.getOrNull() else null) ?: section(PlayerFiles.read(user))
        sec?.let { doc.add("interfaces", it) }
        return JsonFileManager.getGSON().toJson(doc)
    }
}
