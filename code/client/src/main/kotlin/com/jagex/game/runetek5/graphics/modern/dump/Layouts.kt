package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder

class Slot(val offset: Int, val int: Boolean = false, val comps: Int = 0, val stride: Int = 16)

class Attr(val location: Int, val offset: Int, val bytes: Int)

class Pass(val id: Int, val stride: Int, val block: Int, val attrs: List<Attr>, val slots: Map<String, Slot>, val samplers: List<String>) {
    fun pack(values: Map<String, FloatArray>, extra: Map<String, FloatArray> = emptyMap()): ByteArray {
        val out = ByteBuffer.allocate(block).order(ByteOrder.LITTLE_ENDIAN)
        for ((name, v) in values + extra) {
            val s = slots[name] ?: continue
            if (s.comps == 0) {
                for (i in v.indices) if (s.offset + i * 4 + 4 <= block) if (s.int) out.putInt(s.offset + i * 4, v[i].toInt()) else out.putFloat(s.offset + i * 4, v[i])
            } else {
                for (i in v.indices) {
                    val at = s.offset + (i / s.comps) * s.stride + (i % s.comps) * 4
                    if (at + 4 <= block) out.putFloat(at, v[i])
                }
            }
        }
        return out.array()
    }
}

object Passes {
    private fun m(vararg p: Pair<String, Slot>) = mapOf(*p)
    private fun f(o: Int) = Slot(o)
    private fun i(o: Int) = Slot(o, true)
    private fun a(o: Int, c: Int) = Slot(o, false, c)

    private val model36 = listOf(Attr(0, 0, 12), Attr(1, 12, 12), Attr(2, 24, 8), Attr(3, 32, 4))

    val model = Pass(0, 36, 272, model36, m(
        "uModelViewProjection" to f(0), "uModelView" to f(64), "uNormalMatrix" to a(128, 3), "uSunDirection" to f(176), "uSunColor" to f(192),
        "uAmbient" to f(204), "uContrast" to f(208), "uFogStart" to f(212), "uFogEnd" to f(216), "uFogEnabled" to i(220),
        "uPreLitColors" to i(224), "uTextureEnabled" to i(228), "uFogColor" to f(240), "uAlphaThreshold" to f(256)
    ), listOf("uTexture"))

    val ground = Pass(1, 28, 4448, listOf(Attr(0, 0, 12), Attr(1, 12, 12), Attr(3, 24, 4)), m(
        "uModelViewProjection" to f(0), "uModelView" to f(64), "uNormalMatrix" to a(128, 3), "uSunDirection" to f(176), "uSunColor" to f(192),
        "uAmbient" to f(204), "uContrast" to f(208), "uFogStart" to f(212), "uFogEnd" to f(216), "uFogEnabled" to i(220), "uShadowMatrix" to f(224),
        "uShadowEnabled" to i(288), "uPointLightCount" to i(292), "uPointLightPos" to a(304, 4), "uPointLightColor" to a(2352, 3),
        "uGroundShadowEnabled" to i(4400), "uGroundShadowScale" to f(4404), "uFogColor" to f(4416), "uShadowBias" to f(4432),
        "uShadowIntensity" to f(4436), "uShadowMapSize" to f(4440)
    ), listOf("uGroundShadowTex", "uShadowMap"))

    val sprite = Pass(2, 40, 64, listOf(Attr(0, 0, 8), Attr(2, 8, 8), Attr(3, 16, 4), Attr(4, 20, 4), Attr(5, 24, 16)), m("uProjection" to f(0)), listOf("uTextures"))

    val terrain = Pass(3, 40, 5552, listOf(Attr(0, 0, 12), Attr(1, 12, 4), Attr(2, 16, 12), Attr(3, 28, 12)), m(
        "uViewProjection" to f(0), "uClipPlane" to f(64), "uTextureScale" to f(80), "uTextureScroll" to f(88), "uHasNormals" to i(96),
        "uSunDirection" to f(112), "uSunColor" to f(128), "uAmbient" to f(140), "uContrast" to f(144), "uSunBrightness" to f(148),
        "uContrastPos" to f(152), "uContrastNeg" to f(156), "uRawColorDebug" to i(160), "uFogStart" to f(164), "uFogEnd" to f(168),
        "uFogEnabled" to i(172), "uPointLightCount" to i(176), "uPointLightPos" to a(192, 4), "uPointLightColor" to a(2240, 3),
        "uWaterMode" to i(4288), "uStillFog" to f(4304), "uLapNoise" to a(4320, 2), "uLapTurbulence" to f(5344), "uLapAmbient" to f(5360),
        "uWaterTime" to f(5372), "uRippleFog" to f(5376), "uHasWaterNoise" to i(5384), "uWaterColour" to f(5392), "uDepthFog" to i(5404),
        "uInvViewProjection" to f(5408), "uViewport" to f(5472), "uFramebufferSize" to f(5488), "uGroundShadowEnabled" to i(5496),
        "uGroundShadowScale" to f(5500), "uHasTexture" to i(5504), "uFogColor" to f(5520), "uCombineMode" to i(5536), "uAlphaFromVertex" to i(5540)
    ), listOf("uTexture", "uGroundShadowTex"))

    val instanced = Pass(4, 36, 4320, model36, m(
        "uViewProjection" to f(0), "uClipPlane" to f(64), "uViewMatrix" to f(80), "uSunDirection" to f(144), "uSunBrightness" to f(156),
        "uSunColor" to f(160), "uContrastPos" to f(172), "uContrastNeg" to f(176), "uFogStart" to f(180), "uFogEnd" to f(184), "uFogEnabled" to i(188),
        "uFogColor2" to f(192), "uAlphaThreshold" to f(204), "uTextureTime" to f(208), "uMaterialFlags" to i(212), "uTextureSpeed" to f(216),
        "uPointLightPos" to a(224, 4), "uPointLightColor" to a(2272, 3)
    ), listOf("uTexture"))

    val shadow = Pass(5, 36, 144, model36, m(
        "uLightViewProjection" to f(0), "uModelMatrix" to f(64), "uAlphaTestEnabled" to i(128), "uAlphaThreshold" to f(132)
    ), listOf("uTexture"))

    val particle = Pass(6, 24, 128, listOf(Attr(0, 0, 12), Attr(2, 12, 8), Attr(3, 20, 4)), m(
        "uViewProjection" to f(0), "uTextureScroll" to f(64), "uAmbient" to f(80), "uFogEnabled" to i(92), "uFogStart" to f(96), "uFogEnd" to f(100),
        "uTextureEnabled" to i(104), "uCombineMode" to i(108), "uFogColor" to f(112)
    ), listOf("uTexture"))

    val hdWater = Pass(7, 28, 4432, listOf(Attr(0, 0, 12), Attr(1, 12, 4), Attr(2, 16, 12)), m(
        "uViewProjection" to f(0), "uHasWaveNoise" to i(64), "uWaveScale" to f(68), "uTime" to f(72), "uFlow" to f(80), "uWaveAmplitude" to f(88),
        "uHasWaterDepth" to i(92), "uUsePlanar" to i(96), "uPlanarDistort" to f(100), "uHasFoamNoise" to i(104), "uHasEnvMap" to i(108),
        "uShadowEnabled" to i(112), "uInvViewProjection" to f(128), "uViewport" to f(192), "uFramebufferSize" to f(208), "uSunDirection" to f(224),
        "uSunColor" to f(240), "uAmbient" to f(252), "uSkyColour" to f(256), "uWaterColour" to f(272), "uAbsorbDistance" to f(284), "uScatter" to f(288),
        "uRefraction" to f(292), "uWaveStrength" to f(296), "uSlowTime" to f(300), "uShadowScale" to f(304), "uReflectionSteps" to i(308),
        "uFogEnabled" to i(312), "uFogStart" to f(316), "uFogEnd" to f(320), "uPointLightCount" to i(324), "uPointLightPos" to a(336, 4),
        "uPointLightColor" to a(2384, 3)
    ), listOf("uWaveNoise", "uFoamNoise", "uEnvMap", "uShadowTex", "uReflection"))

    val material = Pass(8, 36, 272, model36, m(
        "uModelViewProjection" to f(0), "uModelView" to f(64), "uNormalMatrix" to a(128, 3), "uSunDirection" to f(176), "uSunColor" to f(192),
        "uAmbient" to f(204), "uContrast" to f(208), "uPreLitColors" to i(212), "uFogStart" to f(216), "uFogEnd" to f(220), "uFogEnabled" to i(224),
        "uTextureEnabled" to i(228), "uFogColor" to f(240), "uAlphaThreshold" to f(256), "uSpecularLevel" to i(260), "uEffectEnabled" to i(264),
        "uLimitedTextureUnits" to i(268)
    ), listOf("uTexture"))

    val water = Pass(9, 36, 384, model36, m(
        "uModelView" to f(0), "uProjection" to f(64), "uTextureMatrix" to f(128), "uFogParams" to f(192), "uWaterPlane" to f(208), "uUseLighting" to i(224),
        "uUseDepthPlane" to i(228), "uLight0Direction" to f(240), "uLight1Direction" to f(256), "uLight0Diffuse" to f(272), "uLight1Diffuse" to f(288),
        "uAmbientColor" to f(304), "uWaterColor" to f(320), "uHasBaseTexture" to i(332), "uFogStart" to f(336), "uFogEnd" to f(340), "uFogColor" to f(352),
        "uFogEnabled" to i(368)
    ), listOf("uTexture", "uEdgeTexture"))

    val waterHD = Pass(10, 36, 336, model36, m(
        "uModelView" to f(0), "uProjection" to f(64), "uTextureMatrix" to f(128), "uNormalMatrix" to a(192, 3), "uTime" to f(240), "uScale" to f(244),
        "uFogStart" to f(248), "uFogEnd" to f(252), "uSunDir" to f(256), "uSunColour" to f(272), "uSunExponent" to f(288), "uBreakWaterDepth" to f(292),
        "uBreakWaterOffset" to f(296), "uUseWaveEffect" to i(300), "uWaveIntensity" to f(304), "uWaveExponent" to f(312), "uFogColor" to f(320)
    ), listOf("uNormalSampler", "uEnvMapSampler"))

    val waterLapping = Pass(11, 36, 1392, model36, m(
        "uModelView" to f(0), "uProjection" to f(64), "uTextureMatrix" to f(128), "uInverseViewRotation" to f(192), "uNoise" to a(256, 2),
        "uTurbulence" to f(1280), "uAmbientLight" to f(1296), "uTime" to f(1308), "uUse3DTexture" to i(1312), "uHasOverlay" to i(1316),
        "uHasBaseTexture" to i(1320), "uOverlayConstColor" to f(1328), "uFogStart" to f(1344), "uFogEnd" to f(1348), "uFogColor" to f(1360),
        "uFogEnabled" to i(1376)
    ), listOf("uBaseTexture", "uOverlayTexture3D", "uOverlayTexture2D"))

    val waterReflecting = Pass(12, 36, 272, model36, m(
        "uModelViewProjection" to f(0), "uModelView" to f(64), "uNormalMatrix" to a(128, 3), "uViewRotation" to a(176, 3), "uHasBaseTexture" to i(224),
        "uReflectionEnabled" to i(228), "uFogStart" to f(232), "uFogEnd" to f(236), "uFogColor" to f(240), "uFogEnabled" to i(256)
    ), listOf("uBaseTexture", "uCubeMap"))

    val waterRippling = Pass(13, 36, 304, model36, m(
        "uModelViewProjection" to f(0), "uModelView" to f(64), "uNormalMatrix" to a(128, 3), "uSunDirection" to f(176), "uSunColor" to f(192),
        "uAmbient" to f(204), "uContrast" to f(208), "uFogStart" to f(212), "uFogEnd" to f(216), "uFogEnabled" to i(220), "uEyePlaneS" to f(224),
        "uTextureScale" to f(240), "uFogDensity" to f(244), "uFogPosition" to f(248), "uTextureEnabled" to i(252), "uUse3DTexture" to i(256),
        "uAnimTime" to f(260), "uFogColor" to f(272), "uAlphaThreshold" to f(288)
    ), listOf("uTexture", "uTexture3D"))

    val waterfall = Pass(14, 36, 304, model36, m(
        "uModelViewProjection" to f(0), "uModelView" to f(64), "uNormalMatrix" to a(128, 3), "uSunDirection" to f(176), "uSunColor" to f(192),
        "uAmbient" to f(204), "uContrast" to f(208), "uFogStart" to f(212), "uFogEnd" to f(216), "uFogEnabled" to i(220), "uEyePlaneS" to f(224),
        "uEyePlaneT" to f(240), "uAnimTimeR" to f(256), "uUse3DTexture" to i(260), "uTextureEnabled" to i(264), "uHasWaterfallTexture" to i(268),
        "uFogColor" to f(272), "uAlphaThreshold" to f(288)
    ), listOf("uTexture", "uTexture3D", "uTexture2D"))

    val surfaceMask = Pass(15, 24, 64, listOf(Attr(0, 0, 8), Attr(1, 8, 8), Attr(2, 16, 8)), m("uProjection" to f(0)), listOf("uBase", "uMask"))

    val byName = mapOf(
        "model" to model, "ground" to ground, "sprite" to sprite, "groundTextured" to terrain, "instanced" to instanced, "shadow" to shadow,
        "particle" to particle, "hdWater" to hdWater, "materialCubeSpec" to material, "water" to water, "waterHD" to waterHD,
        "waterLapping" to waterLapping, "waterReflecting" to waterReflecting, "waterRippling" to waterRippling, "waterfall" to waterfall, "surfaceMask" to surfaceMask
    )
}
