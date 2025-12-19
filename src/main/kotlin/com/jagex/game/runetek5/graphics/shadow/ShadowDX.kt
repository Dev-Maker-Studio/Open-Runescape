package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.launch.Loader

open class ShadowDX(i_2: Int, i_3: Int) : Shadow() {

    @JvmField
    var minX: Int = 0
    @JvmField
    var minZ: Int = 0
    @JvmField
    var sizeX: Int = 0
    @JvmField
    var sizeZ: Int = 0
    @JvmField
    val shadowRaster: ByteArray = ByteArray(i_3 * i_2)

    fun isClean(width: Int, length: Int): Boolean {
        return shadowRaster.size >= length * width
    }

    fun setSize(minX: Int, minZ: Int, maxX: Int, maxZ: Int) {
        this.minX = minX
        this.minZ = minZ
        this.sizeX = maxX - minX
        this.sizeZ = maxZ - minZ
    }

    fun clearPixels() {
        var pixelIndex = -1
        val limit = shadowRaster.size - 8

        while (pixelIndex < limit) {
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
            shadowRaster[++pixelIndex] = 0
        }

        while (pixelIndex < shadowRaster.size - 1) {
            shadowRaster[++pixelIndex] = 0
        }
    }

    fun calculateShadowTriangle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        if (Loader.MULTI_THREADED_MAPLOADING) {
            calculateShadowTriangle876(i_1, i_2, i_3, i_4, i_5, i_6)
            return
        }
        var i_61 = i_6
        var i_51 = i_5
        var i_11 = i_1
        var i_41 = i_4
        var i_31 = i_3
        var i_21 = i_2
        var i_7 = 0
        if (i_21 != i_11) {
            i_7 = (i_51 - i_41 shl 16) / (i_21 - i_11)
        }

        var i_8 = 0
        if (i_31 != i_21) {
            i_8 = (i_61 - i_51 shl 16) / (i_31 - i_21)
        }

        var i_9 = 0
        if (i_31 != i_11) {
            i_9 = (i_41 - i_61 shl 16) / (i_11 - i_31)
        }

        if (i_11 <= i_21 && i_11 <= i_31) {
            if (i_21 < i_31) {
                i_61 = i_41 shl 16
                i_41 = i_61
                if (i_11 < 0) {
                    i_61 -= i_9 * i_11
                    i_41 -= i_7 * i_11
                    i_11 = 0
                }

                i_51 = i_51 shl 16
                if (i_21 < 0) {
                    i_51 -= i_8 * i_21
                    i_21 = 0
                }

                if (i_21 != i_11 && i_9 < i_7 || i_21 == i_11 && i_9 > i_8) {
                    i_31 -= i_21
                    i_21 -= i_11
                    i_11 *= this.sizeX

                    while (true) {
                        --i_21
                        if (i_21 < 0) {
                            while (true) {
                                --i_31
                                if (i_31 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_61 shr 16, i_51 shr 16)
                                i_61 += i_9
                                i_51 += i_8
                                i_11 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_61 shr 16, i_41 shr 16)
                        i_61 += i_9
                        i_41 += i_7
                        i_11 += this.sizeX
                    }
                } else {
                    i_31 -= i_21
                    i_21 -= i_11
                    i_11 *= this.sizeX

                    while (true) {
                        --i_21
                        if (i_21 < 0) {
                            while (true) {
                                --i_31
                                if (i_31 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_51 shr 16, i_61 shr 16)
                                i_61 += i_9
                                i_51 += i_8
                                i_11 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_41 shr 16, i_61 shr 16)
                        i_61 += i_9
                        i_41 += i_7
                        i_11 += this.sizeX
                    }
                }
            } else {
                i_51 = i_41 shl 16
                i_41 = i_51
                if (i_11 < 0) {
                    i_51 -= i_9 * i_11
                    i_41 -= i_7 * i_11
                    i_11 = 0
                }

                i_61 = i_61 shl 16
                if (i_31 < 0) {
                    i_61 -= i_31 * i_8
                    i_31 = 0
                }

                if ((i_31 == i_11 || i_9 >= i_7) && (i_31 != i_11 || i_8 <= i_7)) {
                    i_21 -= i_31
                    i_31 -= i_11
                    i_11 *= this.sizeX

                    while (true) {
                        --i_31
                        if (i_31 < 0) {
                            while (true) {
                                --i_21
                                if (i_21 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_41 shr 16, i_61 shr 16)
                                i_61 += i_8
                                i_41 += i_7
                                i_11 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_41 shr 16, i_51 shr 16)
                        i_51 += i_9
                        i_41 += i_7
                        i_11 += this.sizeX
                    }
                } else {
                    i_21 -= i_31
                    i_31 -= i_11
                    i_11 *= this.sizeX

                    while (true) {
                        --i_31
                        if (i_31 < 0) {
                            while (true) {
                                --i_21
                                if (i_21 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_61 shr 16, i_41 shr 16)
                                i_61 += i_8
                                i_41 += i_7
                                i_11 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_51 shr 16, i_41 shr 16)
                        i_51 += i_9
                        i_41 += i_7
                        i_11 += this.sizeX
                    }
                }
            }
        } else if (i_21 <= i_31) {
            if (i_31 < i_11) {
                i_41 = i_51 shl 16
                i_51 = i_41
                if (i_21 < 0) {
                    i_41 -= i_7 * i_21
                    i_51 -= i_8 * i_21
                    i_21 = 0
                }

                i_61 = i_61 shl 16
                if (i_31 < 0) {
                    i_61 -= i_31 * i_9
                    i_31 = 0
                }

                if ((i_31 == i_21 || i_7 >= i_8) && (i_31 != i_21 || i_7 <= i_9)) {
                    i_11 -= i_31
                    i_31 -= i_21
                    i_21 *= this.sizeX

                    while (true) {
                        --i_31
                        if (i_31 < 0) {
                            while (true) {
                                --i_11
                                if (i_11 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_61 shr 16, i_41 shr 16)
                                i_41 += i_7
                                i_61 += i_9
                                i_21 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_51 shr 16, i_41 shr 16)
                        i_41 += i_7
                        i_51 += i_8
                        i_21 += this.sizeX
                    }
                } else {
                    i_11 -= i_31
                    i_31 -= i_21
                    i_21 *= this.sizeX

                    while (true) {
                        --i_31
                        if (i_31 < 0) {
                            while (true) {
                                --i_11
                                if (i_11 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_41 shr 16, i_61 shr 16)
                                i_41 += i_7
                                i_61 += i_9
                                i_21 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_41 shr 16, i_51 shr 16)
                        i_41 += i_7
                        i_51 += i_8
                        i_21 += this.sizeX
                    }
                }
            } else {
                i_61 = i_51 shl 16
                i_51 = i_61
                if (i_21 < 0) {
                    i_61 -= i_7 * i_21
                    i_51 -= i_8 * i_21
                    i_21 = 0
                }

                i_41 = i_41 shl 16
                if (i_11 < 0) {
                    i_41 -= i_9 * i_11
                    i_11 = 0
                }

                if (i_7 < i_8) {
                    i_31 -= i_11
                    i_11 -= i_21
                    i_21 *= this.sizeX

                    while (true) {
                        --i_11
                        if (i_11 < 0) {
                            while (true) {
                                --i_31
                                if (i_31 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_41 shr 16, i_51 shr 16)
                                i_41 += i_9
                                i_51 += i_8
                                i_21 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_61 shr 16, i_51 shr 16)
                        i_61 += i_7
                        i_51 += i_8
                        i_21 += this.sizeX
                    }
                } else {
                    i_31 -= i_11
                    i_11 -= i_21
                    i_21 *= this.sizeX

                    while (true) {
                        --i_11
                        if (i_11 < 0) {
                            while (true) {
                                --i_31
                                if (i_31 < 0) {
                                    return
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_51 shr 16, i_41 shr 16)
                                i_41 += i_9
                                i_51 += i_8
                                i_21 += this.sizeX
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_51 shr 16, i_61 shr 16)
                        i_61 += i_7
                        i_51 += i_8
                        i_21 += this.sizeX
                    }
                }
            }
        } else if (i_11 < i_21) {
            i_51 = i_61 shl 16
            i_61 = i_51
            if (i_31 < 0) {
                i_51 -= i_31 * i_8
                i_61 -= i_31 * i_9
                i_31 = 0
            }

            i_41 = i_41 shl 16
            if (i_11 < 0) {
                i_41 -= i_7 * i_11
                i_11 = 0
            }

            if (i_8 < i_9) {
                i_21 -= i_11
                i_11 -= i_31
                i_31 *= this.sizeX

                while (true) {
                    --i_11
                    if (i_11 < 0) {
                        while (true) {
                            --i_21
                            if (i_21 < 0) {
                                return
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_51 shr 16, i_41 shr 16)
                            i_51 += i_8
                            i_41 += i_7
                            i_31 += this.sizeX
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_51 shr 16, i_61 shr 16)
                    i_51 += i_8
                    i_61 += i_9
                    i_31 += this.sizeX
                }
            } else {
                i_21 -= i_11
                i_11 -= i_31
                i_31 *= this.sizeX

                while (true) {
                    --i_11
                    if (i_11 < 0) {
                        while (true) {
                            --i_21
                            if (i_21 < 0) {
                                return
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_41 shr 16, i_51 shr 16)
                            i_51 += i_8
                            i_41 += i_7
                            i_31 += this.sizeX
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_61 shr 16, i_51 shr 16)
                    i_51 += i_8
                    i_61 += i_9
                    i_31 += this.sizeX
                }
            }
        } else {
            i_41 = i_61 shl 16
            i_61 = i_41
            if (i_31 < 0) {
                i_41 -= i_31 * i_8
                i_61 -= i_31 * i_9
                i_31 = 0
            }

            i_51 = i_51 shl 16
            if (i_21 < 0) {
                i_51 -= i_7 * i_21
                i_21 = 0
            }

            if (i_8 < i_9) {
                i_11 -= i_21
                i_21 -= i_31
                i_31 *= this.sizeX

                while (true) {
                    --i_21
                    if (i_21 < 0) {
                        while (true) {
                            --i_11
                            if (i_11 < 0) {
                                return
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_51 shr 16, i_61 shr 16)
                            i_51 += i_7
                            i_61 += i_9
                            i_31 += this.sizeX
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_41 shr 16, i_61 shr 16)
                    i_41 += i_8
                    i_61 += i_9
                    i_31 += this.sizeX
                }
            } else {
                i_11 -= i_21
                i_21 -= i_31
                i_31 *= this.sizeX

                while (true) {
                    --i_21
                    if (i_21 < 0) {
                        while (true) {
                            --i_11
                            if (i_11 < 0) {
                                return
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_61 shr 16, i_51 shr 16)
                            i_51 += i_7
                            i_61 += i_9
                            i_31 += this.sizeX
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_61 shr 16, i_41 shr 16)
                    i_41 += i_8
                    i_61 += i_9
                    i_31 += this.sizeX
                }
            }
        }
    }

    fun calculateShadowTriangle876(i: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int) {
        var i11 = 0
        if (i6 != i)
            i11 = (i9 - i8 shl 16) / (i6 - i)
        var i12 = 0
        if (i7 != i6)
            i12 = (i10 - i9 shl 16) / (i7 - i6)
        var i13 = 0
        if (i7 != i)
            i13 = (i8 - i10 shl 16) / (i - i7)
        var i_var = i
        var i6_var = i6
        var i7_var = i7
        var i8_var = i8
        var i9_var = i9
        var i10_var = i10

        if (i <= i6 && i <= i7) {
            if (i6 < i7) {
                i10_var = i8 shl 16
                i8_var = i10_var
                if (i_var < 0) {
                    i10_var -= i13 * i_var
                    i8_var -= i11 * i_var
                    i_var = 0
                }
                i9_var = i9 shl 16
                if (i6_var < 0) {
                    i9_var -= i12 * i6_var
                    i6_var = 0
                }
                if (i_var != i6_var && i13 < i11 || i_var == i6_var && i13 > i12) {
                    i7_var -= i6_var
                    i6_var -= i_var
                    i_var *= this.sizeX
                    while (--i6_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i10_var shr 16, i8_var shr 16)
                        i10_var += i13
                        i8_var += i11
                        i_var += this.sizeX
                    }
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i10_var shr 16, i9_var shr 16)
                        i10_var += i13
                        i9_var += i12
                        i_var += this.sizeX
                    }
                } else {
                    i7_var -= i6_var
                    i6_var -= i_var
                    i_var *= this.sizeX
                    while (--i6_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i8_var shr 16, i10_var shr 16)
                        i10_var += i13
                        i8_var += i11
                        i_var += this.sizeX
                    }
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i9_var shr 16, i10_var shr 16)
                        i10_var += i13
                        i9_var += i12
                        i_var += this.sizeX
                    }
                }
            } else {
                i9_var = i8 shl 16
                i8_var = i9_var
                if (i_var < 0) {
                    i9_var -= i13 * i_var
                    i8_var -= i11 * i_var
                    i_var = 0
                }
                i10_var = i10 shl 16
                if (i7_var < 0) {
                    i10_var -= i12 * i7_var
                    i7_var = 0
                }
                if (i_var != i7_var && i13 < i11 || i_var == i7_var && i12 > i11) {
                    i6_var -= i7_var
                    i7_var -= i_var
                    i_var *= this.sizeX
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i9_var shr 16, i8_var shr 16)
                        i9_var += i13
                        i8_var += i11
                        i_var += this.sizeX
                    }
                    while (--i6_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i10_var shr 16, i8_var shr 16)
                        i10_var += i12
                        i8_var += i11
                        i_var += this.sizeX
                    }
                } else {
                    i6_var -= i7_var
                    i7_var -= i_var
                    i_var *= this.sizeX
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i8_var shr 16, i9_var shr 16)
                        i9_var += i13
                        i8_var += i11
                        i_var += this.sizeX
                    }
                    while (--i6_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i_var, 0, i8_var shr 16, i10_var shr 16)
                        i10_var += i12
                        i8_var += i11
                        i_var += this.sizeX
                    }
                }
            }
        } else if (i6 <= i7) {
            if (i7 < i) {
                i8_var = i9 shl 16
                i9_var = i8_var
                if (i6_var < 0) {
                    i8_var -= i11 * i6_var
                    i9_var -= i12 * i6_var
                    i6_var = 0
                }
                i10_var = i10 shl 16
                if (i7_var < 0) {
                    i10_var -= i13 * i7_var
                    i7_var = 0
                }
                if (i6_var != i7_var && i11 < i12 || i6_var == i7_var && i11 > i13) {
                    i_var -= i7_var
                    i7_var -= i6_var
                    i6_var *= this.sizeX
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i8_var shr 16, i9_var shr 16)
                        i8_var += i11
                        i9_var += i12
                        i6_var += this.sizeX
                    }
                    while (--i_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i8_var shr 16, i10_var shr 16)
                        i8_var += i11
                        i10_var += i13
                        i6_var += this.sizeX
                    }
                } else {
                    i_var -= i7_var
                    i7_var -= i6_var
                    i6_var *= this.sizeX
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i9_var shr 16, i8_var shr 16)
                        i8_var += i11
                        i9_var += i12
                        i6_var += this.sizeX
                    }
                    while (--i_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i10_var shr 16, i8_var shr 16)
                        i8_var += i11
                        i10_var += i13
                        i6_var += this.sizeX
                    }
                }
            } else {
                i10_var = i9 shl 16
                i9_var = i10_var
                if (i6_var < 0) {
                    i10_var -= i11 * i6_var
                    i9_var -= i12 * i6_var
                    i6_var = 0
                }
                i8_var = i8 shl 16
                if (i_var < 0) {
                    i8_var -= i13 * i_var
                    i_var = 0
                }
                if (i11 < i12) {
                    i7_var -= i_var
                    i_var -= i6_var
                    i6_var *= this.sizeX
                    while (--i_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i10_var shr 16, i9_var shr 16)
                        i10_var += i11
                        i9_var += i12
                        i6_var += this.sizeX
                    }
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i8_var shr 16, i9_var shr 16)
                        i8_var += i13
                        i9_var += i12
                        i6_var += this.sizeX
                    }
                } else {
                    i7_var -= i_var
                    i_var -= i6_var
                    i6_var *= this.sizeX
                    while (--i_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i9_var shr 16, i10_var shr 16)
                        i10_var += i11
                        i9_var += i12
                        i6_var += this.sizeX
                    }
                    while (--i7_var >= 0) {
                        calculateShadowScanline876(this.shadowRaster, i6_var, 0, i9_var shr 16, i8_var shr 16)
                        i8_var += i13
                        i9_var += i12
                        i6_var += this.sizeX
                    }
                }
            }
        } else if (i < i6) {
            i9_var = i10 shl 16
            i10_var = i9_var
            if (i7_var < 0) {
                i9_var -= i12 * i7_var
                i10_var -= i13 * i7_var
                i7_var = 0
            }
            i8_var = i8 shl 16
            if (i_var < 0) {
                i8_var -= i11 * i_var
                i_var = 0
            }
            if (i12 < i13) {
                i6_var -= i_var
                i_var -= i7_var
                i7_var *= this.sizeX
                while (--i_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i9_var shr 16, i10_var shr 16)
                    i9_var += i12
                    i10_var += i13
                    i7_var += this.sizeX
                }
                while (--i6_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i9_var shr 16, i8_var shr 16)
                    i9_var += i12
                    i8_var += i11
                    i7_var += this.sizeX
                }
            } else {
                i6_var -= i_var
                i_var -= i7_var
                i7_var *= this.sizeX
                while (--i_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i10_var shr 16, i9_var shr 16)
                    i9_var += i12
                    i10_var += i13
                    i7_var += this.sizeX
                }
                while (--i6_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i8_var shr 16, i9_var shr 16)
                    i9_var += i12
                    i8_var += i11
                    i7_var += this.sizeX
                }
            }
        } else {
            i8_var = i10 shl 16
            i10_var = i8_var
            if (i7_var < 0) {
                i8_var -= i12 * i7_var
                i10_var -= i13 * i7_var
                i7_var = 0
            }
            i9_var = i9 shl 16
            if (i6_var < 0) {
                i9_var -= i11 * i6_var
                i6_var = 0
            }
            if (i12 < i13) {
                i_var -= i6_var
                i6_var -= i7_var
                i7_var *= this.sizeX
                while (--i6_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i8_var shr 16, i10_var shr 16)
                    i8_var += i12
                    i10_var += i13
                    i7_var += this.sizeX
                }
                while (--i_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i9_var shr 16, i10_var shr 16)
                    i9_var += i11
                    i10_var += i13
                    i7_var += this.sizeX
                }
            } else {
                i_var -= i6_var
                i6_var -= i7_var
                i7_var *= this.sizeX
                while (--i6_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i10_var shr 16, i8_var shr 16)
                    i8_var += i12
                    i10_var += i13
                    i7_var += this.sizeX
                }
                while (--i_var >= 0) {
                    calculateShadowScanline876(this.shadowRaster, i7_var, 0, i10_var shr 16, i9_var shr 16)
                    i9_var += i11
                    i10_var += i13
                    i7_var += this.sizeX
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun calculateShadowScanline(raster: ByteArray, position: Int, i_3: Int, i_4: Int) {
            var i_21 = 0
            var position_var = position
            if (i_3 < i_4) {
                position_var += i_3
                i_21 = i_4 - i_3 shr 2

                while (true) {
                    --i_21
                    if (i_21 < 0) {
                        i_21 = i_4 - i_3 and 0x3

                        while (true) {
                            --i_21
                            if (i_21 < 0) {
                                return
                            }

                            raster[position_var++] = 1
                        }
                    }

                    raster[position_var++] = 1
                    raster[position_var++] = 1
                    raster[position_var++] = 1
                    raster[position_var++] = 1
                }
            }
        }

        @JvmStatic
        fun calculateShadowScanline876(bytes: ByteArray, i: Int, i14: Int, i15: Int, i16: Int) {
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
