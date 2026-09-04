package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.launch.Loader

open class ShadowGL(sizeX: Int, sizeY: Int) : Shadow() {

    @JvmField val shade: ByteArray = ByteArray(sizeY * sizeX)
    @JvmField var startX: Int = 0
    @JvmField var startY: Int = 0
    @JvmField var sizeX: Int = 0
    @JvmField var sizeY: Int = 0

    fun reset() {
        var count = -1
        val limit = shade.size - 8
        while (count < limit) {
            shade[++count] = 0
            shade[++count] = 0
            shade[++count] = 0
            shade[++count] = 0
            shade[++count] = 0
            shade[++count] = 0
            shade[++count] = 0
            shade[++count] = 0
        }
        while (count < shade.size - 1) {
            shade[++count] = 0
        }
    }

    fun markShadedTriangle(zA: Int, zB: Int, zC: Int, xA: Int, xB: Int, xC: Int) {
        if (Loader.MULTI_THREADED_MAPLOADING) {
            markShadedTriangle876(zA, zB, zC, xA, xB, xC)
            return
        }
        var slopeBa = 0
        if (zB != zA) {
            slopeBa = (xB - xA shl 16) / (zB - zA)
        }
        var slopeCb = 0
        if (zC != zB) {
            slopeCb = (xC - xB shl 16) / (zC - zB)
        }
        var slopeAc = 0
        if (zC != zA) {
            slopeAc = (xA - xC shl 16) / (zA - zC)
        }
        var zA_var = zA
        var zB_var = zB
        var zC_var = zC
        var xA_var = xA
        var xB_var = xB
        var xC_var = xC

        if (zA <= zB && zA <= zC) {
            if (zB < zC) {
                xC_var = xA shl 16
                xA_var = xC_var
                if (zA_var < 0) {
                    xC_var -= slopeAc * zA_var
                    xA_var -= slopeBa * zA_var
                    zA_var = 0
                }
                xB_var = xB shl 16
                if (zB_var < 0) {
                    xB_var -= slopeCb * zB_var
                    zB_var = 0
                }
                if (zB_var != zA_var && slopeAc < slopeBa || zB_var == zA_var && slopeAc > slopeCb) {
                    zC_var -= zB_var
                    zB_var -= zA_var
                    zA_var *= this.sizeX
                    while (--zB_var >= 0) {
                        markShadedScanline(shade, zA_var, xC_var shr 16, xA_var shr 16)
                        xC_var += slopeAc
                        xA_var += slopeBa
                        zA_var += this.sizeX
                    }
                    while (--zC_var >= 0) {
                        markShadedScanline(shade, zA_var, xC_var shr 16, xB_var shr 16)
                        xC_var += slopeAc
                        xB_var += slopeCb
                        zA_var += this.sizeX
                    }
                    return
                }
                zC_var -= zB_var
                zB_var -= zA_var
                zA_var *= this.sizeX
                while (--zB_var >= 0) {
                    markShadedScanline(shade, zA_var, xA_var shr 16, xC_var shr 16)
                    xC_var += slopeAc
                    xA_var += slopeBa
                    zA_var += this.sizeX
                }
                while (--zC_var >= 0) {
                    markShadedScanline(shade, zA_var, xB_var shr 16, xC_var shr 16)
                    xC_var += slopeAc
                    xB_var += slopeCb
                    zA_var += this.sizeX
                }
                return
            }
            xB_var = xA shl 16
            xA_var = xB_var
            if (zA_var < 0) {
                xB_var -= slopeAc * zA_var
                xA_var -= slopeBa * zA_var
                zA_var = 0
            }
            xC_var = xC shl 16
            if (zC_var < 0) {
                xC_var -= zC_var * slopeCb
                zC_var = 0
            }
            if ((zC_var == zA_var || slopeAc >= slopeBa) && (zC_var != zA_var || slopeCb <= slopeBa)) {
                zB_var -= zC_var
                zC_var -= zA_var
                zA_var *= this.sizeX
                while (--zC_var >= 0) {
                    markShadedScanline(shade, zA_var, xA_var shr 16, xB_var shr 16)
                    xB_var += slopeAc
                    xA_var += slopeBa
                    zA_var += this.sizeX
                }
                while (--zB_var >= 0) {
                    markShadedScanline(shade, zA_var, xA_var shr 16, xC_var shr 16)
                    xC_var += slopeCb
                    xA_var += slopeBa
                    zA_var += this.sizeX
                }
                return
            }
            zB_var -= zC_var
            zC_var -= zA_var
            zA_var *= this.sizeX
            while (--zC_var >= 0) {
                markShadedScanline(shade, zA_var, xB_var shr 16, xA_var shr 16)
                xB_var += slopeAc
                xA_var += slopeBa
                zA_var += this.sizeX
            }
            while (--zB_var >= 0) {
                markShadedScanline(shade, zA_var, xC_var shr 16, xA_var shr 16)
                xC_var += slopeCb
                xA_var += slopeBa
                zA_var += this.sizeX
            }
        } else if (zB <= zC) {
            if (zC < zA) {
                xA_var = xB shl 16
                xB_var = xA_var
                if (zB_var < 0) {
                    xA_var -= slopeBa * zB_var
                    xB_var -= slopeCb * zB_var
                    zB_var = 0
                }
                xC_var = xC shl 16
                if (zC_var < 0) {
                    xC_var -= zC_var * slopeAc
                    zC_var = 0
                }
                if ((zC_var == zB_var || slopeBa >= slopeCb) && (zC_var != zB_var || slopeBa <= slopeAc)) {
                    zA_var -= zC_var
                    zC_var -= zB_var
                    zB_var *= this.sizeX
                    while (--zC_var >= 0) {
                        markShadedScanline(shade, zB_var, xB_var shr 16, xA_var shr 16)
                        xA_var += slopeBa
                        xB_var += slopeCb
                        zB_var += this.sizeX
                    }
                    while (--zA_var >= 0) {
                        markShadedScanline(shade, zB_var, xC_var shr 16, xA_var shr 16)
                        xA_var += slopeBa
                        xC_var += slopeAc
                        zB_var += this.sizeX
                    }
                    return
                }
                zA_var -= zC_var
                zC_var -= zB_var
                zB_var *= this.sizeX
                while (--zC_var >= 0) {
                    markShadedScanline(shade, zB_var, xA_var shr 16, xB_var shr 16)
                    xA_var += slopeBa
                    xB_var += slopeCb
                    zB_var += this.sizeX
                }
                while (--zA_var >= 0) {
                    markShadedScanline(shade, zB_var, xA_var shr 16, xC_var shr 16)
                    xA_var += slopeBa
                    xC_var += slopeAc
                    zB_var += this.sizeX
                }
            } else {
                xC_var = xB shl 16
                xB_var = xC_var
                if (zB_var < 0) {
                    xC_var -= slopeBa * zB_var
                    xB_var -= slopeCb * zB_var
                    zB_var = 0
                }
                xA_var = xA shl 16
                if (zA_var < 0) {
                    xA_var -= slopeAc * zA_var
                    zA_var = 0
                }
                if (slopeBa < slopeCb) {
                    zC_var -= zA_var
                    zA_var -= zB_var
                    zB_var *= this.sizeX
                    while (--zA_var >= 0) {
                        markShadedScanline(shade, zB_var, xC_var shr 16, xB_var shr 16)
                        xC_var += slopeBa
                        xB_var += slopeCb
                        zB_var += this.sizeX
                    }
                    while (--zC_var >= 0) {
                        markShadedScanline(shade, zB_var, xA_var shr 16, xB_var shr 16)
                        xA_var += slopeAc
                        xB_var += slopeCb
                        zB_var += this.sizeX
                    }
                    return
                }
                zC_var -= zA_var
                zA_var -= zB_var
                zB_var *= this.sizeX
                while (--zA_var >= 0) {
                    markShadedScanline(shade, zB_var, xB_var shr 16, xC_var shr 16)
                    xC_var += slopeBa
                    xB_var += slopeCb
                    zB_var += this.sizeX
                }
                while (--zC_var >= 0) {
                    markShadedScanline(shade, zB_var, xB_var shr 16, xA_var shr 16)
                    xA_var += slopeAc
                    xB_var += slopeCb
                    zB_var += this.sizeX
                }
            }
        } else if (zA < zB) {
            xB_var = xC shl 16
            xC_var = xB_var
            if (zC_var < 0) {
                xB_var -= zC_var * slopeCb
                xC_var -= zC_var * slopeAc
                zC_var = 0
            }
            xA_var = xA shl 16
            if (zA_var < 0) {
                xA_var -= slopeBa * zA_var
                zA_var = 0
            }
            if (slopeCb < slopeAc) {
                zB_var -= zA_var
                zA_var -= zC_var
                zC_var *= this.sizeX
                while (--zA_var >= 0) {
                    markShadedScanline(shade, zC_var, xB_var shr 16, xC_var shr 16)
                    xB_var += slopeCb
                    xC_var += slopeAc
                    zC_var += this.sizeX
                }
                while (--zB_var >= 0) {
                    markShadedScanline(shade, zC_var, xB_var shr 16, xA_var shr 16)
                    xB_var += slopeCb
                    xA_var += slopeBa
                    zC_var += this.sizeX
                }
                return
            }
            zB_var -= zA_var
            zA_var -= zC_var
            zC_var *= this.sizeX
            while (--zA_var >= 0) {
                markShadedScanline(shade, zC_var, xC_var shr 16, xB_var shr 16)
                xB_var += slopeCb
                xC_var += slopeAc
                zC_var += this.sizeX
            }
            while (--zB_var >= 0) {
                markShadedScanline(shade, zC_var, xA_var shr 16, xB_var shr 16)
                xB_var += slopeCb
                xA_var += slopeBa
                zC_var += this.sizeX
            }
        } else {
            xA_var = xC shl 16
            xC_var = xA_var
            if (zC_var < 0) {
                xA_var -= zC_var * slopeCb
                xC_var -= zC_var * slopeAc
                zC_var = 0
            }
            xB_var = xB shl 16
            if (zB_var < 0) {
                xB_var -= slopeBa * zB_var
                zB_var = 0
            }
            if (slopeCb < slopeAc) {
                zA_var -= zB_var
                zB_var -= zC_var
                zC_var *= this.sizeX
                while (--zB_var >= 0) {
                    markShadedScanline(shade, zC_var, xA_var shr 16, xC_var shr 16)
                    xA_var += slopeCb
                    xC_var += slopeAc
                    zC_var += this.sizeX
                }
                while (--zA_var >= 0) {
                    markShadedScanline(shade, zC_var, xB_var shr 16, xC_var shr 16)
                    xB_var += slopeBa
                    xC_var += slopeAc
                    zC_var += this.sizeX
                }
                return
            }
            zA_var -= zB_var
            zB_var -= zC_var
            zC_var *= this.sizeX
            while (--zB_var >= 0) {
                markShadedScanline(shade, zC_var, xC_var shr 16, xA_var shr 16)
                xA_var += slopeCb
                xC_var += slopeAc
                zC_var += this.sizeX
            }
            while (--zA_var >= 0) {
                markShadedScanline(shade, zC_var, xC_var shr 16, xB_var shr 16)
                xB_var += slopeBa
                xC_var += slopeAc
                zC_var += this.sizeX
            }
        }
    }

    fun fitsDimension(sizeX: Int, sizeY: Int): Boolean {
        return shade.size >= sizeY * sizeX
    }

    fun setDimension(startX: Int, startY: Int, endX: Int, endY: Int) {
        this.startX = startX
        this.startY = startY
        this.sizeX = endX - startX
        this.sizeY = endY - startY
    }

    fun markShadedTriangle876(i: Int, i24: Int, i25: Int, i26: Int, i27: Int, i28: Int) {
        var i29 = 0
        if (i24 != i)
            i29 = (i27 - i26 shl 16) / (i24 - i)
        var i30 = 0
        if (i25 != i24)
            i30 = (i28 - i27 shl 16) / (i25 - i24)
        var i31 = 0
        if (i25 != i)
            i31 = (i26 - i28 shl 16) / (i - i25)
        var i_var = i
        var i24_var = i24
        var i25_var = i25
        var i26_var = i26
        var i27_var = i27
        var i28_var = i28

        if (i <= i24 && i <= i25) {
            if (i24 < i25) {
                i28_var = i26 shl 16
                i26_var = i28_var
                if (i_var < 0) {
                    i28_var -= i31 * i_var
                    i26_var -= i29 * i_var
                    i_var = 0
                }
                i27_var = i27 shl 16
                if (i24_var < 0) {
                    i27_var -= i30 * i24_var
                    i24_var = 0
                }
                if (i_var != i24_var && i31 < i29 || i_var == i24_var && i31 > i30) {
                    i25_var -= i24_var
                    i24_var -= i_var
                    i_var *= this.sizeX
                    while (--i24_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i28_var shr 16, i26_var shr 16)
                        i28_var += i31
                        i26_var += i29
                        i_var += this.sizeX
                    }
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i28_var shr 16, i27_var shr 16)
                        i28_var += i31
                        i27_var += i30
                        i_var += this.sizeX
                    }
                } else {
                    i25_var -= i24_var
                    i24_var -= i_var
                    i_var *= this.sizeX
                    while (--i24_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i26_var shr 16, i28_var shr 16)
                        i28_var += i31
                        i26_var += i29
                        i_var += this.sizeX
                    }
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i27_var shr 16, i28_var shr 16)
                        i28_var += i31
                        i27_var += i30
                        i_var += this.sizeX
                    }
                }
            } else {
                i27_var = i26 shl 16
                i26_var = i27_var
                if (i_var < 0) {
                    i27_var -= i31 * i_var
                    i26_var -= i29 * i_var
                    i_var = 0
                }
                i28_var = i28 shl 16
                if (i25_var < 0) {
                    i28_var -= i30 * i25_var
                    i25_var = 0
                }
                if (i_var != i25_var && i31 < i29 || i_var == i25_var && i30 > i29) {
                    i24_var -= i25_var
                    i25_var -= i_var
                    i_var *= this.sizeX
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i27_var shr 16, i26_var shr 16)
                        i27_var += i31
                        i26_var += i29
                        i_var += this.sizeX
                    }
                    while (--i24_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i28_var shr 16, i26_var shr 16)
                        i28_var += i30
                        i26_var += i29
                        i_var += this.sizeX
                    }
                } else {
                    i24_var -= i25_var
                    i25_var -= i_var
                    i_var *= this.sizeX
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i26_var shr 16, i27_var shr 16)
                        i27_var += i31
                        i26_var += i29
                        i_var += this.sizeX
                    }
                    while (--i24_var >= 0) {
                        markShadedScanline876(shade, i_var, 0, i26_var shr 16, i28_var shr 16)
                        i28_var += i30
                        i26_var += i29
                        i_var += this.sizeX
                    }
                }
            }
        } else if (i24 <= i25) {
            if (i25 < i) {
                i26_var = i27 shl 16
                i27_var = i26_var
                if (i24_var < 0) {
                    i26_var -= i29 * i24_var
                    i27_var -= i30 * i24_var
                    i24_var = 0
                }
                i28_var = i28 shl 16
                if (i25_var < 0) {
                    i28_var -= i31 * i25_var
                    i25_var = 0
                }
                if (i24_var != i25_var && i29 < i30 || i24_var == i25_var && i29 > i31) {
                    i_var -= i25_var
                    i25_var -= i24_var
                    i24_var *= this.sizeX
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i26_var shr 16, i27_var shr 16)
                        i26_var += i29
                        i27_var += i30
                        i24_var += this.sizeX
                    }
                    while (--i_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i26_var shr 16, i28_var shr 16)
                        i26_var += i29
                        i28_var += i31
                        i24_var += this.sizeX
                    }
                } else {
                    i_var -= i25_var
                    i25_var -= i24_var
                    i24_var *= this.sizeX
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i27_var shr 16, i26_var shr 16)
                        i26_var += i29
                        i27_var += i30
                        i24_var += this.sizeX
                    }
                    while (--i_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i28_var shr 16, i26_var shr 16)
                        i26_var += i29
                        i28_var += i31
                        i24_var += this.sizeX
                    }
                }
            } else {
                i28_var = i27 shl 16
                i27_var = i28_var
                if (i24_var < 0) {
                    i28_var -= i29 * i24_var
                    i27_var -= i30 * i24_var
                    i24_var = 0
                }
                i26_var = i26 shl 16
                if (i_var < 0) {
                    i26_var -= i31 * i_var
                    i_var = 0
                }
                if (i29 < i30) {
                    i25_var -= i_var
                    i_var -= i24_var
                    i24_var *= this.sizeX
                    while (--i_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i28_var shr 16, i27_var shr 16)
                        i28_var += i29
                        i27_var += i30
                        i24_var += this.sizeX
                    }
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i26_var shr 16, i27_var shr 16)
                        i26_var += i31
                        i27_var += i30
                        i24_var += this.sizeX
                    }
                } else {
                    i25_var -= i_var
                    i_var -= i24_var
                    i24_var *= this.sizeX
                    while (--i_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i27_var shr 16, i28_var shr 16)
                        i28_var += i29
                        i27_var += i30
                        i24_var += this.sizeX
                    }
                    while (--i25_var >= 0) {
                        markShadedScanline876(shade, i24_var, 0, i27_var shr 16, i26_var shr 16)
                        i26_var += i31
                        i27_var += i30
                        i24_var += this.sizeX
                    }
                }
            }
        } else if (i < i24) {
            i27_var = i28 shl 16
            i28_var = i27_var
            if (i25_var < 0) {
                i27_var -= i30 * i25_var
                i28_var -= i31 * i25_var
                i25_var = 0
            }
            i26_var = i26 shl 16
            if (i_var < 0) {
                i26_var -= i29 * i_var
                i_var = 0
            }
            if (i30 < i31) {
                i24_var -= i_var
                i_var -= i25_var
                i25_var *= this.sizeX
                while (--i_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i27_var shr 16, i28_var shr 16)
                    i27_var += i30
                    i28_var += i31
                    i25_var += this.sizeX
                }
                while (--i24_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i27_var shr 16, i26_var shr 16)
                    i27_var += i30
                    i26_var += i29
                    i25_var += this.sizeX
                }
            } else {
                i24_var -= i_var
                i_var -= i25_var
                i25_var *= this.sizeX
                while (--i_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i28_var shr 16, i27_var shr 16)
                    i27_var += i30
                    i28_var += i31
                    i25_var += this.sizeX
                }
                while (--i24_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i26_var shr 16, i27_var shr 16)
                    i27_var += i30
                    i26_var += i29
                    i25_var += this.sizeX
                }
            }
        } else {
            i26_var = i28 shl 16
            i28_var = i26_var
            if (i25_var < 0) {
                i26_var -= i30 * i25_var
                i28_var -= i31 * i25_var
                i25_var = 0
            }
            i27_var = i27 shl 16
            if (i24_var < 0) {
                i27_var -= i29 * i24_var
                i24_var = 0
            }
            if (i30 < i31) {
                i_var -= i24_var
                i24_var -= i25_var
                i25_var *= this.sizeX
                while (--i24_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i26_var shr 16, i28_var shr 16)
                    i26_var += i30
                    i28_var += i31
                    i25_var += this.sizeX
                }
                while (--i_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i27_var shr 16, i28_var shr 16)
                    i27_var += i29
                    i28_var += i31
                    i25_var += this.sizeX
                }
            } else {
                i_var -= i24_var
                i24_var -= i25_var
                i25_var *= this.sizeX
                while (--i24_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i28_var shr 16, i26_var shr 16)
                    i26_var += i30
                    i28_var += i31
                    i25_var += this.sizeX
                }
                while (--i_var >= 0) {
                    markShadedScanline876(shade, i25_var, 0, i28_var shr 16, i27_var shr 16)
                    i27_var += i29
                    i28_var += i31
                    i25_var += this.sizeX
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun markShadedScanline(floorShade: ByteArray, shadowIndex: Int, rotation: Int, rotateUntil: Int) {
            var rotationOffset = 0
            var shadowIndex_var = shadowIndex
            if (rotation < rotateUntil) {
                shadowIndex_var += rotation
                rotationOffset = rotateUntil - rotation shr 2
                while (true) {
                    if (--rotationOffset < 0) {
                        rotationOffset = rotateUntil - rotation and 0x3
                        while (true) {
                            if (--rotationOffset < 0) {
                                return
                            }
                            floorShade[shadowIndex_var++] = 1
                        }
                    }
                    floorShade[shadowIndex_var++] = 1
                    floorShade[shadowIndex_var++] = 1
                    floorShade[shadowIndex_var++] = 1
                    floorShade[shadowIndex_var++] = 1
                }
            }
        }

        @JvmStatic
        fun markShadedScanline876(bytes: ByteArray, i: Int, i14: Int, i15: Int, i16: Int) {
            var i_var = i
            var i14_var = i14
            if (i15 < i16) {
                i_var += i15
                i14_var = i16 - i15 shr 2
                while (--i14_var >= 0) {
                    bytes[i_var++] = 1.toByte()
                    bytes[i_var++] = 1.toByte()
                    bytes[i_var++] = 1.toByte()
                    bytes[i_var++] = 1.toByte()
                }
                i14_var = i16 - i15 and 0x3
                while (--i14_var >= 0)
                    bytes[i_var++] = 1.toByte()
            }
        }
    }
}
