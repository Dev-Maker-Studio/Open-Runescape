package com.jagex.game.runetek5.scene.culling

/**
 * https://www.youtube.com/watch?v=-ymmkfjMIeI&ab_channel=OGLDEV
 * @author Steven
 *
 */
open class FrustumCulling(
    @JvmField val culling: Culling
) {

    @JvmField
    var state: Int = 0

    @JvmField
    var anInt2588: Int = 0

    // Could be named inside_view_frustum other than that it also calcs
    fun cull(yA: Int, yB: Int, yC: Int, xA: Int, xB: Int, xC: Int, zA: Int, zB: Int, zC: Int): Boolean {
        var yA = yA
        var yB = yB
        var yC = yC
        var xA = xA
        var xB = xB
        var xC = xC
        var zA = zA
        var zB = zB
        var zC = zC

        if (yA > CLIPSPACE_W || yB > CLIPSPACE_W || yC > CLIPSPACE_W || xA > CLIPSPACE_W || xB > CLIPSPACE_W || xC > CLIPSPACE_W) {
            return false
        }
        if (yA < -CLIPSPACE_W || yB < -CLIPSPACE_W || yC < -CLIPSPACE_W || xA < -CLIPSPACE_W || xB < -CLIPSPACE_W || xC < -CLIPSPACE_W) {
            return false
        }

        if (this.state == 2) {
            var i_10 = this.culling.oneThirdViewportWidth * yA + xA
            if (i_10 >= 0 && i_10 < this.culling.anIntArray2565!!.size && (zA shl 8) - 39168 < this.culling.anIntArray2565!![i_10]) {
                return false
            }

            i_10 = xB + this.culling.oneThirdViewportWidth * yB
            if (i_10 >= 0 && i_10 < this.culling.anIntArray2565!!.size && (zB shl 8) - 39168 < this.culling.anIntArray2565!![i_10]) {
                return false
            }

            i_10 = yC * this.culling.oneThirdViewportWidth + xC
            if (i_10 >= 0 && i_10 < this.culling.anIntArray2565!!.size && (zC shl 8) - 39168 < this.culling.anIntArray2565!![i_10]) {
                return false
            }
        }
        val slopeXBa = xB - xA
        val slopeYBa = yB - yA
        val slopeXCa = xC - xA
        val slopeYCa = yC - yA
        val slopeZBa = zB - zA
        val slopeZCa = zC - zA
        if (yA < yB && yA < yC) {
            yA--
            if (yB > yC) {
                yB++
            } else {
                yC++
            }
        } else if (yB < yC) {
            yB--
            if (yA > yC) {
                yA++
            } else {
                yC++
            }
        } else {
            yC--
            if (yA > yB) {
                yA++
            } else {
                yB++
            }
        }

        var perspectiveBa = 0
        if (yB != yA) {
            perspectiveBa = (xB - xA shl 12) / (yB - yA)
        }

        var perspectiveCb = 0
        if (yC != yB) {
            perspectiveCb = (xC - xB shl 12) / (yC - yB)
        }

        var perspectiveAc = 0
        if (yC != yA) {
            perspectiveAc = (xA - xC shl 12) / (yA - yC)
        }

        val size2d = slopeXBa * slopeYCa - slopeXCa * slopeYBa
        if (size2d == 0) {
            return false
        }
        val i_20 = (slopeZBa * slopeYCa - slopeZCa * slopeYBa shl 8) / size2d
        val i_21 = (slopeZCa * slopeXBa - slopeZBa * slopeXCa shl 8) / size2d
        if (yA <= yB && yA <= yC) {
            if (yA >= this.culling.oneThirdViewportHeight) {
                return true
            }
            if (yB > this.culling.oneThirdViewportHeight) {
                yB = this.culling.oneThirdViewportHeight
            }

            if (yC > this.culling.oneThirdViewportHeight) {
                yC = this.culling.oneThirdViewportHeight
            }

            zA = i_20 + ((zA shl 8) - i_20 * xA)
            if (yB < yC) {
                xC = (xA shl 12).also { xA = it }
                if (yA < 0) {
                    xC -= perspectiveAc * yA
                    xA -= perspectiveBa * yA
                    zA -= i_21 * yA
                    yA = 0
                }

                xB = xB shl 12
                if (yB < 0) {
                    xB -= perspectiveCb * yB
                    yB = 0
                }

                if ((if (yB != yA) perspectiveAc < perspectiveBa else perspectiveAc > perspectiveCb)) {
                    yC -= yB
                    yB -= yA
                    yA = this.culling.oneThirdViewportWidth * yA

                    while (true) {
                        if (--yB < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yA, (xC shr 12) - 1, (xB shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveAc
                                xB += perspectiveCb
                                zA += i_21
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yA, (xC shr 12) - 1, (xA shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveAc
                        xA += perspectiveBa
                        zA += i_21
                        yA += this.culling.oneThirdViewportWidth
                    }
                } else {
                    yC -= yB
                    yB -= yA
                    yA = this.culling.oneThirdViewportWidth * yA

                    while (true) {
                        if (--yB < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yA, (xB shr 12) - 1, (xC shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveAc
                                xB += perspectiveCb
                                zA += i_21
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yA, (xA shr 12) - 1, (xC shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveAc
                        xA += perspectiveBa
                        zA += i_21
                        yA += this.culling.oneThirdViewportWidth
                    }
                }
            } else {
                xB = (xA shl 12).also { xA = it }
                if (yA < 0) {
                    xB -= perspectiveAc * yA
                    xA -= perspectiveBa * yA
                    zA -= i_21 * yA
                    yA = 0
                }

                xC = xC shl 12
                if (yC < 0) {
                    xC -= yC * perspectiveCb
                    yC = 0
                }

                if ((if (yC != yA) perspectiveAc < perspectiveBa else perspectiveCb > perspectiveBa)) {
                    yB -= yC
                    yC -= yA
                    yA = this.culling.oneThirdViewportWidth * yA

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yB < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yA, (xC shr 12) - 1, (xA shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveCb
                                xA += perspectiveBa
                                zA += i_21
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yA, (xB shr 12) - 1, (xA shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xB += perspectiveAc
                        xA += perspectiveBa
                        zA += i_21
                        yA += this.culling.oneThirdViewportWidth
                    }
                } else {
                    yB -= yC
                    yC -= yA
                    yA = this.culling.oneThirdViewportWidth * yA

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yB < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yA, (xA shr 12) - 1, (xC shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveCb
                                xA += perspectiveBa
                                zA += i_21
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yA, (xA shr 12) - 1, (xB shr 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xB += perspectiveAc
                        xA += perspectiveBa
                        zA += i_21
                        yA += this.culling.oneThirdViewportWidth
                    }
                }
            }
        } else if (yB <= yC) {
            if (yB >= this.culling.oneThirdViewportHeight) {
                return true
            }
            if (yC > this.culling.oneThirdViewportHeight) {
                yC = this.culling.oneThirdViewportHeight
            }

            if (yA > this.culling.oneThirdViewportHeight) {
                yA = this.culling.oneThirdViewportHeight
            }

            zB = i_20 + ((zB shl 8) - xB * i_20)
            if (yC < yA) {
                xA = (xB shl 12).also { xB = it }
                if (yB < 0) {
                    xA -= perspectiveBa * yB
                    xB -= perspectiveCb * yB
                    zB -= i_21 * yB
                    yB = 0
                }

                xC = xC shl 12
                if (yC < 0) {
                    xC -= yC * perspectiveAc
                    yC = 0
                }

                if ((if (yC != yB) perspectiveBa < perspectiveCb else perspectiveBa > perspectiveAc)) {
                    yA -= yC
                    yC -= yB
                    yB = this.culling.oneThirdViewportWidth * yB

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yA < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yB, (xA shr 12) - 1, (xC shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveBa
                                xC += perspectiveAc
                                zB += i_21
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yB, (xA shr 12) - 1, (xB shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xA += perspectiveBa
                        xB += perspectiveCb
                        zB += i_21
                        yB += this.culling.oneThirdViewportWidth
                    }
                } else {
                    yA -= yC
                    yC -= yB
                    yB = this.culling.oneThirdViewportWidth * yB

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yA < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yB, (xC shr 12) - 1, (xA shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveBa
                                xC += perspectiveAc
                                zB += i_21
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yB, (xB shr 12) - 1, (xA shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xA += perspectiveBa
                        xB += perspectiveCb
                        zB += i_21
                        yB += this.culling.oneThirdViewportWidth
                    }
                }
            } else {
                xC = (xB shl 12).also { xB = it }
                if (yB < 0) {
                    xC -= perspectiveBa * yB
                    xB -= perspectiveCb * yB
                    zB -= i_21 * yB
                    yB = 0
                }

                xA = xA shl 12
                if (yA < 0) {
                    xA -= perspectiveAc * yA
                    yA = 0
                }

                if (perspectiveBa < perspectiveCb) {
                    yC -= yA
                    yA -= yB
                    yB = this.culling.oneThirdViewportWidth * yB

                    while (true) {
                        if (--yA < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yB, (xA shr 12) - 1, (xB shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveAc
                                xB += perspectiveCb
                                zB += i_21
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yB, (xC shr 12) - 1, (xB shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveBa
                        xB += perspectiveCb
                        zB += i_21
                        yB += this.culling.oneThirdViewportWidth
                    }
                } else {
                    yC -= yA
                    yA -= yB
                    yB = this.culling.oneThirdViewportWidth * yB

                    while (true) {
                        if (--yA < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true
                                }

                                if (!method3373(this.culling.anIntArray2565!!, yB, (xB shr 12) - 1, (xA shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveAc
                                xB += perspectiveCb
                                zB += i_21
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565!!, yB, (xB shr 12) - 1, (xC shr 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveBa
                        xB += perspectiveCb
                        zB += i_21
                        yB += this.culling.oneThirdViewportWidth
                    }
                }
            }
        } else if (yC >= this.culling.oneThirdViewportHeight) {
            return true
        }
        if (yA > this.culling.oneThirdViewportHeight) {
            yA = this.culling.oneThirdViewportHeight
        }

        if (yB > this.culling.oneThirdViewportHeight) {
            yB = this.culling.oneThirdViewportHeight
        }

        zC = i_20 + ((zC shl 8) - i_20 * xC)
        if (yA < yB) {
            xB = (xC shl 12).also { xC = it }
            if (yC < 0) {
                xB -= yC * perspectiveCb
                xC -= yC * perspectiveAc
                zC -= yC * i_21
                yC = 0
            }

            xA = xA shl 12
            if (yA < 0) {
                xA -= perspectiveBa * yA
                yA = 0
            }

            if (perspectiveCb < perspectiveAc) {
                yB -= yA
                yA -= yC
                yC *= this.culling.oneThirdViewportWidth

                while (true) {
                    if (--yA < 0) {
                        while (true) {
                            if (--yB < 0) {
                                return true
                            }

                            if (!method3373(this.culling.anIntArray2565!!, yC, (xB shr 12) - 1, (xA shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveCb
                            xA += perspectiveBa
                            zC += i_21
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565!!, yC, (xB shr 12) - 1, (xC shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xB += perspectiveCb
                    xC += perspectiveAc
                    zC += i_21
                    yC += this.culling.oneThirdViewportWidth
                }
            } else {
                yB -= yA
                yA -= yC
                yC *= this.culling.oneThirdViewportWidth

                while (true) {
                    if (--yA < 0) {
                        while (true) {
                            if (--yB < 0) {
                                return true
                            }

                            if (!method3373(this.culling.anIntArray2565!!, yC, (xA shr 12) - 1, (xB shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveCb
                            xA += perspectiveBa
                            zC += i_21
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565!!, yC, (xC shr 12) - 1, (xB shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xB += perspectiveCb
                    xC += perspectiveAc
                    zC += i_21
                    yC += this.culling.oneThirdViewportWidth
                }
            }
        } else {
            xA = (xC shl 12).also { xC = it }
            if (yC < 0) {
                xA -= yC * perspectiveCb
                xC -= yC * perspectiveAc
                zC -= yC * i_21
                yC = 0
            }

            xB = xB shl 12
            if (yB < 0) {
                xB -= perspectiveBa * yB
                yB = 0
            }

            if (perspectiveCb < perspectiveAc) {
                yA -= yB
                yB -= yC
                yC *= this.culling.oneThirdViewportWidth

                while (true) {
                    if (--yB < 0) {
                        while (true) {
                            if (--yA < 0) {
                                return true
                            }

                            if (!method3373(this.culling.anIntArray2565!!, yC, (xB shr 12) - 1, (xC shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveBa
                            xC += perspectiveAc
                            zC += i_21
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565!!, yC, (xA shr 12) - 1, (xC shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xA += perspectiveCb
                    xC += perspectiveAc
                    zC += i_21
                    yC += this.culling.oneThirdViewportWidth
                }
            } else {
                yA -= yB
                yB -= yC
                yC *= this.culling.oneThirdViewportWidth

                while (true) {
                    if (--yB < 0) {
                        while (true) {
                            if (--yA < 0) {
                                return true
                            }

                            if (!method3373(this.culling.anIntArray2565!!, yC, (xC shr 12) - 1, (xB shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveBa
                            xC += perspectiveAc
                            zC += i_21
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565!!, yC, (xC shr 12) - 1, (xA shr 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xA += perspectiveCb
                    xC += perspectiveAc
                    zC += i_21
                    yC += this.culling.oneThirdViewportWidth
                }
            }
        }
    }

    fun method3373(ints_1: IntArray, i_2: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int): Boolean {
        var i_2 = i_2
        var i_4 = i_4
        var i_5 = i_5
        var i_6 = i_6
        var i_31: Int
        if (i_5 > i_8) {
            i_5 = i_8
        }
        if (i_4 < 0) {
            i_4 = 0
        }
        if (i_4 >= i_5) {
            return true
        }
        i_2 += i_4 - 1
        i_31 = i_5 - i_4 shr 2
        i_6 += i_4 * i_7
        if (this.state == 1) {
            this.anInt2588 += i_31

            while (true) {
                if (--i_31 < 0) {
                    i_31 = i_5 - i_4 and 0x3

                    while (true) {
                        if (--i_31 < 0) {
                            return true
                        }

                        if (i_6 < ints_1[++i_2]) {
                            ints_1[i_2] = i_6
                        }

                        i_6 += i_7
                    }
                }

                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6
                }

                i_6 += i_7
                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6
                }

                i_6 += i_7
                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6
                }

                i_6 += i_7
                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6
                }

                i_6 += i_7
            }
        } else {
            i_6 -= 39168

            while (true) {
                if (--i_31 < 0) {
                    i_31 = i_5 - i_4 and 0x3

                    while (true) {
                        if (--i_31 < 0) {
                            return true
                        }

                        if (i_6 < ints_1[++i_2]) {
                            return false
                        }

                        i_6 += i_7
                    }
                }

                if (i_6 < ints_1[++i_2]) {
                    return false
                }

                i_6 += i_7
                if (i_6 < ints_1[++i_2]) {
                    return false
                }

                i_6 += i_7
                if (i_6 < ints_1[++i_2]) {
                    return false
                }

                i_6 += i_7
                if (i_6 < ints_1[++i_2]) {
                    return false
                }

                i_6 += i_7
            }
        }
    }

    companion object {
        @JvmField
        val CLIPSPACE_W = 2002
    }
}
