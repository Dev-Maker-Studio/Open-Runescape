package com.jagex.game.runetek5.scene.culling;

/**
 * https://www.youtube.com/watch?v=-ymmkfjMIeI&ab_channel=OGLDEV
 * @author Steven
 *
 */
public class FrustumCulling {
	
	public static final int CLIPSPACE_W = 2002;

    public int state;
    public int anInt2588;
    public final Culling culling;

    public FrustumCulling(Culling sceneCulling) {
        this.culling = sceneCulling;
    }

    // Could be named inside_view_frustum other than that it also calcs
    public boolean cull(int yA, int yB, int yC, int xA, int xB, int xC, int zA, int zB, int zC) {
        if (yA > CLIPSPACE_W || yB > CLIPSPACE_W || yC > CLIPSPACE_W || xA > CLIPSPACE_W || xB > CLIPSPACE_W || xC > CLIPSPACE_W) {
            return false;
        }
        if (yA < -CLIPSPACE_W || yB < -CLIPSPACE_W || yC < -CLIPSPACE_W || xA < -CLIPSPACE_W || xB < -CLIPSPACE_W || xC < -CLIPSPACE_W) {
            return false;
        }
        
        if (this.state == 2) {
            int i_10 = this.culling.oneThirdViewportWidth * yA + xA;
            if (i_10 >= 0 && i_10 < this.culling.anIntArray2565.length && (zA << 8) - 39168 < this.culling.anIntArray2565[i_10]) {
                return false;
            }

            i_10 = xB + this.culling.oneThirdViewportWidth * yB;
            if (i_10 >= 0 && i_10 < this.culling.anIntArray2565.length && (zB << 8) - 39168 < this.culling.anIntArray2565[i_10]) {
                return false;
            }

            i_10 = yC * this.culling.oneThirdViewportWidth + xC;
            if (i_10 >= 0 && i_10 < this.culling.anIntArray2565.length && (zC << 8) - 39168 < this.culling.anIntArray2565[i_10]) {
                return false;
            }
        }
        int slopeXBa = xB - xA;
        int slopeYBa = yB - yA;
        int slopeXCa = xC - xA;
        int slopeYCa = yC - yA;
        int slopeZBa = zB - zA;
        int slopeZCa = zC - zA;
        if (yA < yB && yA < yC) {
            yA--;
            if (yB > yC) {
                yB++;
            } else {
                yC++;
            }
        } else if (yB < yC) {
            yB--;
            if (yA > yC) {
                yA++;
            } else {
                yC++;
            }
        } else {
            yC--;
            if (yA > yB) {
                yA++;
            } else {
                yB++;
            }
        }

        int perspectiveBa = 0;
        if (yB != yA) {
            perspectiveBa = (xB - xA << 12) / (yB - yA);
        }

        int perspectiveCb = 0;
        if (yC != yB) {
            perspectiveCb = (xC - xB << 12) / (yC - yB);
        }

        int perspectiveAc = 0;
        if (yC != yA) {
            perspectiveAc = (xA - xC << 12) / (yA - yC);
        }

        int size2d = slopeXBa * slopeYCa - slopeXCa * slopeYBa;
        if (size2d == 0) {
            return false;
        }
        int i_20 = (slopeZBa * slopeYCa - slopeZCa * slopeYBa << 8) / size2d;
        int i_21 = (slopeZCa * slopeXBa - slopeZBa * slopeXCa << 8) / size2d;
        if (yA <= yB && yA <= yC) {
            if (yA >= this.culling.oneThirdViewportHeight) {
                return true;
            }
            if (yB > this.culling.oneThirdViewportHeight) {
                yB = this.culling.oneThirdViewportHeight;
            }

            if (yC > this.culling.oneThirdViewportHeight) {
                yC = this.culling.oneThirdViewportHeight;
            }

            zA = i_20 + ((zA << 8) - i_20 * xA);
            if (yB < yC) {
                xC = xA <<= 12;
                if (yA < 0) {
                    xC -= perspectiveAc * yA;
                    xA -= perspectiveBa * yA;
                    zA -= i_21 * yA;
                    yA = 0;
                }

                xB <<= 12;
                if (yB < 0) {
                    xB -= perspectiveCb * yB;
                    yB = 0;
                }

                if (yB != yA ? perspectiveAc < perspectiveBa : perspectiveAc > perspectiveCb) {
                    yC -= yB;
                    yB -= yA;
                    yA = this.culling.oneThirdViewportWidth * yA;

                    while (true) {
                        if (--yB < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yA, (xC >> 12) - 1, (xB >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xC += perspectiveAc;
                                xB += perspectiveCb;
                                zA += i_21;
                                yA += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yA, (xC >> 12) - 1, (xA >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xC += perspectiveAc;
                        xA += perspectiveBa;
                        zA += i_21;
                        yA += this.culling.oneThirdViewportWidth;
                    }
                } else {
                    yC -= yB;
                    yB -= yA;
                    yA = this.culling.oneThirdViewportWidth * yA;

                    while (true) {
                        if (--yB < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yA, (xB >> 12) - 1, (xC >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xC += perspectiveAc;
                                xB += perspectiveCb;
                                zA += i_21;
                                yA += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yA, (xA >> 12) - 1, (xC >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xC += perspectiveAc;
                        xA += perspectiveBa;
                        zA += i_21;
                        yA += this.culling.oneThirdViewportWidth;
                    }
                }
            } else {
                xB = xA <<= 12;
                if (yA < 0) {
                    xB -= perspectiveAc * yA;
                    xA -= perspectiveBa * yA;
                    zA -= i_21 * yA;
                    yA = 0;
                }

                xC <<= 12;
                if (yC < 0) {
                    xC -= yC * perspectiveCb;
                    yC = 0;
                }

                if (yC != yA ? perspectiveAc < perspectiveBa : perspectiveCb > perspectiveBa) {
                    yB -= yC;
                    yC -= yA;
                    yA = this.culling.oneThirdViewportWidth * yA;

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yB < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yA, (xC >> 12) - 1, (xA >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xC += perspectiveCb;
                                xA += perspectiveBa;
                                zA += i_21;
                                yA += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yA, (xB >> 12) - 1, (xA >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xB += perspectiveAc;
                        xA += perspectiveBa;
                        zA += i_21;
                        yA += this.culling.oneThirdViewportWidth;
                    }
                } else {
                    yB -= yC;
                    yC -= yA;
                    yA = this.culling.oneThirdViewportWidth * yA;

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yB < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yA, (xA >> 12) - 1, (xC >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xC += perspectiveCb;
                                xA += perspectiveBa;
                                zA += i_21;
                                yA += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yA, (xA >> 12) - 1, (xB >> 12) + 1, zA, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xB += perspectiveAc;
                        xA += perspectiveBa;
                        zA += i_21;
                        yA += this.culling.oneThirdViewportWidth;
                    }
                }
            }
        } else if (yB <= yC) {
            if (yB >= this.culling.oneThirdViewportHeight) {
                return true;
            }
            if (yC > this.culling.oneThirdViewportHeight) {
                yC = this.culling.oneThirdViewportHeight;
            }

            if (yA > this.culling.oneThirdViewportHeight) {
                yA = this.culling.oneThirdViewportHeight;
            }

            zB = i_20 + ((zB << 8) - xB * i_20);
            if (yC < yA) {
                xA = xB <<= 12;
                if (yB < 0) {
                    xA -= perspectiveBa * yB;
                    xB -= perspectiveCb * yB;
                    zB -= i_21 * yB;
                    yB = 0;
                }

                xC <<= 12;
                if (yC < 0) {
                    xC -= yC * perspectiveAc;
                    yC = 0;
                }

                if (yC != yB ? perspectiveBa < perspectiveCb : perspectiveBa > perspectiveAc) {
                    yA -= yC;
                    yC -= yB;
                    yB = this.culling.oneThirdViewportWidth * yB;

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yA < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yB, (xA >> 12) - 1, (xC >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xA += perspectiveBa;
                                xC += perspectiveAc;
                                zB += i_21;
                                yB += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yB, (xA >> 12) - 1, (xB >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xA += perspectiveBa;
                        xB += perspectiveCb;
                        zB += i_21;
                        yB += this.culling.oneThirdViewportWidth;
                    }
                } else {
                    yA -= yC;
                    yC -= yB;
                    yB = this.culling.oneThirdViewportWidth * yB;

                    while (true) {
                        if (--yC < 0) {
                            while (true) {
                                if (--yA < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yB, (xC >> 12) - 1, (xA >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xA += perspectiveBa;
                                xC += perspectiveAc;
                                zB += i_21;
                                yB += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yB, (xB >> 12) - 1, (xA >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xA += perspectiveBa;
                        xB += perspectiveCb;
                        zB += i_21;
                        yB += this.culling.oneThirdViewportWidth;
                    }
                }
            } else {
                xC = xB <<= 12;
                if (yB < 0) {
                    xC -= perspectiveBa * yB;
                    xB -= perspectiveCb * yB;
                    zB -= i_21 * yB;
                    yB = 0;
                }

                xA <<= 12;
                if (yA < 0) {
                    xA -= perspectiveAc * yA;
                    yA = 0;
                }

                if (perspectiveBa < perspectiveCb) {
                    yC -= yA;
                    yA -= yB;
                    yB = this.culling.oneThirdViewportWidth * yB;

                    while (true) {
                        if (--yA < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yB, (xA >> 12) - 1, (xB >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xA += perspectiveAc;
                                xB += perspectiveCb;
                                zB += i_21;
                                yB += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yB, (xC >> 12) - 1, (xB >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xC += perspectiveBa;
                        xB += perspectiveCb;
                        zB += i_21;
                        yB += this.culling.oneThirdViewportWidth;
                    }
                } else {
                    yC -= yA;
                    yA -= yB;
                    yB = this.culling.oneThirdViewportWidth * yB;

                    while (true) {
                        if (--yA < 0) {
                            while (true) {
                                if (--yC < 0) {
                                    return true;
                                }

                                if (!method3373(this.culling.anIntArray2565, yB, (xB >> 12) - 1, (xA >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                                    return false;
                                }

                                xA += perspectiveAc;
                                xB += perspectiveCb;
                                zB += i_21;
                                yB += this.culling.oneThirdViewportWidth;
                            }
                        }

                        if (!method3373(this.culling.anIntArray2565, yB, (xB >> 12) - 1, (xC >> 12) + 1, zB, i_20, this.culling.oneThirdViewportWidth)) {
                            return false;
                        }

                        xC += perspectiveBa;
                        xB += perspectiveCb;
                        zB += i_21;
                        yB += this.culling.oneThirdViewportWidth;
                    }
                }
            }
        } else if (yC >= this.culling.oneThirdViewportHeight) {
            return true;
        }
        if (yA > this.culling.oneThirdViewportHeight) {
            yA = this.culling.oneThirdViewportHeight;
        }

        if (yB > this.culling.oneThirdViewportHeight) {
            yB = this.culling.oneThirdViewportHeight;
        }

        zC = i_20 + ((zC << 8) - i_20 * xC);
        if (yA < yB) {
            xB = xC <<= 12;
            if (yC < 0) {
                xB -= yC * perspectiveCb;
                xC -= yC * perspectiveAc;
                zC -= yC * i_21;
                yC = 0;
            }

            xA <<= 12;
            if (yA < 0) {
                xA -= perspectiveBa * yA;
                yA = 0;
            }

            if (perspectiveCb < perspectiveAc) {
                yB -= yA;
                yA -= yC;
                yC *= this.culling.oneThirdViewportWidth;

                while (true) {
                    if (--yA < 0) {
                        while (true) {
                            if (--yB < 0) {
                                return true;
                            }

                            if (!method3373(this.culling.anIntArray2565, yC, (xB >> 12) - 1, (xA >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false;
                            }

                            xB += perspectiveCb;
                            xA += perspectiveBa;
                            zC += i_21;
                            yC += this.culling.oneThirdViewportWidth;
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565, yC, (xB >> 12) - 1, (xC >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false;
                    }

                    xB += perspectiveCb;
                    xC += perspectiveAc;
                    zC += i_21;
                    yC += this.culling.oneThirdViewportWidth;
                }
            } else {
                yB -= yA;
                yA -= yC;
                yC *= this.culling.oneThirdViewportWidth;

                while (true) {
                    if (--yA < 0) {
                        while (true) {
                            if (--yB < 0) {
                                return true;
                            }

                            if (!method3373(this.culling.anIntArray2565, yC, (xA >> 12) - 1, (xB >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false;
                            }

                            xB += perspectiveCb;
                            xA += perspectiveBa;
                            zC += i_21;
                            yC += this.culling.oneThirdViewportWidth;
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565, yC, (xC >> 12) - 1, (xB >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false;
                    }

                    xB += perspectiveCb;
                    xC += perspectiveAc;
                    zC += i_21;
                    yC += this.culling.oneThirdViewportWidth;
                }
            }
        } else {
            xA = xC <<= 12;
            if (yC < 0) {
                xA -= yC * perspectiveCb;
                xC -= yC * perspectiveAc;
                zC -= yC * i_21;
                yC = 0;
            }

            xB <<= 12;
            if (yB < 0) {
                xB -= perspectiveBa * yB;
                yB = 0;
            }

            if (perspectiveCb < perspectiveAc) {
                yA -= yB;
                yB -= yC;
                yC *= this.culling.oneThirdViewportWidth;

                while (true) {
                    if (--yB < 0) {
                        while (true) {
                            if (--yA < 0) {
                                return true;
                            }

                            if (!method3373(this.culling.anIntArray2565, yC, (xB >> 12) - 1, (xC >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false;
                            }

                            xB += perspectiveBa;
                            xC += perspectiveAc;
                            zC += i_21;
                            yC += this.culling.oneThirdViewportWidth;
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565, yC, (xA >> 12) - 1, (xC >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false;
                    }

                    xA += perspectiveCb;
                    xC += perspectiveAc;
                    zC += i_21;
                    yC += this.culling.oneThirdViewportWidth;
                }
            } else {
                yA -= yB;
                yB -= yC;
                yC *= this.culling.oneThirdViewportWidth;

                while (true) {
                    if (--yB < 0) {
                        while (true) {
                            if (--yA < 0) {
                                return true;
                            }

                            if (!method3373(this.culling.anIntArray2565, yC, (xC >> 12) - 1, (xB >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                                return false;
                            }

                            xB += perspectiveBa;
                            xC += perspectiveAc;
                            zC += i_21;
                            yC += this.culling.oneThirdViewportWidth;
                        }
                    }

                    if (!method3373(this.culling.anIntArray2565, yC, (xC >> 12) - 1, (xA >> 12) + 1, zC, i_20, this.culling.oneThirdViewportWidth)) {
                        return false;
                    }

                    xA += perspectiveCb;
                    xC += perspectiveAc;
                    zC += i_21;
                    yC += this.culling.oneThirdViewportWidth;
                }
            }
        }
    }

    public boolean method3373(int[] ints_1, int i_2, int i_4, int i_5, int i_6, int i_7, int i_8) {
        int i_51 = i_5;
        int i_41 = i_4;
        int i_31 = 0;
        if (i_51 > i_8) {
            i_51 = i_8;
        }
        if (i_41 < 0) {
            i_41 = 0;
        }
        if (i_41 >= i_51) {
            return true;
        } 
        i_2 += i_41 - 1;
        i_31 = i_51 - i_41 >> 2;
        i_6 += i_41 * i_7;
        if (this.state == 1) {
            this.anInt2588 += i_31;

            while (true) {
                if (--i_31 < 0) {
                    i_31 = i_51 - i_41 & 0x3;

                    while (true) {
                        if (--i_31 < 0) {
                            return true;
                        }

                        if (i_6 < ints_1[++i_2]) {
                            ints_1[i_2] = i_6;
                        }

                        i_6 += i_7;
                    }
                }

                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6;
                }

                i_6 += i_7;
                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6;
                }

                i_6 += i_7;
                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6;
                }

                i_6 += i_7;
                if (i_6 < ints_1[++i_2]) {
                    ints_1[i_2] = i_6;
                }

                i_6 += i_7;
            }
        } else {
            i_6 -= 39168;

            while (true) {
                if (--i_31 < 0) {
                    i_31 = i_51 - i_41 & 0x3;

                    while (true) {
                        if (--i_31 < 0) {
                            return true;
                        }

                        if (i_6 < ints_1[++i_2]) {
                            return false;
                        }

                        i_6 += i_7;
                    }
                }

                if (i_6 < ints_1[++i_2]) {
                    return false;
                }

                i_6 += i_7;
                if (i_6 < ints_1[++i_2]) {
                    return false;
                }

                i_6 += i_7;
                if (i_6 < ints_1[++i_2]) {
                    return false;
                }

                i_6 += i_7;
                if (i_6 < ints_1[++i_2]) {
                    return false;
                }

                i_6 += i_7;
            }
        }
    }

}
