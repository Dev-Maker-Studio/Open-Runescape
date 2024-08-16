package com.jagex.game.runetek5.preference;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.preference.impl.AntiAliasingPreference;
import com.jagex.game.runetek5.preference.impl.BloomPreference;
import com.jagex.game.runetek5.preference.impl.BrightnessPreference;
import com.jagex.game.runetek5.preference.impl.CpuMaxMemoryPreference;
import com.jagex.game.runetek5.preference.impl.CharacterShadowPreference;
import com.jagex.game.runetek5.preference.impl.CustomCursorsPreference;
import com.jagex.game.runetek5.preference.impl.DefaultToolkitPreference;
import com.jagex.game.runetek5.preference.impl.FlickeringEffectPreference;
import com.jagex.game.runetek5.preference.impl.FogPreference;
import com.jagex.game.runetek5.preference.impl.GameTipPreference;
import com.jagex.game.runetek5.preference.impl.GraphicsPreference;
import com.jagex.game.runetek5.preference.impl.ToolkitPreference;
import com.jagex.game.runetek5.preference.impl.GroundBlendingPreference;
import com.jagex.game.runetek5.preference.impl.GroundDecorationPreference;
import com.jagex.game.runetek5.preference.impl.IdleAnimationsPreference;
import com.jagex.game.runetek5.preference.impl.LightDetailPreference;
import com.jagex.game.runetek5.preference.impl.MapSizePreference;
import com.jagex.game.runetek5.preference.impl.MaxScreenSizePreference;
import com.jagex.game.runetek5.preference.impl.MonoStereoPreference;
import com.jagex.game.runetek5.preference.impl.AsyncMaploadingPreference;
import com.jagex.game.runetek5.preference.impl.ParticlePreference;
import com.jagex.game.runetek5.preference.impl.RoofPreference;
import com.jagex.game.runetek5.preference.impl.SafeModePreference;
import com.jagex.game.runetek5.preference.impl.BuildAreaPreference;
import com.jagex.game.runetek5.preference.impl.SceneryShadowPreference;
import com.jagex.game.runetek5.preference.impl.ScreenSizePreference;
import com.jagex.game.runetek5.preference.impl.SkyboxPreference;
import com.jagex.game.runetek5.preference.impl.SoundLevelPreference;
import com.jagex.game.runetek5.preference.impl.TexturesPreference;
import com.jagex.game.runetek5.preference.impl.WaterPreference;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.systeminfo.ProcessorSpecs;
import com.jagex.game.runetek5.systeminfo.SystemInfo;

public class GamePreferences extends Node<GamePreferences> {

	/**
	 * The preferred toolkit to render the game
	 * (may still get overridden on startup as the 
	 * game profiles the performance of given toolkit)
	 * 
	 * Options:
	 * - Safe mode
	 * - Enhanced software mode
	 * - OpenGL
	 * - OpenGL with shaders
	 * - DirectX
	 */
    public ToolkitPreference activeToolkit;
    
    /**
     * The actual AA preference that persists
     * over ingame / multiple game sessions
     */
    public AntiAliasingPreference antiAliasing;
    
    /**
     * When switching AA setting this preference
     * setting is used until the user presses
     * confirm
     */
    public AntiAliasingPreference antiAliasingPreview;
    
    public BloomPreference bloom;
    public BrightnessPreference brightness;
    public MapSizePreference mapSize;
    public BuildAreaPreference buildArea;
    public FlickeringEffectPreference flickeringEffects;
    public FogPreference fog;
    public GroundBlendingPreference goundBlending;
    public GroundDecorationPreference groundDecoration;
    public IdleAnimationsPreference idleAnimations;
    public LightDetailPreference lightDetail;
    public SceneryShadowPreference sceneryShadows;
    public DefaultToolkitPreference defaultToolkit;
    public ParticlePreference particles;
    public RoofPreference removeRoofs;
    public RoofPreference removeRoofsOptionOverride;
    public MaxScreenSizePreference maxScreenSize;
    public SkyboxPreference skyboxes;
    public CharacterShadowPreference characterShadows;
    public TexturesPreference textures;
    public ToolkitPreference toolkit;
    public WaterPreference water;
    public ScreenSizePreference fixedScreen;
    public ScreenSizePreference fullScreen;
    public CustomCursorsPreference customCursor;
    public GraphicsPreference graphics;
    public CpuMaxMemoryPreference cpu;
    public GameTipPreference gametipStartStagePreference;
    public SafeModePreference safeMode;
    public AsyncMaploadingPreference asyncMaploading;
    public SoundLevelPreference soundEffectVolume;
    public SoundLevelPreference ambientSoundVolume;
    public SoundLevelPreference voiceOverVolume;
    public SoundLevelPreference musicVolume;
    public SoundLevelPreference soundLevel;
    public MonoStereoPreference monoStereo;
    public final Game game;
    public final ProcessorSpecs processorSpecs;

    public GamePreferences(Packet buffer, Game game) {
        this.game = game;
        this.processorSpecs = new ProcessorSpecs(GameShell.maxMemory, GameShell.availableProcessors, SystemInfo.OS_ARCH.indexOf("arm") != -1);
        this.activeToolkit = new ToolkitPreference(0, this);
        decode(buffer);
    }

    public GamePreferences(Game game) {
        this.game = game;
        this.processorSpecs = new ProcessorSpecs(GameShell.maxMemory, GameShell.availableProcessors, SystemInfo.OS_ARCH.toLowerCase().indexOf("arm") != -1);
        this.activeToolkit = new ToolkitPreference(0, this);
        setDefaults(true);
    }

    public void decode(Packet stream) {
        if (stream != null && stream.buffer != null) {
            int version = stream.readUnsignedByte();
            if (version < 23) {
                try {
                    decodeDeprecatedVersion(stream, version);
                } catch (Exception exception) {
                    setDefaults(true);
                }
                setDefaults(false);
            } else if (version > 27) {
                setDefaults(true);
            } else {
                this.antiAliasing = new AntiAliasingPreference(stream.readUnsignedByte(), this);
                this.antiAliasingPreview = new AntiAliasingPreference(this.antiAliasing.getValue(), this);
                this.bloom = new BloomPreference(stream.readUnsignedByte(), this);
                this.brightness = new BrightnessPreference(stream.readUnsignedByte(), this);
                this.mapSize = new MapSizePreference(stream.readUnsignedByte(), this);
                if (version >= 27) {
                    this.buildArea = new BuildAreaPreference(stream.readUnsignedByte(), this);
                }

                this.flickeringEffects = new FlickeringEffectPreference(stream.readUnsignedByte(), this);
                this.fog = new FogPreference(stream.readUnsignedByte(), this);
                this.goundBlending = new GroundBlendingPreference(stream.readUnsignedByte(), this);
                this.groundDecoration = new GroundDecorationPreference(stream.readUnsignedByte(), this);
                this.idleAnimations = new IdleAnimationsPreference(stream.readUnsignedByte(), this);
                this.lightDetail = new LightDetailPreference(stream.readUnsignedByte(), this);
                this.sceneryShadows = new SceneryShadowPreference(stream.readUnsignedByte(), this);
                if (version >= 24) {
                    this.defaultToolkit = new DefaultToolkitPreference(stream.readUnsignedByte(), this);
                }

                this.particles = new ParticlePreference(stream.readUnsignedByte(), this);
                this.removeRoofs = new RoofPreference(stream.readUnsignedByte(), this);
                this.removeRoofsOptionOverride = new RoofPreference(this.removeRoofs.getValue(), this);
                this.maxScreenSize = new MaxScreenSizePreference(stream.readUnsignedByte(), this);
                if (version >= 25) {
                    this.skyboxes = new SkyboxPreference(stream.readUnsignedByte(), this);
                }

                this.characterShadows = new CharacterShadowPreference(stream.readUnsignedByte(), this);
                if (version <= 25) {
                    stream.currentPosition++;
                }

                this.textures = new TexturesPreference(stream.readUnsignedByte(), this);
                this.toolkit = new ToolkitPreference(stream.readUnsignedByte(), this);
                this.activeToolkit = new ToolkitPreference(this.toolkit.getValue(), this);
                stream.readUnsignedByte();
                this.water = new WaterPreference(stream.readUnsignedByte(), this);
                this.fixedScreen = new ScreenSizePreference(stream.readUnsignedByte(), this);
                this.fullScreen = new ScreenSizePreference(this.fixedScreen.getValue(), this);
                this.customCursor = new CustomCursorsPreference(this);
                this.graphics = new GraphicsPreference(stream.readUnsignedByte(), this);
                this.cpu = new CpuMaxMemoryPreference(stream.readUnsignedByte(), this);
                this.gametipStartStagePreference = new GameTipPreference(stream.readUnsignedByte(), this);
                this.safeMode = new SafeModePreference(stream.readUnsignedByte(), this);
                if (version >= 26) {
                    this.asyncMaploading = new AsyncMaploadingPreference(stream.readUnsignedByte(), this);
                }

                this.soundEffectVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
                this.ambientSoundVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
                this.voiceOverVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
                this.musicVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
                this.soundLevel = new SoundLevelPreference(stream.readUnsignedByte(), this);
                this.monoStereo = new MonoStereoPreference(stream.readUnsignedByte(), this);
                setDefaults(false);
            }
        } else {
            setDefaults(true);
        }

        ensureValid();
    }

    public void setDefaults(boolean override) {
        if (override || this.antiAliasing == null) {
            this.antiAliasing = new AntiAliasingPreference(this);
        }

        if (override || this.antiAliasingPreview == null) {
            this.antiAliasingPreview = new AntiAliasingPreference(this.antiAliasing.getValue(), this);
        }

        if (override || this.bloom == null) {
            this.bloom = new BloomPreference(this);
        }

        if (override || this.brightness == null) {
            this.brightness = new BrightnessPreference(this);
        }

        if (override || this.mapSize == null) {
            this.mapSize = new MapSizePreference(this);
        }

        if (override || this.buildArea == null) {
            this.buildArea = new BuildAreaPreference(this);
        }

        if (override || this.flickeringEffects == null) {
            this.flickeringEffects = new FlickeringEffectPreference(this);
        }

        if (override || this.fog == null) {
            this.fog = new FogPreference(this);
        }

        if (override || this.goundBlending == null) {
            this.goundBlending = new GroundBlendingPreference(this);
        }

        if (override || this.groundDecoration == null) {
            this.groundDecoration = new GroundDecorationPreference(this);
        }

        if (override || this.idleAnimations == null) {
            this.idleAnimations = new IdleAnimationsPreference(this);
        }

        if (override || this.lightDetail == null) {
            this.lightDetail = new LightDetailPreference(this);
        }

        if (override || this.sceneryShadows == null) {
            this.sceneryShadows = new SceneryShadowPreference(this);
        }

        if (override || this.defaultToolkit == null) {
            this.defaultToolkit = new DefaultToolkitPreference(this);
        }

        if (override || this.particles == null) {
            this.particles = new ParticlePreference(this);
        }

        if (override || this.removeRoofs == null) {
            this.removeRoofs = new RoofPreference(this);
        }

        if (override || this.removeRoofsOptionOverride == null) {
            this.removeRoofsOptionOverride = new RoofPreference(this.removeRoofs.getValue(), this);
        }

        if (override || this.maxScreenSize == null) {
            this.maxScreenSize = new MaxScreenSizePreference(this);
        }

        if (override || this.skyboxes == null) {
            this.skyboxes = new SkyboxPreference(this);
        }

        if (override || this.characterShadows == null) {
            this.characterShadows = new CharacterShadowPreference(this);
        }

        if (override || this.textures == null) {
            this.textures = new TexturesPreference(this);
        }

        if (override || this.toolkit == null) {
            this.toolkit = new ToolkitPreference(this);
        }

        if (override || this.activeToolkit == null) {
            this.activeToolkit = new ToolkitPreference(this.toolkit.getValue(), this);
        }

        if (override || this.water == null) {
            this.water = new WaterPreference(this);
        }

        if (override || this.fixedScreen == null) {
            this.fixedScreen = new ScreenSizePreference(this);
        }

        if (override || this.fullScreen == null) {
            this.fullScreen = new ScreenSizePreference(this.fixedScreen.getValue(), this);
        }

        if (override || this.customCursor == null) {
            this.customCursor = new CustomCursorsPreference(this);
        }

        if (override || this.graphics == null) {
            this.graphics = new GraphicsPreference(this);
        }

        if (override || this.cpu == null) {
            this.cpu = new CpuMaxMemoryPreference(this);
        }

        if (override || this.gametipStartStagePreference == null) {
            this.gametipStartStagePreference = new GameTipPreference(this);
        }

        if (override || this.safeMode == null) {
            this.safeMode = new SafeModePreference(this);
        }

        if (override || this.asyncMaploading == null) {
            this.asyncMaploading = new AsyncMaploadingPreference(this);
        }

        if (override || this.soundEffectVolume == null) {
            this.soundEffectVolume = new SoundLevelPreference(this);
        }

        if (override || this.ambientSoundVolume == null) {
            this.ambientSoundVolume = new SoundLevelPreference(this);
        }

        if (override || this.voiceOverVolume == null) {
            this.voiceOverVolume = new SoundLevelPreference(this);
        }

        if (override || this.musicVolume == null) {
            this.musicVolume = new SoundLevelPreference(this);
        }

        if (override || this.soundLevel == null) {
            this.soundLevel = new SoundLevelPreference(this);
        }

        if (override || this.monoStereo == null) {
            this.monoStereo = new MonoStereoPreference(this);
        }
    }

    public void decodeDeprecatedVersion(Packet stream, int version) {
        this.brightness = new BrightnessPreference(stream.readUnsignedByte(), this);
        stream.currentPosition++;
        this.removeRoofs = new RoofPreference(stream.readUnsignedByte() + 1, this);
        this.groundDecoration = new GroundDecorationPreference(stream.readUnsignedByte(), this);
        stream.currentPosition++;
        this.idleAnimations = new IdleAnimationsPreference(stream.readUnsignedByte(), this);
        this.flickeringEffects = new FlickeringEffectPreference(stream.readUnsignedByte(), this);
        stream.readUnsignedByte();
        this.characterShadows = new CharacterShadowPreference(stream.readUnsignedByte(), this);
        int i_4 = stream.readUnsignedByte();
        int i_5 = 0;
        if (version >= 17) {
            i_5 = stream.readUnsignedByte();
        }

        this.sceneryShadows = new SceneryShadowPreference(Math.max(i_4, i_5), this);
        boolean highDetailLight = true;
        boolean highDetailShadows = true;
        if (version >= 2) {
            highDetailLight = stream.readUnsignedByte() == 1;
            if (version >= 17) {
                highDetailShadows = stream.readUnsignedByte() == 1;
            }
        } else {
            highDetailLight = stream.readUnsignedByte() == 1;
            stream.readUnsignedByte();
        }

        this.lightDetail = new LightDetailPreference(highDetailLight | highDetailShadows ? 1 : 0, this);
        this.water = new WaterPreference(stream.readUnsignedByte(), this);
        this.fog = new FogPreference(stream.readUnsignedByte(), this);
        this.antiAliasing = new AntiAliasingPreference(stream.readUnsignedByte(), this);
        this.monoStereo = new MonoStereoPreference(stream.readUnsignedByte(), this);
        this.soundEffectVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
        if (version >= 20) {
            this.voiceOverVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
        } else {
            this.voiceOverVolume = new SoundLevelPreference(this.soundEffectVolume.getValue(), this);
        }

        this.musicVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
        this.ambientSoundVolume = new SoundLevelPreference(stream.readUnsignedByte(), this);
        if (version >= 21) {
            this.soundLevel = new SoundLevelPreference(stream.readUnsignedByte(), this);
        } else {
            this.soundLevel = new SoundLevelPreference(this.musicVolume.getValue(), this);
        }

        if (version >= 1) {
            stream.readUnsignedShort();
            stream.readUnsignedShort();
        }

        if (version >= 3 && version < 6) {
            stream.readUnsignedByte();
        }

        if (version >= 4) {
            this.particles = new ParticlePreference(stream.readUnsignedByte(), this);
        }

        stream.readInt();
        if (version >= 6) {
            this.fixedScreen = new ScreenSizePreference(stream.readUnsignedByte(), this);
        }

        if (version >= 7) {
            this.safeMode = new SafeModePreference(stream.readUnsignedByte(), this);
        }

        if (version >= 8) {
            stream.readUnsignedByte();
        }

        if (version >= 9) {
            this.mapSize = new MapSizePreference(stream.readUnsignedByte(), this);
        }

        if (version >= 10) {
            this.bloom = new BloomPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 11) {
            this.customCursor = new CustomCursorsPreference(this);
        }

        if (version >= 12) {
            this.idleAnimations = new IdleAnimationsPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 13) {
            this.goundBlending = new GroundBlendingPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 14) {
            this.toolkit = new ToolkitPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 15) {
            this.cpu = new CpuMaxMemoryPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 16) {
            this.textures = new TexturesPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 18) {
            this.graphics = new GraphicsPreference(stream.readUnsignedByte(), this);
        }

        if (version >= 19) {
            this.maxScreenSize = new MaxScreenSizePreference(stream.readUnsignedByte(), this);
        }

        if (version >= 22) {
            this.gametipStartStagePreference = new GameTipPreference(stream.readUnsignedByte(), this);
        }

    }

    public Packet encode() {
        Packet stream = new Packet(40);
        /*0*/
        stream.writeByte(27);
        /*1*/
        stream.writeByte(this.antiAliasing.getValue());
        /*2*/
        stream.writeByte(this.bloom.getValue());
        /*3*/
        stream.writeByte(this.brightness.getValue());
        /*4*/
        stream.writeByte(this.mapSize.getValue());
        /*5*/
        stream.writeByte(this.buildArea.getValue());
        /*6*/
        stream.writeByte(this.flickeringEffects.getValue());
        /*7*/
        stream.writeByte(this.fog.getValue());
        /*8*/
        stream.writeByte(this.goundBlending.getValue());
        /*9*/
        stream.writeByte(this.groundDecoration.getValue());
        /*10*/
        stream.writeByte(this.idleAnimations.getValue());
        /*11*/
        stream.writeByte(this.lightDetail.getValue());
        /*12*/
        stream.writeByte(this.sceneryShadows.getValue());
        /*13*/
        stream.writeByte(this.defaultToolkit.getValue());
        /*14*/
        stream.writeByte(this.particles.getValue());
        /*15*/
        stream.writeByte(this.removeRoofs.getValue());
        /*16*/
        stream.writeByte(this.maxScreenSize.getValue());
        /*17*/
        stream.writeByte(this.skyboxes.getValue());
        /*18*/
        stream.writeByte(this.characterShadows.getValue());
        /*19*/
        stream.writeByte(this.textures.getValue());
        /*20*/
        stream.writeByte(this.toolkit.getValue());
        /*21*/
        stream.writeByte(0);
        /*22*/
        stream.writeByte(this.water.getValue());
        /*23*/
        stream.writeByte(this.fixedScreen.getValue());
        /*24*/
        stream.writeByte(this.customCursor.getValue());
        /*25*/
        stream.writeByte(this.graphics.getValue());
        /*26*/
        stream.writeByte(this.cpu.getValue());
        /*27*/
        stream.writeByte(this.gametipStartStagePreference.getValue());
        /*28*/
        stream.writeByte(this.safeMode.getValue());
        /*29*/
        stream.writeByte(this.asyncMaploading.getValue());
        /*30*/
        stream.writeByte(this.soundEffectVolume.getValue());
        /*31*/
        stream.writeByte(this.ambientSoundVolume.getValue());
        /*32*/
        stream.writeByte(this.voiceOverVolume.getValue());
        /*33*/
        stream.writeByte(this.musicVolume.getValue());
        /*34*/
        stream.writeByte(this.soundLevel.getValue());
        /*35*/
        stream.writeByte(this.monoStereo.getValue());
        return stream;
    }

    public void ensureValid() {
        this.antiAliasing.ensureValid();
        this.antiAliasingPreview.ensureValid();
        this.bloom.ensureValid();
        this.brightness.ensureValid();
        this.mapSize.ensureValid();
        this.buildArea.ensureValid();
        this.flickeringEffects.ensureValid();
        this.fog.ensureValid();
        this.goundBlending.ensureValid();
        this.groundDecoration.ensureValid();
        this.idleAnimations.ensureValid();
        this.lightDetail.ensureValid();
        this.sceneryShadows.ensureValid();
        this.defaultToolkit.ensureValid();
        this.particles.ensureValid();
        this.removeRoofs.ensureValid();
        this.removeRoofsOptionOverride.ensureValid();
        this.maxScreenSize.ensureValid();
        this.skyboxes.ensureValid();
        this.characterShadows.ensureValid();
        this.textures.ensureValid();
        this.toolkit.ensureValid();
        this.activeToolkit.ensureValid();
        this.water.ensureValid();
        this.fixedScreen.ensureValid();
        this.fullScreen.ensureValid();
        this.customCursor.ensureValid();
        this.graphics.ensureValid();
        this.cpu.ensureValid();
        this.gametipStartStagePreference.ensureValid();
        this.safeMode.ensureValid();
        this.asyncMaploading.ensureValid();
        this.soundEffectVolume.ensureValid();
        this.ambientSoundVolume.ensureValid();
        this.voiceOverVolume.ensureValid();
        this.musicVolume.ensureValid();
        this.soundLevel.ensureValid();
        this.monoStereo.ensureValid();
    }

    public ProcessorSpecs getProcessorSpecs() {
        return this.processorSpecs;
    }

    public void verifySettings(ToolkitPreference toolkitPreference, boolean toolkitChanged) {
        toolkitPreference.setVerifySettings(toolkitChanged);
        ensureValid();
    }

    public void setValue(Preference preference, int value) {
        preference.setValidatedValue(value);
        ensureValid();
    }

    public Game getGame() {
        return this.game;
    }

}
