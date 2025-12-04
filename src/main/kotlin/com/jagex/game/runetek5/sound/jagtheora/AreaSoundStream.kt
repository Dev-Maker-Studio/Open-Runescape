package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.staticmethods.StaticMethods

abstract class AreaSoundStream : PcmStream() {

    @JvmField var start: Int = 0
    @JvmField var end: Int = 0
    @JvmField var enableLoop: Boolean = false
    @JvmField var sampleRate: Int = 0
    @JvmField var anInt9848: Int = 0
    @JvmField var anInt9845: Int = 0
    @JvmField var anInt9850: Int = 0
    @JvmField var anInt9858: Int = 0
    @JvmField var anInt9854: Int = 0
    @JvmField var volume: Int = 0
    @JvmField var anInt9853: Int = 0
    @JvmField var anInt9847: Int = 0
    @JvmField var anInt9849: Int = 0
    @JvmField var anInt9852: Int = 0
    @JvmField var anInt9851: Int = 0

    @Synchronized
    fun method15308(i_1: Int) {
        if (this.sampleRate < 0) {
            this.sampleRate = -i_1
        } else {
            this.sampleRate = i_1
        }
    }

    @Synchronized
    fun method15311(): Boolean {
        return this.anInt9858 != 0
    }

    @Synchronized
    @JvmName("setVolumeWithShift")
    fun setVolume(volume: Int) {
        method15364(volume shl 6, method15316())
    }

    @Synchronized
    fun method15313() {
        this.sampleRate = (this.sampleRate xor (this.sampleRate shr 31)) + (this.sampleRate ushr 31)
        this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
    }

    @Synchronized
    fun method15315(): Int {
        return if (this.volume == Int.MIN_VALUE) 0 else this.volume
    }

    @Synchronized
    fun method15316(): Int {
        return if (this.anInt9847 < 0) -1 else this.anInt9847
    }

    @Synchronized
    fun method15317(i_1: Int) {
        var i_11 = i_1
        val i_3 = ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
        if (i_11 < -1) {
            i_11 = -1
        }

        if (i_11 > i_3) {
            i_11 = i_3
        }

        this.anInt9853 = i_11
    }

    @Synchronized
    fun method15319(): Boolean {
        return this.anInt9853 < 0 || this.anInt9853 >= ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
    }

    @Synchronized
    fun method15320(i_1: Int, i_2: Int) {
        method15321(i_1, i_2, method15316())
    }

    @Synchronized
    fun method15321(i_1: Int, i_2: Int, i_3: Int) {
        var i_11 = i_1
        if (i_11 == 0) {
            method15364(i_2, i_3)
        } else {
            val i_5 = StaticMethods.method488(i_2, i_3)
            val i_6 = StaticMethods.method15390(i_2, i_3)
            if (i_5 == this.anInt9845 && i_6 == this.anInt9850) {
                this.anInt9858 = 0
            } else {
                var i_7 = i_2 - this.anInt9848
                if (this.anInt9848 - i_2 > i_7) {
                    i_7 = this.anInt9848 - i_2
                }

                if (i_5 - this.anInt9845 > i_7) {
                    i_7 = i_5 - this.anInt9845
                }

                if (this.anInt9845 - i_5 > i_7) {
                    i_7 = this.anInt9845 - i_5
                }

                if (i_6 - this.anInt9850 > i_7) {
                    i_7 = i_6 - this.anInt9850
                }

                if (this.anInt9850 - i_6 > i_7) {
                    i_7 = this.anInt9850 - i_6
                }

                if (i_11 > i_7) {
                    i_11 = i_7
                }

                this.anInt9858 = i_11
                this.volume = i_2
                this.anInt9847 = i_3
                this.anInt9849 = (i_2 - this.anInt9848) / i_11
                this.anInt9852 = (i_5 - this.anInt9845) / i_11
                this.anInt9851 = (i_6 - this.anInt9850) / i_11
            }
        }
    }

    @Synchronized
    fun method15322(i_1: Int) {
        var i_11 = i_1
        if (i_11 == 0) {
            method15326()
            unlink()
        } else if (this.anInt9845 == 0 && this.anInt9850 == 0) {
            this.anInt9858 = 0
            this.volume = 0
            this.anInt9848 = 0
            unlink()
        } else {
            var i_3 = -this.anInt9848
            if (this.anInt9848 > i_3) {
                i_3 = this.anInt9848
            }

            if (-this.anInt9845 > i_3) {
                i_3 = -this.anInt9845
            }

            if (this.anInt9845 > i_3) {
                i_3 = this.anInt9845
            }

            if (-this.anInt9850 > i_3) {
                i_3 = -this.anInt9850
            }

            if (this.anInt9850 > i_3) {
                i_3 = this.anInt9850
            }

            if (i_11 > i_3) {
                i_11 = i_3
            }

            this.anInt9858 = i_11
            this.volume = Int.MIN_VALUE
            this.anInt9849 = -this.anInt9848 / i_11
            this.anInt9852 = -this.anInt9845 / i_11
            this.anInt9851 = -this.anInt9850 / i_11
        }
    }

    @Synchronized
    fun method15323(): Int {
        return if (this.sampleRate < 0) -this.sampleRate else this.sampleRate
    }

    override fun method12228(): Int {
        return if (this.volume == 0 && this.anInt9858 == 0) 0 else 1
    }

    fun method15324(): Boolean {
        var i_2 = this.volume
        val i_3: Int
        val i_4: Int
        if (i_2 == Int.MIN_VALUE) {
            i_4 = 0
            i_3 = 0
            i_2 = 0
        } else {
            i_3 = StaticMethods.method488(i_2, this.anInt9847)
            i_4 = StaticMethods.method15390(i_2, this.anInt9847)
        }

        if (i_2 == this.anInt9848 && i_3 == this.anInt9845 && i_4 == this.anInt9850) {
            if (this.volume == Int.MIN_VALUE) {
                this.volume = 0
                this.anInt9850 = 0
                this.anInt9845 = 0
                this.anInt9848 = 0
                unlink()
                return true
            } else {
                method15332()
                return false
            }
        } else {
            if (this.anInt9848 < i_2) {
                this.anInt9849 = 1
                this.anInt9858 = i_2 - this.anInt9848
            } else if (this.anInt9848 > i_2) {
                this.anInt9849 = -1
                this.anInt9858 = this.anInt9848 - i_2
            } else {
                this.anInt9849 = 0
            }

            if (this.anInt9845 < i_3) {
                this.anInt9852 = 1
                if (this.anInt9858 == 0 || this.anInt9858 > i_3 - this.anInt9845) {
                    this.anInt9858 = i_3 - this.anInt9845
                }
            } else if (this.anInt9845 > i_3) {
                this.anInt9852 = -1
                if (this.anInt9858 == 0 || this.anInt9858 > this.anInt9845 - i_3) {
                    this.anInt9858 = this.anInt9845 - i_3
                }
            } else {
                this.anInt9852 = 0
            }

            if (this.anInt9850 < i_4) {
                this.anInt9851 = 1
                if (this.anInt9858 == 0 || this.anInt9858 > i_4 - this.anInt9850) {
                    this.anInt9858 = i_4 - this.anInt9850
                }
            } else if (this.anInt9850 > i_4) {
                this.anInt9851 = -1
                if (this.anInt9858 == 0 || this.anInt9858 > this.anInt9850 - i_4) {
                    this.anInt9858 = this.anInt9850 - i_4
                }
            } else {
                this.anInt9851 = 0
            }

            return false
        }
    }

    @Synchronized
    override fun skip(i_1: Int) {
        var i_11 = i_1
        if (this.anInt9858 > 0) {
            if (i_11 >= this.anInt9858) {
                if (this.volume == Int.MIN_VALUE) {
                    this.volume = 0
                    this.anInt9850 = 0
                    this.anInt9845 = 0
                    this.anInt9848 = 0
                    unlink()
                    i_11 = this.anInt9858
                }

                this.anInt9858 = 0
                method15332()
            } else {
                this.anInt9848 += i_11 * this.anInt9849
                this.anInt9845 += this.anInt9852 * i_11
                this.anInt9850 += this.anInt9851 * i_11
                this.anInt9858 -= i_11
            }
        }

        val i_2 = this.start shl 8
        val i_3 = this.end shl 8
        val i_4 = ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
        val i_5 = i_3 - i_2
        if (i_5 <= 0) {
            this.anInt9854 = 0
        }

        if (this.anInt9853 < 0) {
            if (this.sampleRate <= 0) {
                method15354()
                unlink()
                return
            }

            this.anInt9853 = 0
        }

        if (this.anInt9853 >= i_4) {
            if (this.sampleRate >= 0) {
                method15354()
                unlink()
                return
            }

            this.anInt9853 = i_4 - 1
        }

        this.anInt9853 += this.sampleRate * i_11
        if (this.anInt9854 < 0) {
            if (this.enableLoop) {
                if (this.sampleRate < 0) {
                    if (this.anInt9853 >= i_2) {
                        return
                    }

                    this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                }

                while (this.anInt9853 >= i_3) {
                    this.anInt9853 = i_3 + i_3 - 1 - this.anInt9853
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                    if (this.anInt9853 >= i_2) {
                        break
                    }

                    this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                }
            } else if (this.sampleRate < 0) {
                if (this.anInt9853 < i_2) {
                    this.anInt9853 = i_3 - 1 - (i_3 - 1 - this.anInt9853) % i_5
                }
            } else if (this.anInt9853 >= i_3) {
                this.anInt9853 = i_2 + (this.anInt9853 - i_2) % i_5
            }
        } else {
            if (this.anInt9854 > 0) {
                if (this.enableLoop) {
                    label123@ {
                        if (this.sampleRate < 0) {
                            if (this.anInt9853 >= i_2) {
                                return@label123
                            }

                            this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                            if (--this.anInt9854 == 0) {
                                return@label123
                            }
                        }

                        do {
                            if (this.anInt9853 < i_3) {
                                return@label123
                            }

                            this.anInt9853 = i_3 + i_3 - 1 - this.anInt9853
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                            if (--this.anInt9854 == 0) {
                                break
                            }

                            if (this.anInt9853 >= i_2) {
                                return@label123
                            }

                            this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                        } while (--this.anInt9854 != 0)
                    }
                } else {
                    var i_6: Int
                    if (this.sampleRate < 0) {
                        if (this.anInt9853 >= i_2) {
                            return
                        }

                        i_6 = (i_3 - 1 - this.anInt9853) / i_5
                        if (i_6 < this.anInt9854) {
                            this.anInt9853 += i_5 * i_6
                            this.anInt9854 -= i_6
                            return
                        }

                        this.anInt9853 += this.anInt9854 * i_5
                    } else {
                        if (this.anInt9853 < i_3) {
                            return
                        }

                        i_6 = (this.anInt9853 - i_2) / i_5
                        if (i_6 < this.anInt9854) {
                            this.anInt9853 -= i_5 * i_6
                            this.anInt9854 -= i_6
                            return
                        }

                        this.anInt9853 -= i_5 * this.anInt9854
                    }
                    this.anInt9854 = 0
                }
            }

            if (this.sampleRate < 0) {
                if (this.anInt9853 < 0) {
                    this.anInt9853 = -1
                    method15354()
                    unlink()
                }
            } else if (this.anInt9853 >= i_4) {
                this.anInt9853 = i_4
                method15354()
                unlink()
            }
        }
    }

    override fun getFirstStream(): PcmStream? {
        return null
    }

    @Synchronized
    fun setNumLoops(i_1: Int) {
        this.anInt9854 = i_1
    }

    @Synchronized
    fun method15326() {
        method15364(0, method15316())
    }

    fun method15332() {
        this.anInt9848 = this.volume
        this.anInt9845 = StaticMethods.method488(this.volume, this.anInt9847)
        this.anInt9850 = StaticMethods.method15390(this.volume, this.anInt9847)
    }

    override fun getPriority(): Int {
        var i_1 = this.anInt9848 * 3 shr 6
        i_1 = (i_1 xor (i_1 shr 31)) + (i_1 ushr 31)
        if (this.anInt9854 == 0) {
            i_1 -= i_1 * this.anInt9853 / (((super.buffer as AbstractAudioFile).getTotalSamples() shl 8))
        } else if (this.anInt9854 >= 0) {
            i_1 -= i_1 * this.start / ((super.buffer as AbstractAudioFile).getTotalSamples())
        }

        return Math.min(255, i_1)
    }

    @Synchronized
    fun method15340(i_1: Int) {
        method15364(method15315(), i_1)
    }

    override abstract fun fill(var1: IntArray, var2: Int, var3: Int)

    override fun getNextStream(): PcmStream? {
        return null
    }

    fun method15354() {
        if (this.anInt9858 != 0) {
            if (this.volume == Int.MIN_VALUE) {
                this.volume = 0
            }

            this.anInt9858 = 0
            method15332()
        }
    }

    @Synchronized
    fun method15364(i_1: Int, i_2: Int) {
        this.volume = i_1
        this.anInt9847 = i_2
        this.anInt9858 = 0
        method15332()
    }
}
