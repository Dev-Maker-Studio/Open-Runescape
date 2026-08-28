package com.jagex.game.runetek5.config.rsinterface.cs2.ops

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.login.AccountCreationResponseOpcodes
import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.CreateAccountRequest
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.client.window.DisplayModeDetails
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.clanvar.ClanVarType
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsType
import com.jagex.game.runetek5.config.enumtype.EnumType
import com.jagex.game.runetek5.config.material.texture.ColorImageCache
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.param.ParamType
import com.jagex.game.runetek5.config.quest.QuestType
import com.jagex.game.runetek5.config.quickchat.QuickChatCategoryType
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageType
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.IFEvents
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerType
import com.jagex.game.runetek5.config.sequence.InterfaceSequence
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapCategory
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay
import com.jagex.game.runetek5.graphics.model.MediaType
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY
import com.jagex.game.runetek5.interaction.Friend
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.Ignore
import com.jagex.game.runetek5.interaction.IgnoreList
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.ChatLine
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketDecoder
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.network.world.WorldDescriptor
import com.jagex.game.runetek5.rendering.MessageBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.*
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.VorbisSoundManager
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.ProfileCpu
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.*
import com.jagex.game.runetek5.vardomain.VarProviders
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2CallFrame
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode

object MathStringOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.ADD -> add(exec)
            ClientScriptOpCode.SUB -> subtract(exec)
            ClientScriptOpCode.MULTIPLY -> multiply(exec)
            ClientScriptOpCode.DIVIDE -> divide(exec)
            ClientScriptOpCode.RANDOM -> random(exec)
            ClientScriptOpCode.RANDOMINC -> randomInclusive(exec)
            ClientScriptOpCode.INTERPOLATE -> interpolate(exec)
            ClientScriptOpCode.ADDPERCENT -> addPercent(exec)
            ClientScriptOpCode.SETBIT -> setBit(exec)
            ClientScriptOpCode.CLEARBIT -> clearBit(exec)
            ClientScriptOpCode.TESTBIT -> bitFlagged(exec)
            ClientScriptOpCode.MODULO -> modulo(exec)
            ClientScriptOpCode.POW -> pow(exec)
            ClientScriptOpCode.INVPOW -> powInverse(exec)
            ClientScriptOpCode.AND -> bitAnd(exec)
            ClientScriptOpCode.OR -> bitOr(exec)
            ClientScriptOpCode.MIN -> min(exec)
            ClientScriptOpCode.MAX -> max(exec)
            ClientScriptOpCode.SCALE -> scale(exec)
            ClientScriptOpCode.RANDOM_SOUND_PITCH -> randSoundPitch(exec)
            ClientScriptOpCode.HSVTORGB -> hsvToRGB(exec)
            ClientScriptOpCode.NOT -> bitNot(exec)
            ClientScriptOpCode.APPEND_NUM -> strAppendNum(exec)
            ClientScriptOpCode.APPEND -> strAppend(exec)
            ClientScriptOpCode.APPEND_SIGNNUM -> strAppendSigNum(exec)
            ClientScriptOpCode.GET_COL_TAG -> getColTag(exec)
            ClientScriptOpCode.LOWERCASE -> lowerString(exec)
            ClientScriptOpCode.FROMDATE -> fromDate(exec)
            ClientScriptOpCode.TEXT_GENDER -> textGender(exec)
            ClientScriptOpCode.TOSTRING -> CS2SharedOps.toString(exec)
            ClientScriptOpCode.COMPARE -> stringEqual(exec)
            ClientScriptOpCode.PARAHEIGHT -> paraheight(exec)
            ClientScriptOpCode.PARAWIDTH -> paraWidth(exec)
            ClientScriptOpCode.TEXT_SWITCH -> textSwitch(exec)
            ClientScriptOpCode.ESCAPE -> escape(exec)
            ClientScriptOpCode.APPEND_CHAR -> appendChar(exec)
            ClientScriptOpCode.CHAR_ISPRINTABLE -> charIsPrintable(exec)
            ClientScriptOpCode.CHAR_ISALPHANUMERIC -> charIsAlphaNumeric(exec)
            ClientScriptOpCode.CHAR_ISALPHA -> charIsalpha(exec)
            ClientScriptOpCode.CHAR_ISNUMERIC -> charIsnumeric(exec)
            ClientScriptOpCode.STRING_LENGTH -> stringLength(exec)
            ClientScriptOpCode.SUBSTRING -> CS2SharedOps.substring(exec)
            ClientScriptOpCode.REMOVETAGS -> removetags(exec)
            ClientScriptOpCode.STRING_INDEXOF_CHAR -> stringIndexOfChar(exec)
            ClientScriptOpCode.STRING_INDEXOF_STRING -> stringIndexOfString(exec)
            ClientScriptOpCode.CHAR_TOLOWERCASE -> charToLowerCase(exec)
            ClientScriptOpCode.CHAR_TOUPPERCASE -> charToUpperCase(exec)
            ClientScriptOpCode.TOSTRING_LOCALISED -> toStringLocalised(exec)
            ClientScriptOpCode.STRINGWIDTH -> stringwidth(exec)
            ClientScriptOpCode.FORMAT_DATETIME_FROM_MINUTES -> formatDatetimeFromMinutes(exec)
            ClientScriptOpCode.CLANFORUMQFC_TOSTRING -> clanForumQfcToString(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by MathStringOps")
        }
    }

    @JvmStatic
    fun stringIndexOfString(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val text: String? = executor.stringStack[executor.stringStackPtr] as String?
        val search: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = text!!.indexOf(search!!, value)
    }

    @JvmStatic
    fun scale(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val numerator = executor.intStack[executor.intStackPtr]
        val denominator = executor.intStack[executor.intStackPtr + 1]
        val value = executor.intStack[executor.intStackPtr + 2]
        executor.intStack[++executor.intStackPtr - 1] = (value.toLong() * numerator.toLong() / denominator.toLong()).toInt()
    }

    @JvmStatic
    fun multiply(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val lhs = executor.intStack[executor.intStackPtr]
        val rhs = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = rhs * lhs
    }

    @JvmStatic
    fun addPercent(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val value = executor.intStack[executor.intStackPtr]
        val percent = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = (value + value.toLong() * percent.toLong() / 100L).toInt()
    }

    @JvmStatic
    fun stringwidth(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val value = executor.intStack[--executor.intStackPtr]
        val fontMetrics: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, value)
        assert(fontMetrics != null)
        executor.intStack[++executor.intStackPtr - 1] = fontMetrics!!.getTextWidth(text, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun stringLength(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (text != null) {
            executor.intStack[++executor.intStackPtr - 1] = text!!.length
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun randomInclusive(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (Math.random() * (value + 1)).toInt()
    }

    @JvmStatic
    fun min(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val a = executor.intStack[executor.intStackPtr]
        val b = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = Math.min(a, b)
    }

    @JvmStatic
    fun charIsalpha(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (StaticMethods.isAlphabetic(value.toChar())) 1 else 0
    }

    @JvmStatic
    fun random(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (Math.random() * value).toInt()
    }

    @JvmStatic
    fun strAppend(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val text: String? = executor.stringStack[executor.stringStackPtr] as String?
        val suffix: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = text + suffix
    }

    @JvmStatic
    fun toStringLocalised(executor: CS2Executor) {
        val flag = executor.intStack[--executor.intStackPtr] != 0
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = SystemInfo.getCacheCompletionPercent(value.toLong(), 0, flag, Client.CURRENT_LANGUAGE!!)
    }

    @JvmStatic
    fun stringIndexOfChar(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        val charCode = executor.intStack[executor.intStackPtr]
        val fromIndex = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = text!!.indexOf(charCode.toChar(), fromIndex)
    }

    @JvmStatic
    fun charIsPrintable(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (StaticMethods.isPrintableChar(value.toChar())) 1 else 0
    }

    @JvmStatic
    fun clanForumQfcToString(executor: CS2Executor) {
        val longValue = executor.longStack[--executor.longStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = if (longValue == -1L) "" else java.lang.Long.toString(longValue, 36).uppercase()
    }

    @JvmStatic
    fun paraWidth(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        val width = executor.intStack[executor.intStackPtr]
        val fontId = executor.intStack[executor.intStackPtr + 1]
        val fontMetrics: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, fontId)
        assert(fontMetrics != null)
        executor.intStack[++executor.intStackPtr - 1] = fontMetrics!!.getTextWidthMultiline(text!!, width, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun pow(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val base = executor.intStack[executor.intStackPtr]
        val exponent = executor.intStack[executor.intStackPtr + 1]
        if (base == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Math.pow(base.toDouble(), exponent.toDouble()).toInt()
        }
    }

    @JvmStatic
    fun bitOr(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val lhs = executor.intStack[executor.intStackPtr]
        val rhs = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = lhs or rhs
    }

    @JvmStatic
    fun stringEqual(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStack[++executor.intStackPtr - 1] = TextUtils.compareLocaleAware(executor.stringStack[executor.stringStackPtr] as CharSequence, executor.stringStack[executor.stringStackPtr + 1] as CharSequence, Client.CURRENT_LANGUAGE!!)
    }

    @JvmStatic
    fun charIsAlphaNumeric(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (TextUtils.isAlphabeticalOrNumeric(value.toChar())) 1 else 0
    }

    @JvmStatic
    fun divide(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val dividend = executor.intStack[executor.intStackPtr]
        val divisor = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = dividend / divisor
    }

    @JvmStatic
    fun fromDate(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.getLocalDateString(CalenderUtil.convertDaysToMilliseconds(executor.intStack[--executor.intStackPtr]), Client.CURRENT_LANGUAGE!!.getValue())
    }

    @JvmStatic
    fun subtract(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val lhs = executor.intStack[executor.intStackPtr]
        val rhs = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = lhs - rhs
    }

    @JvmStatic
    fun strAppendSigNum(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = text + TextUtils.createNumberCenteredWithinText(value, true)
    }

    @JvmStatic
    fun removetags(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val builder: StringBuilder? = StringBuilder(text!!.length)
        var flag = false
        for (value in 0 until text!!.length) {
            val ch: Char = text!![value]
            if (ch == '<') {
                flag = true
            } else if (ch == '>') {
                flag = false
            } else if (!flag) {
                builder!!.append(ch)
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = builder!!.toString()
    }

    @JvmStatic
    fun appendChar(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val value = executor.intStack[--executor.intStackPtr]
        if (value == -1) {
            throw RuntimeException("")
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = text + value.toChar()
        }
    }

    @JvmStatic
    fun bitFlagged(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val flags = executor.intStack[executor.intStackPtr]
        val bit = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = if ((flags and (1 shl bit)) != 0) 1 else 0
    }

    @JvmStatic
    fun bitAnd(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val lhs = executor.intStack[executor.intStackPtr]
        val rhs = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = lhs and rhs
    }

    @JvmStatic
    fun setBit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val flags = executor.intStack[executor.intStackPtr]
        val bit = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = flags or (1 shl bit)
    }

    @JvmStatic
    fun charIsnumeric(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (Utils.isNumber(value.toChar())) 1 else 0
    }

    @JvmStatic
    fun getColTag(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TextUtils.setTextColor(value)
    }

    @JvmStatic
    fun lowerString(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = text!!.lowercase()
    }

    @JvmStatic
    fun textSwitch(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val trueText: String? = executor.stringStack[executor.stringStackPtr] as String?
        val falseText: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (executor.intStack[--executor.intStackPtr] == 1) {
            executor.stringStack[++executor.stringStackPtr - 1] = trueText
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = falseText
        }
    }

    @JvmStatic
    fun charToUpperCase(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Character.toUpperCase(value.toChar()).code
    }

    @JvmStatic
    fun paraheight(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        val width = executor.intStack[executor.intStackPtr]
        val fontId = executor.intStack[executor.intStackPtr + 1]
        val fontMetrics: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, fontId)
        assert(fontMetrics != null)
        executor.intStack[++executor.intStackPtr - 1] = fontMetrics!!.getLineCount(text!!, width, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun modulo(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val dividend = executor.intStack[executor.intStackPtr]
        val divisor = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = dividend % divisor
    }

    @JvmStatic
    fun formatDatetimeFromMinutes(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.getLanguageTimestring(executor.intStack[--executor.intStackPtr] * 60000L, Client.CURRENT_LANGUAGE!!.getValue()) + " UTC"
    }

    @JvmStatic
    fun powInverse(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val base = executor.intStack[executor.intStackPtr]
        val root = executor.intStack[executor.intStackPtr + 1]
        if (base == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else if (root == 0) {
            executor.intStack[++executor.intStackPtr - 1] = Integer.MAX_VALUE
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Math.pow(base.toDouble(), 1.0 / root).toInt()
        }
    }

    @JvmStatic
    fun hsvToRGB(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(value).toInt() and 0xffff]
    }

    @JvmStatic
    fun escape(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = TextUtils.atLtGtTags(text!!)
    }

    @JvmStatic
    fun bitNot(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = value.inv()
    }

    @JvmStatic
    fun randSoundPitch(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val pitchDown = executor.intStack[executor.intStackPtr]
        val pitchUp = executor.intStack[executor.intStackPtr + 1]
        if (pitchDown > 700 || pitchUp > 700) {
            executor.intStack[++executor.intStackPtr - 1] = 256
            return
        }
        val octaves = (Math.random() * (pitchUp + pitchDown) - pitchDown + 800.0) / 100.0
        executor.intStack[++executor.intStackPtr - 1] = (Math.pow(2.0, octaves) + 0.5).toInt()
    }

    @JvmStatic
    fun textGender(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val maleText: String? = executor.stringStack[executor.stringStackPtr] as String?
        val femaleText: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null && PlayerUpdate.MY_PLAYER!!.playerAppearance!!.female) {
            executor.stringStack[++executor.stringStackPtr - 1] = femaleText
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = maleText
        }
    }

    @JvmStatic
    fun clearBit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val flags = executor.intStack[executor.intStackPtr]
        val bit = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = flags and -1 - ((1 shl bit))
    }

    @JvmStatic
    fun add(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val lhs = executor.intStack[executor.intStackPtr]
        val rhs = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = lhs + rhs
    }

    @JvmStatic
    fun charToLowerCase(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Character.toLowerCase(value.toChar()).code
    }

    @JvmStatic
    fun interpolate(executor: CS2Executor) {
        executor.intStackPtr -= 5
        val a = executor.intStack[executor.intStackPtr]
        val b = executor.intStack[executor.intStackPtr + 1]
        val c = executor.intStack[executor.intStackPtr + 2]
        val d = executor.intStack[executor.intStackPtr + 3]
        val e = executor.intStack[executor.intStackPtr + 4]
        executor.intStack[++executor.intStackPtr - 1] = a + (e - c) * (b - a) / (d - c)
    }

    @JvmStatic
    fun strAppendNum(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = text + value
    }

    @JvmStatic
    fun max(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val a = executor.intStack[executor.intStackPtr]
        val b = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = Math.max(a, b)
    }

}
