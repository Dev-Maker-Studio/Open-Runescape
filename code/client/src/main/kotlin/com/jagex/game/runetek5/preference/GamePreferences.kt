package com.jagex.game.runetek5.preference

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.preference.impl.*
import com.jagex.game.runetek5.systeminfo.ProcessorSpecs
import com.jagex.game.runetek5.systeminfo.SystemInfo
import kotlin.math.max

class GamePreferences : Node<GamePreferences> {
    @JvmField
    var activeToolkit: ToolkitPreference? = null

    @JvmField
    var antiAliasing: AntiAliasingPreference? = null

    @JvmField
    var antiAliasingPreview: AntiAliasingPreference? = null

    @JvmField
    var bloom: BloomPreference? = null
    @JvmField
    var brightness: BrightnessPreference? = null
    @JvmField
    var mapSize: MapSizePreference? = null
    @JvmField
    var buildArea: BuildAreaPreference? = null
    @JvmField
    var flickeringEffects: FlickeringEffectPreference? = null
    @JvmField
    var fog: FogPreference? = null
    @JvmField
    var goundBlending: GroundBlendingPreference? = null
    @JvmField
    var groundDecoration: GroundDecorationPreference? = null
    @JvmField
    var idleAnimations: IdleAnimationsPreference? = null
    @JvmField
    var lightDetail: LightDetailPreference? = null
    @JvmField
    var sceneryShadows: SceneryShadowPreference? = null
    @JvmField
    var defaultToolkit: DefaultToolkitPreference? = null
    @JvmField
    var particles: ParticlePreference? = null
    @JvmField
    var removeRoofs: RoofPreference? = null
    @JvmField
    var removeRoofsOptionOverride: RoofPreference? = null
    @JvmField
    var maxScreenSize: MaxScreenSizePreference? = null
    @JvmField
    var skyboxes: SkyboxPreference? = null
    @JvmField
    var characterShadows: CharacterShadowPreference? = null
    @JvmField
    var textures: TexturesPreference? = null
    @JvmField
    var toolkit: ToolkitPreference? = null
    @JvmField
    var water: WaterPreference? = null
    @JvmField
    var fixedScreen: ScreenSizePreference? = null
    @JvmField
    var fullScreen: ScreenSizePreference? = null
    @JvmField
    var customCursor: CustomCursorsPreference? = null
    @JvmField
    var graphics: GraphicsPreference? = null
    @JvmField
    var cpu: CpuMaxMemoryPreference? = null
    @JvmField
    var gametipStartStagePreference: GameTipPreference? = null
    @JvmField
    var safeMode: SafeModePreference? = null
    @JvmField
    var soundEffectVolume: SoundLevelPreference? = null
    @JvmField
    var ambientSoundVolume: SoundLevelPreference? = null
    @JvmField
    var voiceOverVolume: SoundLevelPreference? = null
    @JvmField
    var musicVolume: SoundLevelPreference? = null
    @JvmField
    var soundLevel: SoundLevelPreference? = null
    @JvmField
    var monoStereo: MonoStereoPreference? = null
    @JvmField
    val game: Game
    @JvmField
    val processorSpecs: ProcessorSpecs

    constructor(buffer: Packet?, game: Game) {
        this.game = game
        this.processorSpecs = ProcessorSpecs(
            GameShell.maxMemory,
            GameShell.availableProcessors,
            SystemInfo.OS_ARCH.indexOf("arm") != -1
        )
        this.activeToolkit = ToolkitPreference(0, this)
        decode(buffer)
    }

    constructor(game: Game) {
        this.game = game
        this.processorSpecs = ProcessorSpecs(
            GameShell.maxMemory,
            GameShell.availableProcessors,
            SystemInfo.OS_ARCH.lowercase().indexOf("arm") != -1
        )
        this.activeToolkit = ToolkitPreference(0, this)
        setDefaults(true)
    }

    fun decode(stream: Packet?) {
        if (stream != null && stream.buffer != null) {
            val version = stream.readUByte()
            if (version < 23) {
                try {
                    decodeDeprecatedVersion(stream, version)
                } catch (exception: Exception) {
                    setDefaults(true)
                }
                setDefaults(false)
            } else if (version > 28) {
                setDefaults(true)
            } else {
                antiAliasing = AntiAliasingPreference(stream.readUByte(), this)
                antiAliasingPreview = AntiAliasingPreference(antiAliasing!!.getValue(), this)
                bloom = BloomPreference(stream.readUByte(), this)
                brightness = BrightnessPreference(stream.readUByte(), this)
                mapSize = MapSizePreference(stream.readUByte(), this)
                if (version >= 27) {
                    buildArea = BuildAreaPreference(stream.readUByte(), this)
                }

                flickeringEffects = FlickeringEffectPreference(stream.readUByte(), this)
                fog = FogPreference(stream.readUByte(), this)
                goundBlending = GroundBlendingPreference(stream.readUByte(), this)
                groundDecoration = GroundDecorationPreference(stream.readUByte(), this)
                idleAnimations = IdleAnimationsPreference(stream.readUByte(), this)
                lightDetail = LightDetailPreference(stream.readUByte(), this)
                sceneryShadows = SceneryShadowPreference(stream.readUByte(), this)
                if (version >= 24) {
                    defaultToolkit = DefaultToolkitPreference(stream.readUByte(), this)
                }

                particles = ParticlePreference(stream.readUByte(), this)
                removeRoofs = RoofPreference(stream.readUByte(), this)
                removeRoofsOptionOverride = RoofPreference(removeRoofs!!.getValue(), this)
                maxScreenSize = MaxScreenSizePreference(stream.readUByte(), this)
                if (version >= 25) {
                    skyboxes = SkyboxPreference(stream.readUByte(), this)
                }

                characterShadows = CharacterShadowPreference(stream.readUByte(), this)
                if (version <= 25) {
                    stream.currentPosition++
                }

                textures = TexturesPreference(stream.readUByte(), this)
                toolkit = ToolkitPreference(stream.readUByte(), this)
                activeToolkit = ToolkitPreference(toolkit!!.getValue(), this)
                stream.readUByte()
                water = WaterPreference(stream.readUByte(), this)
                fixedScreen = ScreenSizePreference(stream.readUByte(), this)
                fullScreen = ScreenSizePreference(fixedScreen!!.getValue(), this)
                customCursor = CustomCursorsPreference(stream.readUByte(), this)
                graphics = GraphicsPreference(stream.readUByte(), this)
                cpu = CpuMaxMemoryPreference(stream.readUByte(), this)
                gametipStartStagePreference = GameTipPreference(stream.readUByte(), this)
                safeMode = SafeModePreference(stream.readUByte(), this)
                if (version == 26 || version == 27) {
                    stream.readUByte()
                }

                soundEffectVolume = SoundLevelPreference(stream.readUByte(), this)
                ambientSoundVolume = SoundLevelPreference(stream.readUByte(), this)
                voiceOverVolume = SoundLevelPreference(stream.readUByte(), this)
                musicVolume = SoundLevelPreference(stream.readUByte(), this)
                soundLevel = SoundLevelPreference(stream.readUByte(), this)
                monoStereo = MonoStereoPreference(stream.readUByte(), this)
                setDefaults(false)
            }
        } else {
            setDefaults(true)
        }

        ensureValid()
    }

    fun setDefaults(override: Boolean) {
        if (override || antiAliasing == null) {
            antiAliasing = AntiAliasingPreference(this)
        }

        if (override || antiAliasingPreview == null) {
            antiAliasingPreview = AntiAliasingPreference(antiAliasing!!.getValue(), this)
        }

        if (override || bloom == null) {
            bloom = BloomPreference(this)
        }

        if (override || brightness == null) {
            brightness = BrightnessPreference(this)
        }

        if (override || mapSize == null) {
            mapSize = MapSizePreference(this)
        }

        if (override || buildArea == null) {
            buildArea = BuildAreaPreference(this)
        }

        if (override || flickeringEffects == null) {
            flickeringEffects = FlickeringEffectPreference(this)
        }

        if (override || fog == null) {
            fog = FogPreference(this)
        }

        if (override || goundBlending == null) {
            goundBlending = GroundBlendingPreference(this)
        }

        if (override || groundDecoration == null) {
            groundDecoration = GroundDecorationPreference(this)
        }

        if (override || idleAnimations == null) {
            idleAnimations = IdleAnimationsPreference(this)
        }

        if (override || lightDetail == null) {
            lightDetail = LightDetailPreference(this)
        }

        if (override || sceneryShadows == null) {
            sceneryShadows = SceneryShadowPreference(this)
        }

        if (override || defaultToolkit == null) {
            defaultToolkit = DefaultToolkitPreference(this)
        }

        if (override || particles == null) {
            particles = ParticlePreference(this)
        }

        if (override || removeRoofs == null) {
            removeRoofs = RoofPreference(this)
        }

        if (override || removeRoofsOptionOverride == null) {
            removeRoofsOptionOverride = RoofPreference(removeRoofs!!.getValue(), this)
        }

        if (override || maxScreenSize == null) {
            maxScreenSize = MaxScreenSizePreference(this)
        }

        if (override || skyboxes == null) {
            skyboxes = SkyboxPreference(this)
        }

        if (override || characterShadows == null) {
            characterShadows = CharacterShadowPreference(this)
        }

        if (override || textures == null) {
            textures = TexturesPreference(this)
        }

        if (override || toolkit == null) {
            toolkit = ToolkitPreference(this)
        }

        if (override || activeToolkit == null) {
            activeToolkit = ToolkitPreference(toolkit!!.getValue(), this)
        }

        if (override || water == null) {
            water = WaterPreference(this)
        }

        if (override || fixedScreen == null) {
            fixedScreen = ScreenSizePreference(this)
        }

        if (override || fullScreen == null) {
            fullScreen = ScreenSizePreference(fixedScreen!!.getValue(), this)
        }

        if (override || customCursor == null) {
            customCursor = CustomCursorsPreference(this)
        }

        if (override || graphics == null) {
            graphics = GraphicsPreference(this)
        }

        if (override || cpu == null) {
            cpu = CpuMaxMemoryPreference(this)
        }

        if (override || gametipStartStagePreference == null) {
            gametipStartStagePreference = GameTipPreference(this)
        }

        if (override || safeMode == null) {
            safeMode = SafeModePreference(this)
        }

        if (override || soundEffectVolume == null) {
            soundEffectVolume = SoundLevelPreference(this)
        }

        if (override || ambientSoundVolume == null) {
            ambientSoundVolume = SoundLevelPreference(this)
        }

        if (override || voiceOverVolume == null) {
            voiceOverVolume = SoundLevelPreference(this)
        }

        if (override || musicVolume == null) {
            musicVolume = SoundLevelPreference(this)
        }

        if (override || soundLevel == null) {
            soundLevel = SoundLevelPreference(this)
        }

        if (override || monoStereo == null) {
            monoStereo = MonoStereoPreference(this)
        }
    }

    fun decodeDeprecatedVersion(stream: Packet, version: Int) {
        brightness = BrightnessPreference(stream.readUByte(), this)
        stream.currentPosition++
        removeRoofs = RoofPreference(stream.readUByte() + 1, this)
        groundDecoration = GroundDecorationPreference(stream.readUByte(), this)
        stream.currentPosition++
        idleAnimations = IdleAnimationsPreference(stream.readUByte(), this)
        flickeringEffects = FlickeringEffectPreference(stream.readUByte(), this)
        stream.readUByte()
        characterShadows = CharacterShadowPreference(stream.readUByte(), this)
        val i4 = stream.readUByte()
        var i5 = 0
        if (version >= 17) {
            i5 = stream.readUByte()
        }

        sceneryShadows = SceneryShadowPreference(max(i4, i5), this)
        var highDetailLight = true
        var highDetailShadows = true
        if (version >= 2) {
            highDetailLight = stream.readUByte() == 1
            if (version >= 17) {
                highDetailShadows = stream.readUByte() == 1
            }
        } else {
            highDetailLight = stream.readUByte() == 1
            stream.readUByte()
        }

        lightDetail = LightDetailPreference(if (highDetailLight or highDetailShadows) 1 else 0, this)
        water = WaterPreference(stream.readUByte(), this)
        fog = FogPreference(stream.readUByte(), this)
        antiAliasing = AntiAliasingPreference(stream.readUByte(), this)
        monoStereo = MonoStereoPreference(stream.readUByte(), this)
        soundEffectVolume = SoundLevelPreference(stream.readUByte(), this)
        if (version >= 20) {
            voiceOverVolume = SoundLevelPreference(stream.readUByte(), this)
        } else {
            voiceOverVolume = SoundLevelPreference(soundEffectVolume!!.getValue(), this)
        }

        musicVolume = SoundLevelPreference(stream.readUByte(), this)
        ambientSoundVolume = SoundLevelPreference(stream.readUByte(), this)
        if (version >= 21) {
            soundLevel = SoundLevelPreference(stream.readUByte(), this)
        } else {
            soundLevel = SoundLevelPreference(musicVolume!!.getValue(), this)
        }

        if (version >= 1) {
            stream.readUShort()
            stream.readUShort()
        }

        if (version >= 3 && version < 6) {
            stream.readUByte()
        }

        if (version >= 4) {
            particles = ParticlePreference(stream.readUByte(), this)
        }

        stream.readInt()
        if (version >= 6) {
            fixedScreen = ScreenSizePreference(stream.readUByte(), this)
        }

        if (version >= 7) {
            safeMode = SafeModePreference(stream.readUByte(), this)
        }

        if (version >= 8) {
            stream.readUByte()
        }

        if (version >= 9) {
            mapSize = MapSizePreference(stream.readUByte(), this)
        }

        if (version >= 10) {
            bloom = BloomPreference(stream.readUByte(), this)
        }

        if (version >= 11) {
            customCursor = CustomCursorsPreference(stream.readUByte(), this)
        }

        if (version >= 12) {
            idleAnimations = IdleAnimationsPreference(stream.readUByte(), this)
        }

        if (version >= 13) {
            goundBlending = GroundBlendingPreference(stream.readUByte(), this)
        }

        if (version >= 14) {
            toolkit = ToolkitPreference(stream.readUByte(), this)
        }

        if (version >= 15) {
            cpu = CpuMaxMemoryPreference(stream.readUByte(), this)
        }

        if (version >= 16) {
            textures = TexturesPreference(stream.readUByte(), this)
        }

        if (version >= 18) {
            graphics = GraphicsPreference(stream.readUByte(), this)
        }

        if (version >= 19) {
            maxScreenSize = MaxScreenSizePreference(stream.readUByte(), this)
        }

        if (version >= 22) {
            gametipStartStagePreference = GameTipPreference(stream.readUByte(), this)
        }
    }

    fun encode(): Packet {
        val stream = Packet(40)
        stream.writeByte(28)
        stream.writeByte(antiAliasing!!.getValue())
        stream.writeByte(bloom!!.getValue())
        stream.writeByte(brightness!!.getValue())
        stream.writeByte(mapSize!!.getValue())
        stream.writeByte(buildArea!!.getValue())
        stream.writeByte(flickeringEffects!!.getValue())
        stream.writeByte(fog!!.getValue())
        stream.writeByte(goundBlending!!.getValue())
        stream.writeByte(groundDecoration!!.getValue())
        stream.writeByte(idleAnimations!!.getValue())
        stream.writeByte(lightDetail!!.getValue())
        stream.writeByte(sceneryShadows!!.getValue())
        stream.writeByte(defaultToolkit!!.getValue())
        stream.writeByte(particles!!.getValue())
        stream.writeByte(removeRoofs!!.getValue())
        stream.writeByte(maxScreenSize!!.getValue())
        stream.writeByte(skyboxes!!.getValue())
        stream.writeByte(characterShadows!!.getValue())
        stream.writeByte(textures!!.getValue())
        stream.writeByte(toolkit!!.getValue())
        stream.writeByte(0)
        stream.writeByte(water!!.getValue())
        stream.writeByte(fixedScreen!!.getValue())
        stream.writeByte(customCursor!!.getValue())
        stream.writeByte(graphics!!.getValue())
        stream.writeByte(cpu!!.getValue())
        stream.writeByte(gametipStartStagePreference!!.getValue())
        stream.writeByte(safeMode!!.getValue())
        stream.writeByte(soundEffectVolume!!.getValue())
        stream.writeByte(ambientSoundVolume!!.getValue())
        stream.writeByte(voiceOverVolume!!.getValue())
        stream.writeByte(musicVolume!!.getValue())
        stream.writeByte(soundLevel!!.getValue())
        stream.writeByte(monoStereo!!.getValue())
        return stream
    }

    fun ensureValid() {
        antiAliasing!!.ensureValid()
        antiAliasingPreview!!.ensureValid()
        bloom!!.ensureValid()
        brightness!!.ensureValid()
        mapSize!!.ensureValid()
        buildArea!!.ensureValid()
        flickeringEffects!!.ensureValid()
        fog!!.ensureValid()
        goundBlending!!.ensureValid()
        groundDecoration!!.ensureValid()
        idleAnimations!!.ensureValid()
        lightDetail!!.ensureValid()
        sceneryShadows!!.ensureValid()
        defaultToolkit!!.ensureValid()
        particles!!.ensureValid()
        removeRoofs!!.ensureValid()
        removeRoofsOptionOverride!!.ensureValid()
        maxScreenSize!!.ensureValid()
        skyboxes!!.ensureValid()
        characterShadows!!.ensureValid()
        textures!!.ensureValid()
        toolkit!!.ensureValid()
        activeToolkit!!.ensureValid()
        water!!.ensureValid()
        fixedScreen!!.ensureValid()
        fullScreen!!.ensureValid()
        customCursor!!.ensureValid()
        graphics!!.ensureValid()
        cpu!!.ensureValid()
        gametipStartStagePreference!!.ensureValid()
        safeMode!!.ensureValid()
        soundEffectVolume!!.ensureValid()
        ambientSoundVolume!!.ensureValid()
        voiceOverVolume!!.ensureValid()
        musicVolume!!.ensureValid()
        soundLevel!!.ensureValid()
        monoStereo!!.ensureValid()
    }

    fun getProcessorSpecs(): ProcessorSpecs {
        return processorSpecs
    }

    fun verifySettings(toolkitPreference: ToolkitPreference, toolkitChanged: Boolean) {
        toolkitPreference.setVerifySettings(toolkitChanged)
        ensureValid()
    }

    fun setValue(preference: Preference, value: Int) {
        preference.setValidatedValue(value)
        ensureValid()
    }

    fun getGame(): Game {
        return game
    }
}
