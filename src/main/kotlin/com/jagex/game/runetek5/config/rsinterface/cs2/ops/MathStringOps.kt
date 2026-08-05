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
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
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
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = string_2!!.indexOf(string_3!!, value)
    }

    @JvmStatic
    fun scale(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val long_2 = executor.intStack[executor.intStackPtr]
        val long_4 = executor.intStack[executor.intStackPtr + 1]
        val long_6 = executor.intStack[executor.intStackPtr + 2]
        executor.intStack[++executor.intStackPtr - 1] = (long_6.toLong() * long_2.toLong() / long_4.toLong()).toInt()
    }

    @JvmStatic
    fun multiply(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_3 * i_2
    }

    @JvmStatic
    fun addPercent(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val long_2 = executor.intStack[executor.intStackPtr]
        val long_4 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = (long_2 + long_2.toLong() * long_4.toLong() / 100L).toInt()
    }

    @JvmStatic
    fun stringwidth(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val value = executor.intStack[--executor.intStackPtr]
        val fontmetrics4: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, value)
        assert(fontmetrics4 != null)
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics4!!.getTextWidth(text, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = Math.min(i_2, i_3)
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
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + string_3
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
        val i_3 = executor.intStack[executor.intStackPtr]
        val i_4 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = text!!.indexOf(i_3.toChar(), i_4)
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
        val i_3 = executor.intStack[executor.intStackPtr]
        val i_4 = executor.intStack[executor.intStackPtr + 1]
        val fontmetrics5: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_4)
        assert(fontmetrics5 != null)
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics5!!.getTextWidthMultiline(text!!, i_3, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun pow(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Math.pow(i_2.toDouble(), i_3.toDouble()).toInt()
        }
    }

    @JvmStatic
    fun bitOr(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 or i_3
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 / i_3
    }

    @JvmStatic
    fun fromDate(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.getLocalDateString(CalenderUtil.convertDaysToMilliseconds(executor.intStack[--executor.intStackPtr]), Client.CURRENT_LANGUAGE!!.getValue())
    }

    @JvmStatic
    fun subtract(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 - i_3
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
        val stringbuilder3: StringBuilder? = StringBuilder(text!!.length)
        var flag = false
        for (value in 0 until text!!.length) {
            val var_6: Char = text!![value]
            if (var_6 == '<') {
                flag = true
            } else if (var_6 == '>') {
                flag = false
            } else if (!flag) {
                stringbuilder3!!.append(var_6)
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = stringbuilder3!!.toString()
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = if ((i_2 and (1 shl i_3)) != 0) 1 else 0
    }

    @JvmStatic
    fun bitAnd(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 and i_3
    }

    @JvmStatic
    fun setBit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 or (1 shl i_3)
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
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (executor.intStack[--executor.intStackPtr] == 1) {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3
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
        val i_3 = executor.intStack[executor.intStackPtr]
        val i_4 = executor.intStack[executor.intStackPtr + 1]
        val fontmetrics5: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_4)
        assert(fontmetrics5 != null)
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics5!!.getLineCount(text!!, i_3, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun modulo(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 % i_3
    }

    @JvmStatic
    fun formatDatetimeFromMinutes(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.getLanguageTimestring(executor.intStack[--executor.intStackPtr] * 60000L, Client.CURRENT_LANGUAGE!!.getValue()) + " UTC"
    }

    @JvmStatic
    fun powInverse(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else if (i_3 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = Integer.MAX_VALUE
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Math.pow(i_2.toDouble(), 1.0 / i_3).toInt()
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 > 700 || i_3 > 700) {
            executor.intStack[++executor.intStackPtr - 1] = 256
        }
        val d_4 = (Math.random() * (i_3 + i_2) - i_2 + 800.0) / 100.0
        executor.intStack[++executor.intStackPtr - 1] = (Math.pow(2.0, d_4) + 0.5).toInt()
    }

    @JvmStatic
    fun textGender(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null && PlayerUpdate.MY_PLAYER!!.playerAppearance!!.female) {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2
        }
    }

    @JvmStatic
    fun clearBit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 and -1 - ((1 shl i_3))
    }

    @JvmStatic
    fun add(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 + i_3
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = Math.max(i_2, i_3)
    }

}
