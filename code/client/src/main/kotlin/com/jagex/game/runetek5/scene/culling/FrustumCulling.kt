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
    var depthWriteCount: Int = 0

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
            var depthBufferIndex = this.culling.oneThirdViewportWidth * yA + xA
            if (depthBufferIndex >= 0 && depthBufferIndex < this.culling.depthBuffer!!.size && (zA shl 8) - 39168 < this.culling.depthBuffer!![depthBufferIndex]) {
                return false
            }

            depthBufferIndex = xB + this.culling.oneThirdViewportWidth * yB
            if (depthBufferIndex >= 0 && depthBufferIndex < this.culling.depthBuffer!!.size && (zB shl 8) - 39168 < this.culling.depthBuffer!![depthBufferIndex]) {
                return false
            }

            depthBufferIndex = yC * this.culling.oneThirdViewportWidth + xC
            if (depthBufferIndex >= 0 && depthBufferIndex < this.culling.depthBuffer!!.size && (zC shl 8) - 39168 < this.culling.depthBuffer!![depthBufferIndex]) {
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
        val depthGradientX = (slopeZBa * slopeYCa - slopeZCa * slopeYBa shl 8) / size2d
        val depthGradientY = (slopeZCa * slopeXBa - slopeZBa * slopeXCa shl 8) / size2d
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

            zA = depthGradientX + ((zA shl 8) - depthGradientX * xA)
            if (yB < yC) {
                xC = (xA shl 12).also { xA = it }
                if (yA < 0) {
                    xC -= perspectiveAc * yA
                    xA -= perspectiveBa * yA
                    zA -= depthGradientY * yA
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xC shr 12) - 1, (xB shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveAc
                                xB += perspectiveCb
                                zA += depthGradientY
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xC shr 12) - 1, (xA shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveAc
                        xA += perspectiveBa
                        zA += depthGradientY
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xB shr 12) - 1, (xC shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveAc
                                xB += perspectiveCb
                                zA += depthGradientY
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xA shr 12) - 1, (xC shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveAc
                        xA += perspectiveBa
                        zA += depthGradientY
                        yA += this.culling.oneThirdViewportWidth
                    }
                }
            } else {
                xB = (xA shl 12).also { xA = it }
                if (yA < 0) {
                    xB -= perspectiveAc * yA
                    xA -= perspectiveBa * yA
                    zA -= depthGradientY * yA
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xC shr 12) - 1, (xA shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveCb
                                xA += perspectiveBa
                                zA += depthGradientY
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xB shr 12) - 1, (xA shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xB += perspectiveAc
                        xA += perspectiveBa
                        zA += depthGradientY
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xA shr 12) - 1, (xC shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xC += perspectiveCb
                                xA += perspectiveBa
                                zA += depthGradientY
                                yA += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yA, (xA shr 12) - 1, (xB shr 12) + 1, zA, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xB += perspectiveAc
                        xA += perspectiveBa
                        zA += depthGradientY
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

            zB = depthGradientX + ((zB shl 8) - xB * depthGradientX)
            if (yC < yA) {
                xA = (xB shl 12).also { xB = it }
                if (yB < 0) {
                    xA -= perspectiveBa * yB
                    xB -= perspectiveCb * yB
                    zB -= depthGradientY * yB
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xA shr 12) - 1, (xC shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveBa
                                xC += perspectiveAc
                                zB += depthGradientY
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xA shr 12) - 1, (xB shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xA += perspectiveBa
                        xB += perspectiveCb
                        zB += depthGradientY
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xC shr 12) - 1, (xA shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveBa
                                xC += perspectiveAc
                                zB += depthGradientY
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xB shr 12) - 1, (xA shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xA += perspectiveBa
                        xB += perspectiveCb
                        zB += depthGradientY
                        yB += this.culling.oneThirdViewportWidth
                    }
                }
            } else {
                xC = (xB shl 12).also { xB = it }
                if (yB < 0) {
                    xC -= perspectiveBa * yB
                    xB -= perspectiveCb * yB
                    zB -= depthGradientY * yB
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xA shr 12) - 1, (xB shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveAc
                                xB += perspectiveCb
                                zB += depthGradientY
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xC shr 12) - 1, (xB shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveBa
                        xB += perspectiveCb
                        zB += depthGradientY
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

                                if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xB shr 12) - 1, (xA shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                    return false
                                }

                                xA += perspectiveAc
                                xB += perspectiveCb
                                zB += depthGradientY
                                yB += this.culling.oneThirdViewportWidth
                            }
                        }

                        if (!processDepthScanline(this.culling.depthBuffer!!, yB, (xB shr 12) - 1, (xC shr 12) + 1, zB, depthGradientX, this.culling.oneThirdViewportWidth)) {
                            return false
                        }

                        xC += perspectiveBa
                        xB += perspectiveCb
                        zB += depthGradientY
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

        zC = depthGradientX + ((zC shl 8) - depthGradientX * xC)
        if (yA < yB) {
            xB = (xC shl 12).also { xC = it }
            if (yC < 0) {
                xB -= yC * perspectiveCb
                xC -= yC * perspectiveAc
                zC -= yC * depthGradientY
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

                            if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xB shr 12) - 1, (xA shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveCb
                            xA += perspectiveBa
                            zC += depthGradientY
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xB shr 12) - 1, (xC shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xB += perspectiveCb
                    xC += perspectiveAc
                    zC += depthGradientY
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

                            if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xA shr 12) - 1, (xB shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveCb
                            xA += perspectiveBa
                            zC += depthGradientY
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xC shr 12) - 1, (xB shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xB += perspectiveCb
                    xC += perspectiveAc
                    zC += depthGradientY
                    yC += this.culling.oneThirdViewportWidth
                }
            }
        } else {
            xA = (xC shl 12).also { xC = it }
            if (yC < 0) {
                xA -= yC * perspectiveCb
                xC -= yC * perspectiveAc
                zC -= yC * depthGradientY
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

                            if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xB shr 12) - 1, (xC shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveBa
                            xC += perspectiveAc
                            zC += depthGradientY
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xA shr 12) - 1, (xC shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xA += perspectiveCb
                    xC += perspectiveAc
                    zC += depthGradientY
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

                            if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xC shr 12) - 1, (xB shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                                return false
                            }

                            xB += perspectiveBa
                            xC += perspectiveAc
                            zC += depthGradientY
                            yC += this.culling.oneThirdViewportWidth
                        }
                    }

                    if (!processDepthScanline(this.culling.depthBuffer!!, yC, (xC shr 12) - 1, (xA shr 12) + 1, zC, depthGradientX, this.culling.oneThirdViewportWidth)) {
                        return false
                    }

                    xA += perspectiveCb
                    xC += perspectiveAc
                    zC += depthGradientY
                    yC += this.culling.oneThirdViewportWidth
                }
            }
        }
    }

    fun processDepthScanline(depthBuffer: IntArray, scanlineOffset: Int, startX: Int, endX: Int, depth: Int, depthStepX: Int, bufferWidth: Int): Boolean {
        var bufferIndex = scanlineOffset
        var clampedStartX = startX
        var clampedEndX = endX
        var currentDepth = depth
        var pixelCount: Int
        if (clampedEndX > bufferWidth) {
            clampedEndX = bufferWidth
        }
        if (clampedStartX < 0) {
            clampedStartX = 0
        }
        if (clampedStartX >= clampedEndX) {
            return true
        }
        bufferIndex += clampedStartX - 1
        pixelCount = clampedEndX - clampedStartX shr 2
        currentDepth += clampedStartX * depthStepX
        if (this.state == 1) {
            this.depthWriteCount += pixelCount

            while (true) {
                if (--pixelCount < 0) {
                    pixelCount = clampedEndX - clampedStartX and 0x3

                    while (true) {
                        if (--pixelCount < 0) {
                            return true
                        }

                        if (currentDepth < depthBuffer[++bufferIndex]) {
                            depthBuffer[bufferIndex] = currentDepth
                        }

                        currentDepth += depthStepX
                    }
                }

                if (currentDepth < depthBuffer[++bufferIndex]) {
                    depthBuffer[bufferIndex] = currentDepth
                }

                currentDepth += depthStepX
                if (currentDepth < depthBuffer[++bufferIndex]) {
                    depthBuffer[bufferIndex] = currentDepth
                }

                currentDepth += depthStepX
                if (currentDepth < depthBuffer[++bufferIndex]) {
                    depthBuffer[bufferIndex] = currentDepth
                }

                currentDepth += depthStepX
                if (currentDepth < depthBuffer[++bufferIndex]) {
                    depthBuffer[bufferIndex] = currentDepth
                }

                currentDepth += depthStepX
            }
        } else {
            currentDepth -= 39168

            while (true) {
                if (--pixelCount < 0) {
                    pixelCount = clampedEndX - clampedStartX and 0x3

                    while (true) {
                        if (--pixelCount < 0) {
                            return true
                        }

                        if (currentDepth < depthBuffer[++bufferIndex]) {
                            return false
                        }

                        currentDepth += depthStepX
                    }
                }

                if (currentDepth < depthBuffer[++bufferIndex]) {
                    return false
                }

                currentDepth += depthStepX
                if (currentDepth < depthBuffer[++bufferIndex]) {
                    return false
                }

                currentDepth += depthStepX
                if (currentDepth < depthBuffer[++bufferIndex]) {
                    return false
                }

                currentDepth += depthStepX
                if (currentDepth < depthBuffer[++bufferIndex]) {
                    return false
                }

                currentDepth += depthStepX
            }
        }
    }

    companion object {
        @JvmField
        val CLIPSPACE_W = 2002
    }
}
