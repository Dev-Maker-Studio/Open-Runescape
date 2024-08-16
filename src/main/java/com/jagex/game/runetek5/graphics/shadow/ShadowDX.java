package com.jagex.game.runetek5.graphics.shadow;

public class ShadowDX extends Shadow {

	public int minX;
    public int minZ;
    public int sizeX;
    public int sizeZ;
    public final byte[] shadowRaster;

    public ShadowDX(int i_2, int i_3) {
        this.shadowRaster = new byte[i_3 * i_2];
    }

    public static void calculateShadowScanline(byte[] raster, int position, int i_3, int i_4) {
        int i_21 = 0;
        if (i_3 < i_4) {
            position += i_3;
            i_21 = i_4 - i_3 >> 2;

            while (true) {
                --i_21;
                if (i_21 < 0) {
                    i_21 = i_4 - i_3 & 0x3;

                    while (true) {
                        --i_21;
                        if (i_21 < 0) {
                            return;
                        }

                        raster[position++] = 1;
                    }
                }

                raster[position++] = 1;
                raster[position++] = 1;
                raster[position++] = 1;
                raster[position++] = 1;
            }
        }
    }

    public boolean isClean(int width, int length) {
        return this.shadowRaster.length >= length * width;
    }

    public void setSize(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.sizeX = maxX - minX;
        this.sizeZ = maxZ - minZ;
    }

    public void clearPixels() {
        int pixelIndex = -1;

        for (int index = this.shadowRaster.length - 8; pixelIndex < index; this.shadowRaster[pixelIndex] = 0) {
            this.shadowRaster[++pixelIndex] = 0;
            this.shadowRaster[++pixelIndex] = 0;
            this.shadowRaster[++pixelIndex] = 0;
            this.shadowRaster[++pixelIndex] = 0;
            this.shadowRaster[++pixelIndex] = 0;
            this.shadowRaster[++pixelIndex] = 0;
            this.shadowRaster[++pixelIndex] = 0;
            pixelIndex++;
        }

        while (pixelIndex < this.shadowRaster.length - 1) {
            this.shadowRaster[++pixelIndex] = 0;
        }

    }

    public void calculateShadowTriangle(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        int i_61 = i_6;
        int i_51 = i_5;
        int i_11 = i_1;
        int i_41 = i_4;
        int i_31 = i_3;
        int i_21 = i_2;
        int i_7 = 0;
        if (i_21 != i_11) {
            i_7 = (i_51 - i_41 << 16) / (i_21 - i_11);
        }

        int i_8 = 0;
        if (i_31 != i_21) {
            i_8 = (i_61 - i_51 << 16) / (i_31 - i_21);
        }

        int i_9 = 0;
        if (i_31 != i_11) {
            i_9 = (i_41 - i_61 << 16) / (i_11 - i_31);
        }

        if (i_11 <= i_21 && i_11 <= i_31) {
            if (i_21 < i_31) {
                i_61 = i_41 <<= 16;
                if (i_11 < 0) {
                    i_61 -= i_9 * i_11;
                    i_41 -= i_7 * i_11;
                    i_11 = 0;
                }

                i_51 <<= 16;
                if (i_21 < 0) {
                    i_51 -= i_8 * i_21;
                    i_21 = 0;
                }

                if (i_21 != i_11 ? i_9 < i_7 : i_9 > i_8) {
                    i_31 -= i_21;
                    i_21 -= i_11;
                    i_11 = this.sizeX * i_11;

                    while (true) {
                        --i_21;
                        if (i_21 < 0) {
                            while (true) {
                                --i_31;
                                if (i_31 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_61 >> 16, i_51 >> 16);
                                i_61 += i_9;
                                i_51 += i_8;
                                i_11 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_61 >> 16, i_41 >> 16);
                        i_61 += i_9;
                        i_41 += i_7;
                        i_11 += this.sizeX;
                    }
                } else {
                    i_31 -= i_21;
                    i_21 -= i_11;
                    i_11 = this.sizeX * i_11;

                    while (true) {
                        --i_21;
                        if (i_21 < 0) {
                            while (true) {
                                --i_31;
                                if (i_31 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_51 >> 16, i_61 >> 16);
                                i_61 += i_9;
                                i_51 += i_8;
                                i_11 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_41 >> 16, i_61 >> 16);
                        i_61 += i_9;
                        i_41 += i_7;
                        i_11 += this.sizeX;
                    }
                }
            } else {
                i_51 = i_41 <<= 16;
                if (i_11 < 0) {
                    i_51 -= i_9 * i_11;
                    i_41 -= i_7 * i_11;
                    i_11 = 0;
                }

                i_61 <<= 16;
                if (i_31 < 0) {
                    i_61 -= i_31 * i_8;
                    i_31 = 0;
                }

                if ((i_31 == i_11 || i_9 >= i_7) && (i_31 != i_11 || i_8 <= i_7)) {
                    i_21 -= i_31;
                    i_31 -= i_11;
                    i_11 = this.sizeX * i_11;

                    while (true) {
                        --i_31;
                        if (i_31 < 0) {
                            while (true) {
                                --i_21;
                                if (i_21 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_41 >> 16, i_61 >> 16);
                                i_61 += i_8;
                                i_41 += i_7;
                                i_11 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_41 >> 16, i_51 >> 16);
                        i_51 += i_9;
                        i_41 += i_7;
                        i_11 += this.sizeX;
                    }
                } else {
                    i_21 -= i_31;
                    i_31 -= i_11;
                    i_11 = this.sizeX * i_11;

                    while (true) {
                        --i_31;
                        if (i_31 < 0) {
                            while (true) {
                                --i_21;
                                if (i_21 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_11, i_61 >> 16, i_41 >> 16);
                                i_61 += i_8;
                                i_41 += i_7;
                                i_11 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_11, i_51 >> 16, i_41 >> 16);
                        i_51 += i_9;
                        i_41 += i_7;
                        i_11 += this.sizeX;
                    }
                }
            }
        } else if (i_21 <= i_31) {
            if (i_31 < i_11) {
                i_41 = i_51 <<= 16;
                if (i_21 < 0) {
                    i_41 -= i_7 * i_21;
                    i_51 -= i_8 * i_21;
                    i_21 = 0;
                }

                i_61 <<= 16;
                if (i_31 < 0) {
                    i_61 -= i_31 * i_9;
                    i_31 = 0;
                }

                if ((i_31 == i_21 || i_7 >= i_8) && (i_31 != i_21 || i_7 <= i_9)) {
                    i_11 -= i_31;
                    i_31 -= i_21;
                    i_21 *= this.sizeX;

                    while (true) {
                        --i_31;
                        if (i_31 < 0) {
                            while (true) {
                                --i_11;
                                if (i_11 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_61 >> 16, i_41 >> 16);
                                i_41 += i_7;
                                i_61 += i_9;
                                i_21 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_51 >> 16, i_41 >> 16);
                        i_41 += i_7;
                        i_51 += i_8;
                        i_21 += this.sizeX;
                    }
                } else {
                    i_11 -= i_31;
                    i_31 -= i_21;
                    i_21 *= this.sizeX;

                    while (true) {
                        --i_31;
                        if (i_31 < 0) {
                            while (true) {
                                --i_11;
                                if (i_11 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_41 >> 16, i_61 >> 16);
                                i_41 += i_7;
                                i_61 += i_9;
                                i_21 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_41 >> 16, i_51 >> 16);
                        i_41 += i_7;
                        i_51 += i_8;
                        i_21 += this.sizeX;
                    }
                }
            } else {
                i_61 = i_51 <<= 16;
                if (i_21 < 0) {
                    i_61 -= i_7 * i_21;
                    i_51 -= i_8 * i_21;
                    i_21 = 0;
                }

                i_41 <<= 16;
                if (i_11 < 0) {
                    i_41 -= i_9 * i_11;
                    i_11 = 0;
                }

                if (i_7 < i_8) {
                    i_31 -= i_11;
                    i_11 -= i_21;
                    i_21 *= this.sizeX;

                    while (true) {
                        --i_11;
                        if (i_11 < 0) {
                            while (true) {
                                --i_31;
                                if (i_31 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_41 >> 16, i_51 >> 16);
                                i_41 += i_9;
                                i_51 += i_8;
                                i_21 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_61 >> 16, i_51 >> 16);
                        i_61 += i_7;
                        i_51 += i_8;
                        i_21 += this.sizeX;
                    }
                } else {
                    i_31 -= i_11;
                    i_11 -= i_21;
                    i_21 *= this.sizeX;

                    while (true) {
                        --i_11;
                        if (i_11 < 0) {
                            while (true) {
                                --i_31;
                                if (i_31 < 0) {
                                    return;
                                }

                                calculateShadowScanline(this.shadowRaster, i_21, i_51 >> 16, i_41 >> 16);
                                i_41 += i_9;
                                i_51 += i_8;
                                i_21 += this.sizeX;
                            }
                        }

                        calculateShadowScanline(this.shadowRaster, i_21, i_51 >> 16, i_61 >> 16);
                        i_61 += i_7;
                        i_51 += i_8;
                        i_21 += this.sizeX;
                    }
                }
            }
        } else if (i_11 < i_21) {
            i_51 = i_61 <<= 16;
            if (i_31 < 0) {
                i_51 -= i_31 * i_8;
                i_61 -= i_31 * i_9;
                i_31 = 0;
            }

            i_41 <<= 16;
            if (i_11 < 0) {
                i_41 -= i_7 * i_11;
                i_11 = 0;
            }

            if (i_8 < i_9) {
                i_21 -= i_11;
                i_11 -= i_31;
                i_31 *= this.sizeX;

                while (true) {
                    --i_11;
                    if (i_11 < 0) {
                        while (true) {
                            --i_21;
                            if (i_21 < 0) {
                                return;
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_51 >> 16, i_41 >> 16);
                            i_51 += i_8;
                            i_41 += i_7;
                            i_31 += this.sizeX;
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_51 >> 16, i_61 >> 16);
                    i_51 += i_8;
                    i_61 += i_9;
                    i_31 += this.sizeX;
                }
            } else {
                i_21 -= i_11;
                i_11 -= i_31;
                i_31 *= this.sizeX;

                while (true) {
                    --i_11;
                    if (i_11 < 0) {
                        while (true) {
                            --i_21;
                            if (i_21 < 0) {
                                return;
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_41 >> 16, i_51 >> 16);
                            i_51 += i_8;
                            i_41 += i_7;
                            i_31 += this.sizeX;
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_61 >> 16, i_51 >> 16);
                    i_51 += i_8;
                    i_61 += i_9;
                    i_31 += this.sizeX;
                }
            }
        } else {
            i_41 = i_61 <<= 16;
            if (i_31 < 0) {
                i_41 -= i_31 * i_8;
                i_61 -= i_31 * i_9;
                i_31 = 0;
            }

            i_51 <<= 16;
            if (i_21 < 0) {
                i_51 -= i_7 * i_21;
                i_21 = 0;
            }

            if (i_8 < i_9) {
                i_11 -= i_21;
                i_21 -= i_31;
                i_31 *= this.sizeX;

                while (true) {
                    --i_21;
                    if (i_21 < 0) {
                        while (true) {
                            --i_11;
                            if (i_11 < 0) {
                                return;
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_51 >> 16, i_61 >> 16);
                            i_51 += i_7;
                            i_61 += i_9;
                            i_31 += this.sizeX;
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_41 >> 16, i_61 >> 16);
                    i_41 += i_8;
                    i_61 += i_9;
                    i_31 += this.sizeX;
                }
            } else {
                i_11 -= i_21;
                i_21 -= i_31;
                i_31 *= this.sizeX;

                while (true) {
                    --i_21;
                    if (i_21 < 0) {
                        while (true) {
                            --i_11;
                            if (i_11 < 0) {
                                return;
                            }

                            calculateShadowScanline(this.shadowRaster, i_31, i_61 >> 16, i_51 >> 16);
                            i_51 += i_7;
                            i_61 += i_9;
                            i_31 += this.sizeX;
                        }
                    }

                    calculateShadowScanline(this.shadowRaster, i_31, i_61 >> 16, i_41 >> 16);
                    i_41 += i_8;
                    i_61 += i_9;
                    i_31 += this.sizeX;
                }
            }
        }
    }
    
    /*FIXME: 876 below*/
	void calculateShadowTriangle876(int i, int i6, int i7, int i8, int i9, int i10) {
		int i11 = 0;
		if (i6 != i)
			i11 = (i9 - i8 << 16) / (i6 - i);
		int i12 = 0;
		if (i7 != i6)
			i12 = (i10 - i9 << 16) / (i7 - i6);
		int i13 = 0;
		if (i7 != i)
			i13 = (i8 - i10 << 16) / (i - i7);
		if (i <= i6 && i <= i7) {
			if (i6 < i7) {
				i10 = i8 <<= 16;
				if (i < 0) {
					i10 -= i13 * i;
					i8 -= i11 * i;
					i = 0;
				}
				i9 <<= 16;
				if (i6 < 0) {
					i9 -= i12 * i6;
					i6 = 0;
				}
				if (i != i6 && i13 < i11 || i == i6 && i13 > i12) {
					i7 -= i6;
					i6 -= i;
					i *= this.sizeX;
					while (--i6 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i10 >> 16, i8 >> 16);
						i10 += i13;
						i8 += i11;
						i += this.sizeX;
					}
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i10 >> 16, i9 >> 16);
						i10 += i13;
						i9 += i12;
						i += this.sizeX;
					}
				} else {
					i7 -= i6;
					i6 -= i;
					i *= this.sizeX;
					while (--i6 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i8 >> 16, i10 >> 16);
						i10 += i13;
						i8 += i11;
						i += this.sizeX;
					}
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i9 >> 16, i10 >> 16);
						i10 += i13;
						i9 += i12;
						i += this.sizeX;
					}
				}
			} else {
				i9 = i8 <<= 16;
				if (i < 0) {
					i9 -= i13 * i;
					i8 -= i11 * i;
					i = 0;
				}
				i10 <<= 16;
				if (i7 < 0) {
					i10 -= i12 * i7;
					i7 = 0;
				}
				if (i != i7 && i13 < i11 || i == i7 && i12 > i11) {
					i6 -= i7;
					i7 -= i;
					i *= this.sizeX;
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i9 >> 16, i8 >> 16);
						i9 += i13;
						i8 += i11;
						i += this.sizeX;
					}
					while (--i6 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i10 >> 16, i8 >> 16);
						i10 += i12;
						i8 += i11;
						i += this.sizeX;
					}
				} else {
					i6 -= i7;
					i7 -= i;
					i *= this.sizeX;
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i8 >> 16, i9 >> 16);
						i9 += i13;
						i8 += i11;
						i += this.sizeX;
					}
					while (--i6 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i, 0, i8 >> 16, i10 >> 16);
						i10 += i12;
						i8 += i11;
						i += this.sizeX;
					}
				}
			}
		} else if (i6 <= i7) {
			if (i7 < i) {
				i8 = i9 <<= 16;
				if (i6 < 0) {
					i8 -= i11 * i6;
					i9 -= i12 * i6;
					i6 = 0;
				}
				i10 <<= 16;
				if (i7 < 0) {
					i10 -= i13 * i7;
					i7 = 0;
				}
				if (i6 != i7 && i11 < i12 || i6 == i7 && i11 > i13) {
					i -= i7;
					i7 -= i6;
					i6 *= this.sizeX;
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i8 >> 16, i9 >> 16);
						i8 += i11;
						i9 += i12;
						i6 += this.sizeX;
					}
					while (--i >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i8 >> 16, i10 >> 16);
						i8 += i11;
						i10 += i13;
						i6 += this.sizeX;
					}
				} else {
					i -= i7;
					i7 -= i6;
					i6 *= this.sizeX;
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i9 >> 16, i8 >> 16);
						i8 += i11;
						i9 += i12;
						i6 += this.sizeX;
					}
					while (--i >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i10 >> 16, i8 >> 16);
						i8 += i11;
						i10 += i13;
						i6 += this.sizeX;
					}
				}
			} else {
				i10 = i9 <<= 16;
				if (i6 < 0) {
					i10 -= i11 * i6;
					i9 -= i12 * i6;
					i6 = 0;
				}
				i8 <<= 16;
				if (i < 0) {
					i8 -= i13 * i;
					i = 0;
				}
				if (i11 < i12) {
					i7 -= i;
					i -= i6;
					i6 *= this.sizeX;
					while (--i >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i10 >> 16, i9 >> 16);
						i10 += i11;
						i9 += i12;
						i6 += this.sizeX;
					}
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i8 >> 16, i9 >> 16);
						i8 += i13;
						i9 += i12;
						i6 += this.sizeX;
					}
				} else {
					i7 -= i;
					i -= i6;
					i6 *= this.sizeX;
					while (--i >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i9 >> 16, i10 >> 16);
						i10 += i11;
						i9 += i12;
						i6 += this.sizeX;
					}
					while (--i7 >= 0) {
						calculateShadowScanline876(this.shadowRaster, i6, 0, i9 >> 16, i8 >> 16);
						i8 += i13;
						i9 += i12;
						i6 += this.sizeX;
					}
				}
			}
		} else if (i < i6) {
			i9 = i10 <<= 16;
			if (i7 < 0) {
				i9 -= i12 * i7;
				i10 -= i13 * i7;
				i7 = 0;
			}
			i8 <<= 16;
			if (i < 0) {
				i8 -= i11 * i;
				i = 0;
			}
			if (i12 < i13) {
				i6 -= i;
				i -= i7;
				i7 *= this.sizeX;
				while (--i >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i9 >> 16, i10 >> 16);
					i9 += i12;
					i10 += i13;
					i7 += this.sizeX;
				}
				while (--i6 >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i9 >> 16, i8 >> 16);
					i9 += i12;
					i8 += i11;
					i7 += this.sizeX;
				}
			} else {
				i6 -= i;
				i -= i7;
				i7 *= this.sizeX;
				while (--i >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i10 >> 16, i9 >> 16);
					i9 += i12;
					i10 += i13;
					i7 += this.sizeX;
				}
				while (--i6 >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i8 >> 16, i9 >> 16);
					i9 += i12;
					i8 += i11;
					i7 += this.sizeX;
				}
			}
		} else {
			i8 = i10 <<= 16;
			if (i7 < 0) {
				i8 -= i12 * i7;
				i10 -= i13 * i7;
				i7 = 0;
			}
			i9 <<= 16;
			if (i6 < 0) {
				i9 -= i11 * i6;
				i6 = 0;
			}
			if (i12 < i13) {
				i -= i6;
				i6 -= i7;
				i7 *= this.sizeX;
				while (--i6 >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i8 >> 16, i10 >> 16);
					i8 += i12;
					i10 += i13;
					i7 += this.sizeX;
				}
				while (--i >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i9 >> 16, i10 >> 16);
					i9 += i11;
					i10 += i13;
					i7 += this.sizeX;
				}
			} else {
				i -= i6;
				i6 -= i7;
				i7 *= this.sizeX;
				while (--i6 >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i10 >> 16, i8 >> 16);
					i8 += i12;
					i10 += i13;
					i7 += this.sizeX;
				}
				while (--i >= 0) {
					calculateShadowScanline876(this.shadowRaster, i7, 0, i10 >> 16, i9 >> 16);
					i9 += i11;
					i10 += i13;
					i7 += this.sizeX;
				}
			}
		}
	}

	static final void calculateShadowScanline876(byte[] is, int i, int i14, int i15, int i16) {
		if (i15 < i16) {
			i += i15;
			i14 = i16 - i15 >> 2;
			while (--i14 >= 0) {
				is[i++] = (byte) 1;
				is[i++] = (byte) 1;
				is[i++] = (byte) 1;
				is[i++] = (byte) 1;
			}
			i14 = i16 - i15 & 0x3;
			while (--i14 >= 0)
				is[i++] = (byte) 1;
		}
	}
}
