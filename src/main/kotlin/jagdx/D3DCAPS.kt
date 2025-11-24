/* D3DCAPS - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

class D3DCAPS {
    @JvmField var DeviceType: Int = 0
    @JvmField var AdapterOrdinal: Int = 0
    @JvmField var Caps: Int = 0
    @JvmField var Caps2: Int = 0
    @JvmField var Caps3: Int = 0
    @JvmField var PresentationIntervals: Int = 0
    @JvmField var CursorCaps: Int = 0
    @JvmField var DevCaps: Int = 0
    @JvmField var PrimitiveMiscCaps: Int = 0
    @JvmField var RasterCaps: Int = 0
    @JvmField var ZCmpCaps: Int = 0
    @JvmField var SrcBlendCaps: Int = 0
    @JvmField var DestBlendCaps: Int = 0
    @JvmField var AlphaCmpCaps: Int = 0
    @JvmField var ShadeCaps: Int = 0
    @JvmField var TextureCaps: Int = 0
    @JvmField var TextureFilterCaps: Int = 0
    @JvmField var CubeTextureFilterCaps: Int = 0
    @JvmField var VolumeTextureFilterCaps: Int = 0
    @JvmField var TextureAddressCaps: Int = 0
    @JvmField var VolumeTextureAddressCaps: Int = 0
    @JvmField var LineCaps: Int = 0
    @JvmField var MaxTextureWidth: Int = 0
    @JvmField var MaxTextureHeight: Int = 0
    @JvmField var MaxVolumeExtent: Int = 0
    @JvmField var MaxTextureRepeat: Int = 0
    @JvmField var MaxTextureAspectRatio: Int = 0
    @JvmField var MaxAnisotropy: Int = 0
    @JvmField var MaxVertexW: Float = 0f
    @JvmField var GuardBandLeft: Float = 0f
    @JvmField var GuardBandTop: Float = 0f
    @JvmField var GuardBandRight: Float = 0f
    @JvmField var GuardBandBottom: Float = 0f
    @JvmField var ExtentsAdjust: Float = 0f
    @JvmField var StencilCaps: Int = 0
    @JvmField var FVFCaps: Int = 0
    @JvmField var TextureOpCaps: Int = 0
    @JvmField var MaxTextureBlendStages: Int = 0
    @JvmField var MaxSimultaneousTextures: Int = 0
    @JvmField var VertexProcessingCaps: Int = 0
    @JvmField var MaxActiveLights: Int = 0
    @JvmField var MaxUserClipPlanes: Int = 0
    @JvmField var MaxVertexBlendMatrices: Int = 0
    @JvmField var MaxVertexBlendMatrixIndex: Int = 0
    @JvmField var MaxPointSize: Float = 0f
    @JvmField var MaxPrimitiveCount: Int = 0
    @JvmField var MaxVertexIndex: Int = 0
    @JvmField var MaxStreams: Int = 0
    @JvmField var MaxStreamStride: Int = 0
    @JvmField var VertexShaderVersion: Int = 0
    @JvmField var MaxVertexShaderConst: Int = 0
    @JvmField var PixelShaderVersion: Int = 0
    @JvmField var PixelShader1xMaxValue: Float = 0f
    @JvmField var VS20Caps: D3DVSHADERCAPS2_0 = D3DVSHADERCAPS2_0()
    @JvmField var PS20Caps: D3DPSHADERCAPS2_0 = D3DPSHADERCAPS2_0()
    @JvmField var DevCaps2: Int = 0
    @JvmField var MasterAdapterOrdinal: Int = 0
    @JvmField var AdapterOrdinalInGroup: Int = 0
    @JvmField var NumberOfAdaptersInGroup: Int = 0
    @JvmField var DeclTypes: Int = 0
    @JvmField var NumSimultaneousRTs: Int = 0
    @JvmField var StretchRectFilterCaps: Int = 0
    @JvmField var VertexTextureFilterCaps: Int = 0
    @JvmField var MaxVShaderInstructionsExecuted: Int = 0
    @JvmField var MaxPShaderInstructionsExecuted: Int = 0
    @JvmField var MaxVertexShader30InstructionSlots: Int = 0
    @JvmField var MaxPixelShader30InstructionSlots: Int = 0

    companion object {
        const val anInt769: Int = 1048576
        const val anInt770: Int = 16777216
        const val anInt771: Int = 2
        const val anInt772: Int = 16
        const val anInt773: Int = 32
        const val anInt774: Int = 2
        const val anInt775: Int = 8192
        const val anInt776: Int = 16384
        const val anInt777: Int = 65536
        const val anInt778: Int = 2
        const val anInt779: Int = 8
        const val anInt780: Int = 64
        const val anInt781: Int = 512
        const val anInt782: Int = 33554432
    }
}
