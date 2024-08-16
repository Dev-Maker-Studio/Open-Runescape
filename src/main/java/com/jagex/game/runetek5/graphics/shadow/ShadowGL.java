package com.jagex.game.runetek5.graphics.shadow;

import com.jagex.game.launch.Loader;

public class ShadowGL extends Shadow {

	public final byte[] shade;
	public int startX;
	public int startY;
    public int sizeX;
    public int sizeY;

    public ShadowGL(int sizeX, int sizeY) {
        this.shade = new byte[sizeY * sizeX];
    }

	public static void markShadedScanline(byte[] floorShade, int shadowIndex, int rotation, int rotateUntil) {
		int rotationOffset = 0;
		if (rotation < rotateUntil) {
			shadowIndex += rotation;
			rotationOffset = rotateUntil - rotation >> 2;
			for (;;) {
				if (--rotationOffset < 0) {
					rotationOffset = rotateUntil - rotation & 0x3;
					for (;;) {
						if (--rotationOffset < 0) {
							return;
						}
						floorShade[shadowIndex++] = 1;
					}
				}
				floorShade[shadowIndex++] = 1;
				floorShade[shadowIndex++] = 1;
				floorShade[shadowIndex++] = 1;
				floorShade[shadowIndex++] = 1;
			}
		}
	}

    public  void reset() {
        int count = -1;
        for (int i_2 = this.shade.length - 8; count < i_2; this.shade[count] = 0) {
            this.shade[++count] = 0;
            this.shade[++count] = 0;
            this.shade[++count] = 0;
            this.shade[++count] = 0;
            this.shade[++count] = 0;
            this.shade[++count] = 0;
            this.shade[++count] = 0;
            count++;
        }
        while (count < this.shade.length - 1) {
            this.shade[++count] = 0;
        }
    }
    
    public void markShadedTriangle(int zA, int zB, int zC, int xA, int xB, int xC) {
    	if (Loader.MULTI_THREADED_MAPLOADING) {
    		this.markShadedTriangle876(zA, zB, zC, xA, xB, xC);
    		return;
    	}
        int slopeBa = 0;
        if (zB != zA) {
            slopeBa = (xB - xA << 16) / (zB - zA);
        }
        int slopeCb = 0;
        if (zC != zB) {
            slopeCb = (xC - xB << 16) / (zC - zB);
        }
        int slopeAc = 0;
        if (zC != zA) {
            slopeAc = (xA - xC << 16) / (zA - zC);
        }
        if (zA <= zB && zA <= zC) {
            if (zB < zC) {
                xA = (xC = xA << 16);
                if (zA < 0) {
                    xC -= slopeAc * zA;
                    xA -= slopeBa * zA;
                    zA = 0;
                }
                xB <<= 16;
                if (zB < 0) {
                    xB -= slopeCb * zB;
                    zB = 0;
                }                
                do {
                    if (zB != zA) {
                        if (slopeAc >= slopeBa) {
                            break;
                        }
                    }
                    else if (slopeAc <= slopeCb) {
                        break;
                    }
                    zC -= zB;
                    zB -= zA;
                    zA *= this.sizeX;
                    while (--zB >= 0) {
                        markShadedScanline(this.shade, zA, xC >> 16, xA >> 16);
                        xC += slopeAc;
                        xA += slopeBa;
                        zA += this.sizeX;
                    }
                    while (--zC >= 0) {
                        markShadedScanline(this.shade, zA, xC >> 16, xB >> 16);
                        xC += slopeAc;
                        xB += slopeCb;
                        zA += this.sizeX;
                    }
                    return;
                } while (false);
                zC -= zB;
                zB -= zA;
                zA *= this.sizeX;
                while (--zB >= 0) {
                    markShadedScanline(this.shade, zA, xA >> 16, xC >> 16);
                    xC += slopeAc;
                    xA += slopeBa;
                    zA += this.sizeX;
                }
                while (--zC >= 0) {
                    markShadedScanline(this.shade, zA, xB >> 16, xC >> 16);
                    xC += slopeAc;
                    xB += slopeCb;
                    zA += this.sizeX;
                }
                return;
            }
            xA = (xB = xA << 16);
            if (zA < 0) {
                xB -= slopeAc * zA;
                xA -= slopeBa * zA;
                zA = 0;
            }
            xC <<= 16;
            if (zC < 0) {
                xC -= zC * slopeCb;
                zC = 0;
            }
            if ((zC == zA || slopeAc >= slopeBa) && (zC != zA || slopeCb <= slopeBa)) {
                zB -= zC;
                zC -= zA;
                zA *= this.sizeX;
                while (--zC >= 0) {
                    markShadedScanline(this.shade, zA, xA >> 16, xB >> 16);
                    xB += slopeAc;
                    xA += slopeBa;
                    zA += this.sizeX;
                }
                while (--zB >= 0) {
                    markShadedScanline(this.shade, zA, xA >> 16, xC >> 16);
                    xC += slopeCb;
                    xA += slopeBa;
                    zA += this.sizeX;
                }
                return;
            }
            zB -= zC;
            zC -= zA;
            zA *= this.sizeX;
            while (--zC >= 0) {
                markShadedScanline(this.shade, zA, xB >> 16, xA >> 16);
                xB += slopeAc;
                xA += slopeBa;
                zA += this.sizeX;
            }
            while (--zB >= 0) {
                markShadedScanline(this.shade, zA, xC >> 16, xA >> 16);
                xC += slopeCb;
                xA += slopeBa;
                zA += this.sizeX;
            }
        }
        else if (zB <= zC) {
            if (zC < zA) {
                xB = (xA = xB << 16);
                if (zB < 0) {
                    xA -= slopeBa * zB;
                    xB -= slopeCb * zB;
                    zB = 0;
                }
                xC <<= 16;
                if (zC < 0) {
                    xC -= zC * slopeAc;
                    zC = 0;
                }
                if ((zC == zB || slopeBa >= slopeCb) && (zC != zB || slopeBa <= slopeAc)) {
                    zA -= zC;
                    zC -= zB;
                    zB *= this.sizeX;
                    while (--zC >= 0) {
                        markShadedScanline(this.shade, zB, xB >> 16, xA >> 16);
                        xA += slopeBa;
                        xB += slopeCb;
                        zB += this.sizeX;
                    }
                    while (--zA >= 0) {
                        markShadedScanline(this.shade, zB, xC >> 16, xA >> 16);
                        xA += slopeBa;
                        xC += slopeAc;
                        zB += this.sizeX;
                    }
                    return;
                }
                zA -= zC;
                zC -= zB;
                zB *= this.sizeX;
                while (--zC >= 0) {
                    markShadedScanline(this.shade, zB, xA >> 16, xB >> 16);
                    xA += slopeBa;
                    xB += slopeCb;
                    zB += this.sizeX;
                }
                while (--zA >= 0) {
                    markShadedScanline(this.shade, zB, xA >> 16, xC >> 16);
                    xA += slopeBa;
                    xC += slopeAc;
                    zB += this.sizeX;
                }
            }
            else {
                xB = (xC = xB << 16);
                if (zB < 0) {
                    xC -= slopeBa * zB;
                    xB -= slopeCb * zB;
                    zB = 0;
                }
                xA <<= 16;
                if (zA < 0) {
                    xA -= slopeAc * zA;
                    zA = 0;
                }
                if (slopeBa < slopeCb) {
                    zC -= zA;
                    zA -= zB;
                    zB *= this.sizeX;
                    while (--zA >= 0) {
                        markShadedScanline(this.shade, zB, xC >> 16, xB >> 16);
                        xC += slopeBa;
                        xB += slopeCb;
                        zB += this.sizeX;
                    }
                    while (--zC >= 0) {
                        markShadedScanline(this.shade, zB, xA >> 16, xB >> 16);
                        xA += slopeAc;
                        xB += slopeCb;
                        zB += this.sizeX;
                    }
                    return;
                }
                zC -= zA;
                zA -= zB;
                zB *= this.sizeX;
                while (--zA >= 0) {
                    markShadedScanline(this.shade, zB, xB >> 16, xC >> 16);
                    xC += slopeBa;
                    xB += slopeCb;
                    zB += this.sizeX;
                }
                while (--zC >= 0) {
                    markShadedScanline(this.shade, zB, xB >> 16, xA >> 16);
                    xA += slopeAc;
                    xB += slopeCb;
                    zB += this.sizeX;
                }
            }
        }
        else if (zA < zB) {
            xC = (xB = xC << 16);
            if (zC < 0) {
                xB -= zC * slopeCb;
                xC -= zC * slopeAc;
                zC = 0;
            }
            xA <<= 16;
            if (zA < 0) {
                xA -= slopeBa * zA;
                zA = 0;
            }
            if (slopeCb < slopeAc) {
                zB -= zA;
                zA -= zC;
                zC *= this.sizeX;
                while (--zA >= 0) {
                    markShadedScanline(this.shade, zC, xB >> 16, xC >> 16);
                    xB += slopeCb;
                    xC += slopeAc;
                    zC += this.sizeX;
                }
                while (--zB >= 0) {
                    markShadedScanline(this.shade, zC, xB >> 16, xA >> 16);
                    xB += slopeCb;
                    xA += slopeBa;
                    zC += this.sizeX;
                }
                return;
            }
            zB -= zA;
            zA -= zC;
            zC *= this.sizeX;
            while (--zA >= 0) {
                markShadedScanline(this.shade, zC, xC >> 16, xB >> 16);
                xB += slopeCb;
                xC += slopeAc;
                zC += this.sizeX;
            }
            while (--zB >= 0) {
                markShadedScanline(this.shade, zC, xA >> 16, xB >> 16);
                xB += slopeCb;
                xA += slopeBa;
                zC += this.sizeX;
            }
        }
        else {
            xC = (xA = xC << 16);
            if (zC < 0) {
                xA -= zC * slopeCb;
                xC -= zC * slopeAc;
                zC = 0;
            }
            xB <<= 16;
            if (zB < 0) {
                xB -= slopeBa * zB;
                zB = 0;
            }
            if (slopeCb < slopeAc) {
                zA -= zB;
                zB -= zC;
                zC *= this.sizeX;
                while (--zB >= 0) {
                    markShadedScanline(this.shade, zC, xA >> 16, xC >> 16);
                    xA += slopeCb;
                    xC += slopeAc;
                    zC += this.sizeX;
                }
                while (--zA >= 0) {
                    markShadedScanline(this.shade, zC, xB >> 16, xC >> 16);
                    xB += slopeBa;
                    xC += slopeAc;
                    zC += this.sizeX;
                }
                return;
            }
            zA -= zB;
            zB -= zC;
            zC *= this.sizeX;
            while (--zB >= 0) {
                markShadedScanline(this.shade, zC, xC >> 16, xA >> 16);
                xA += slopeCb;
                xC += slopeAc;
                zC += this.sizeX;
            }
            while (--zA >= 0) {
                markShadedScanline(this.shade, zC, xC >> 16, xB >> 16);
                xB += slopeBa;
                xC += slopeAc;
                zC += this.sizeX;
            }
        }
    }

    public boolean fitsDimension(int sizeX, int sizeY) {
        return this.shade.length >= sizeY * sizeX;
    }

    public void setDimension(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.sizeX = endX - startX;
        this.sizeY = endY - startY;
    }
    
    /*FIXME: 876 below*/
	public void markShadedTriangle876(int i, int i24, int i25, int i26, int i27, int i28) {
		int i29 = 0;
		if (i24 != i)
			i29 = (i27 - i26 << 16) / (i24 - i);
		int i30 = 0;
		if (i25 != i24)
			i30 = (i28 - i27 << 16) / (i25 - i24);
		int i31 = 0;
		if (i25 != i)
			i31 = (i26 - i28 << 16) / (i - i25);
		if (i <= i24 && i <= i25) {
			if (i24 < i25) {
				i28 = i26 <<= 16;
				if (i < 0) {
					i28 -= i31 * i;
					i26 -= i29 * i;
					i = 0;
				}
				i27 <<= 16;
				if (i24 < 0) {
					i27 -= i30 * i24;
					i24 = 0;
				}
				if (i != i24 && i31 < i29 || i == i24 && i31 > i30) {
					i25 -= i24;
					i24 -= i;
					i *= this.sizeX;
					while (--i24 >= 0) {
						markShadedScanline876(this.shade, i, 0, i28 >> 16, i26 >> 16);
						i28 += i31;
						i26 += i29;
						i += this.sizeX;
					}
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i, 0, i28 >> 16, i27 >> 16);
						i28 += i31;
						i27 += i30;
						i += this.sizeX;
					}
				} else {
					i25 -= i24;
					i24 -= i;
					i *= this.sizeX;
					while (--i24 >= 0) {
						markShadedScanline876(this.shade, i, 0, i26 >> 16, i28 >> 16);
						i28 += i31;
						i26 += i29;
						i += this.sizeX;
					}
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i, 0, i27 >> 16, i28 >> 16);
						i28 += i31;
						i27 += i30;
						i += this.sizeX;
					}
				}
			} else {
				i27 = i26 <<= 16;
				if (i < 0) {
					i27 -= i31 * i;
					i26 -= i29 * i;
					i = 0;
				}
				i28 <<= 16;
				if (i25 < 0) {
					i28 -= i30 * i25;
					i25 = 0;
				}
				if (i != i25 && i31 < i29 || i == i25 && i30 > i29) {
					i24 -= i25;
					i25 -= i;
					i *= this.sizeX;
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i, 0, i27 >> 16, i26 >> 16);
						i27 += i31;
						i26 += i29;
						i += this.sizeX;
					}
					while (--i24 >= 0) {
						markShadedScanline876(this.shade, i, 0, i28 >> 16, i26 >> 16);
						i28 += i30;
						i26 += i29;
						i += this.sizeX;
					}
				} else {
					i24 -= i25;
					i25 -= i;
					i *= this.sizeX;
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i, 0, i26 >> 16, i27 >> 16);
						i27 += i31;
						i26 += i29;
						i += this.sizeX;
					}
					while (--i24 >= 0) {
						markShadedScanline876(this.shade, i, 0, i26 >> 16, i28 >> 16);
						i28 += i30;
						i26 += i29;
						i += this.sizeX;
					}
				}
			}
		} else if (i24 <= i25) {
			if (i25 < i) {
				i26 = i27 <<= 16;
				if (i24 < 0) {
					i26 -= i29 * i24;
					i27 -= i30 * i24;
					i24 = 0;
				}
				i28 <<= 16;
				if (i25 < 0) {
					i28 -= i31 * i25;
					i25 = 0;
				}
				if (i24 != i25 && i29 < i30 || i24 == i25 && i29 > i31) {
					i -= i25;
					i25 -= i24;
					i24 *= this.sizeX;
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i24, 0, i26 >> 16, i27 >> 16);
						i26 += i29;
						i27 += i30;
						i24 += this.sizeX;
					}
					while (--i >= 0) {
						markShadedScanline876(this.shade, i24, 0, i26 >> 16, i28 >> 16);
						i26 += i29;
						i28 += i31;
						i24 += this.sizeX;
					}
				} else {
					i -= i25;
					i25 -= i24;
					i24 *= this.sizeX;
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i24, 0, i27 >> 16, i26 >> 16);
						i26 += i29;
						i27 += i30;
						i24 += this.sizeX;
					}
					while (--i >= 0) {
						markShadedScanline876(this.shade, i24, 0, i28 >> 16, i26 >> 16);
						i26 += i29;
						i28 += i31;
						i24 += this.sizeX;
					}
				}
			} else {
				i28 = i27 <<= 16;
				if (i24 < 0) {
					i28 -= i29 * i24;
					i27 -= i30 * i24;
					i24 = 0;
				}
				i26 <<= 16;
				if (i < 0) {
					i26 -= i31 * i;
					i = 0;
				}
				if (i29 < i30) {
					i25 -= i;
					i -= i24;
					i24 *= this.sizeX;
					while (--i >= 0) {
						markShadedScanline876(this.shade, i24, 0, i28 >> 16, i27 >> 16);
						i28 += i29;
						i27 += i30;
						i24 += this.sizeX;
					}
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i24, 0, i26 >> 16, i27 >> 16);
						i26 += i31;
						i27 += i30;
						i24 += this.sizeX;
					}
				} else {
					i25 -= i;
					i -= i24;
					i24 *= this.sizeX;
					while (--i >= 0) {
						markShadedScanline876(this.shade, i24, 0, i27 >> 16, i28 >> 16);
						i28 += i29;
						i27 += i30;
						i24 += this.sizeX;
					}
					while (--i25 >= 0) {
						markShadedScanline876(this.shade, i24, 0, i27 >> 16, i26 >> 16);
						i26 += i31;
						i27 += i30;
						i24 += this.sizeX;
					}
				}
			}
		} else if (i < i24) {
			i27 = i28 <<= 16;
			if (i25 < 0) {
				i27 -= i30 * i25;
				i28 -= i31 * i25;
				i25 = 0;
			}
			i26 <<= 16;
			if (i < 0) {
				i26 -= i29 * i;
				i = 0;
			}
			if (i30 < i31) {
				i24 -= i;
				i -= i25;
				i25 *= this.sizeX;
				while (--i >= 0) {
					markShadedScanline876(this.shade, i25, 0, i27 >> 16, i28 >> 16);
					i27 += i30;
					i28 += i31;
					i25 += this.sizeX;
				}
				while (--i24 >= 0) {
					markShadedScanline876(this.shade, i25, 0, i27 >> 16, i26 >> 16);
					i27 += i30;
					i26 += i29;
					i25 += this.sizeX;
				}
			} else {
				i24 -= i;
				i -= i25;
				i25 *= this.sizeX;
				while (--i >= 0) {
					markShadedScanline876(this.shade, i25, 0, i28 >> 16, i27 >> 16);
					i27 += i30;
					i28 += i31;
					i25 += this.sizeX;
				}
				while (--i24 >= 0) {
					markShadedScanline876(this.shade, i25, 0, i26 >> 16, i27 >> 16);
					i27 += i30;
					i26 += i29;
					i25 += this.sizeX;
				}
			}
		} else {
			i26 = i28 <<= 16;
			if (i25 < 0) {
				i26 -= i30 * i25;
				i28 -= i31 * i25;
				i25 = 0;
			}
			i27 <<= 16;
			if (i24 < 0) {
				i27 -= i29 * i24;
				i24 = 0;
			}
			if (i30 < i31) {
				i -= i24;
				i24 -= i25;
				i25 *= this.sizeX;
				while (--i24 >= 0) {
					markShadedScanline876(this.shade, i25, 0, i26 >> 16, i28 >> 16);
					i26 += i30;
					i28 += i31;
					i25 += this.sizeX;
				}
				while (--i >= 0) {
					markShadedScanline876(this.shade, i25, 0, i27 >> 16, i28 >> 16);
					i27 += i29;
					i28 += i31;
					i25 += this.sizeX;
				}
			} else {
				i -= i24;
				i24 -= i25;
				i25 *= this.sizeX;
				while (--i24 >= 0) {
					markShadedScanline876(this.shade, i25, 0, i28 >> 16, i26 >> 16);
					i26 += i30;
					i28 += i31;
					i25 += this.sizeX;
				}
				while (--i >= 0) {
					markShadedScanline876(this.shade, i25, 0, i28 >> 16, i27 >> 16);
					i27 += i29;
					i28 += i31;
					i25 += this.sizeX;
				}
			}
		}
	}
	
	static final void markShadedScanline876(byte[] is, int i, int i14, int i15, int i16) {
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
