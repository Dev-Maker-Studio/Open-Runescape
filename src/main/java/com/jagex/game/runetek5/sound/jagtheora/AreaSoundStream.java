package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.staticmethods.StaticMethods;

public abstract class AreaSoundStream extends PcmStream {

    int start;
    int end;
    boolean enableLoop;
    int sampleRate;
    int anInt9848;
    int anInt9845;
    int anInt9850;
    int anInt9858;
    int anInt9854;
    int volume;
    int anInt9853;
    int anInt9847;
    int anInt9849;
    int anInt9852;
    int anInt9851;

    public synchronized void method15308(int i_1) {
        if (this.sampleRate < 0) {
            this.sampleRate = -i_1;
        } else {
            this.sampleRate = i_1;
        }

    }

    public synchronized boolean method15311() {
        return this.anInt9858 != 0;
    }

    public synchronized void setVolume(int volume) {
        method15364(volume << 6, method15316());
    }

    public synchronized void method15313() {
        this.sampleRate = (this.sampleRate ^ this.sampleRate >> 31) + (this.sampleRate >>> 31);
        this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
    }
    public synchronized int method15315() {
        return this.volume == Integer.MIN_VALUE ? 0 : this.volume;
    }

    public synchronized int method15316() {
        return this.anInt9847 < 0 ? -1 : this.anInt9847;
    }

    public synchronized void method15317(int i_1) {
        int i_11 = i_1;
        int i_3 = ((AbstractAudioFile) super.buffer).getTotalSamples() << 8;
        if (i_11 < -1) {
            i_11 = -1;
        }

        if (i_11 > i_3) {
            i_11 = i_3;
        }

        this.anInt9853 = i_11;
    }

    public synchronized boolean method15319() {
        return this.anInt9853 < 0 || this.anInt9853 >= ((AbstractAudioFile) super.buffer).getTotalSamples() << 8;
    }

    public synchronized void method15320(int i_1, int i_2) {
        method15321(i_1, i_2, method15316());
    }

    public synchronized void method15321(int i_1, int i_2, int i_3) {
        int i_11 = i_1;
        if (i_11 == 0) {
            method15364(i_2, i_3);
        } else {
            int i_5 = StaticMethods.method488(i_2, i_3);
            int i_6 = StaticMethods.method15390(i_2, i_3);
            if (i_5 == this.anInt9845 && i_6 == this.anInt9850) {
                this.anInt9858 = 0;
            } else {
                int i_7 = i_2 - this.anInt9848;
                if (this.anInt9848 - i_2 > i_7) {
                    i_7 = this.anInt9848 - i_2;
                }

                if (i_5 - this.anInt9845 > i_7) {
                    i_7 = i_5 - this.anInt9845;
                }

                if (this.anInt9845 - i_5 > i_7) {
                    i_7 = this.anInt9845 - i_5;
                }

                if (i_6 - this.anInt9850 > i_7) {
                    i_7 = i_6 - this.anInt9850;
                }

                if (this.anInt9850 - i_6 > i_7) {
                    i_7 = this.anInt9850 - i_6;
                }

                if (i_11 > i_7) {
                    i_11 = i_7;
                }

                this.anInt9858 = i_11;
                this.volume = i_2;
                this.anInt9847 = i_3;
                this.anInt9849 = (i_2 - this.anInt9848) / i_11;
                this.anInt9852 = (i_5 - this.anInt9845) / i_11;
                this.anInt9851 = (i_6 - this.anInt9850) / i_11;
            }
        }

    }

    public synchronized void method15322(int i_1) {
        int i_11 = i_1;
        if (i_11 == 0) {
            method15326();
            unlink();
        } else if (this.anInt9845 == 0 && this.anInt9850 == 0) {
            this.anInt9858 = 0;
            this.volume = 0;
            this.anInt9848 = 0;
            unlink();
        } else {
            int i_3 = -this.anInt9848;
            if (this.anInt9848 > i_3) {
                i_3 = this.anInt9848;
            }

            if (-this.anInt9845 > i_3) {
                i_3 = -this.anInt9845;
            }

            if (this.anInt9845 > i_3) {
                i_3 = this.anInt9845;
            }

            if (-this.anInt9850 > i_3) {
                i_3 = -this.anInt9850;
            }

            if (this.anInt9850 > i_3) {
                i_3 = this.anInt9850;
            }

            if (i_11 > i_3) {
                i_11 = i_3;
            }

            this.anInt9858 = i_11;
            this.volume = Integer.MIN_VALUE;
            this.anInt9849 = -this.anInt9848 / i_11;
            this.anInt9852 = -this.anInt9845 / i_11;
            this.anInt9851 = -this.anInt9850 / i_11;
        }

    }

    public synchronized int method15323() {
        return this.sampleRate < 0 ? -this.sampleRate : this.sampleRate;
    }

    @Override
    public int method12228() {
        return this.volume == 0 && this.anInt9858 == 0 ? 0 : 1;
    }

    boolean method15324() {
        int i_2 = this.volume;
        int i_3;
        int i_4;
        if (i_2 == Integer.MIN_VALUE) {
            i_4 = 0;
            i_3 = 0;
            i_2 = 0;
        } else {
            i_3 = StaticMethods.method488(i_2, this.anInt9847);
            i_4 = StaticMethods.method15390(i_2, this.anInt9847);
        }

        if (i_2 == this.anInt9848 && i_3 == this.anInt9845 && i_4 == this.anInt9850) {
            if (this.volume == Integer.MIN_VALUE) {
                this.volume = 0;
                this.anInt9850 = 0;
                this.anInt9845 = 0;
                this.anInt9848 = 0;
                unlink();
                return true;
            } else {
                method15332();
                return false;
            }
        } else {
            if (this.anInt9848 < i_2) {
                this.anInt9849 = 1;
                this.anInt9858 = i_2 - this.anInt9848;
            } else if (this.anInt9848 > i_2) {
                this.anInt9849 = -1;
                this.anInt9858 = this.anInt9848 - i_2;
            } else {
                this.anInt9849 = 0;
            }

            if (this.anInt9845 < i_3) {
                this.anInt9852 = 1;
                if (this.anInt9858 == 0 || this.anInt9858 > i_3 - this.anInt9845) {
                    this.anInt9858 = i_3 - this.anInt9845;
                }
            } else if (this.anInt9845 > i_3) {
                this.anInt9852 = -1;
                if (this.anInt9858 == 0 || this.anInt9858 > this.anInt9845 - i_3) {
                    this.anInt9858 = this.anInt9845 - i_3;
                }
            } else {
                this.anInt9852 = 0;
            }

            if (this.anInt9850 < i_4) {
                this.anInt9851 = 1;
                if (this.anInt9858 == 0 || this.anInt9858 > i_4 - this.anInt9850) {
                    this.anInt9858 = i_4 - this.anInt9850;
                }
            } else if (this.anInt9850 > i_4) {
                this.anInt9851 = -1;
                if (this.anInt9858 == 0 || this.anInt9858 > this.anInt9850 - i_4) {
                    this.anInt9858 = this.anInt9850 - i_4;
                }
            } else {
                this.anInt9851 = 0;
            }

            return false;
        }
    }

    @Override
    public synchronized void skip(int i_1) {
        int i_11 = i_1;
        if (this.anInt9858 > 0) {
            if (i_11 >= this.anInt9858) {
                if (this.volume == Integer.MIN_VALUE) {
                    this.volume = 0;
                    this.anInt9850 = 0;
                    this.anInt9845 = 0;
                    this.anInt9848 = 0;
                    unlink();
                    i_11 = this.anInt9858;
                }

                this.anInt9858 = 0;
                method15332();
            } else {
                this.anInt9848 += i_11 * this.anInt9849;
                this.anInt9845 += this.anInt9852 * i_11;
                this.anInt9850 += this.anInt9851 * i_11;
                this.anInt9858 -= i_11;
            }
        }

        int i_2 = this.start << 8;
        int i_3 = this.end << 8;
        int i_4 = ((AbstractAudioFile) super.buffer).getTotalSamples() << 8;
        int i_5 = i_3 - i_2;
        if (i_5 <= 0) {
            this.anInt9854 = 0;
        }

        if (this.anInt9853 < 0) {
            if (this.sampleRate <= 0) {
                method15354();
                unlink();
                return;
            }

            this.anInt9853 = 0;
        }

        if (this.anInt9853 >= i_4) {
            if (this.sampleRate >= 0) {
                method15354();
                unlink();
                return;
            }

            this.anInt9853 = i_4 - 1;
        }

        this.anInt9853 += this.sampleRate * i_11;
        if (this.anInt9854 < 0) {
            if (this.enableLoop) {
                if (this.sampleRate < 0) {
                    if (this.anInt9853 >= i_2) {
                        return;
                    }

                    this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853;
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
                }

                while (this.anInt9853 >= i_3) {
                    this.anInt9853 = i_3 + i_3 - 1 - this.anInt9853;
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
                    if (this.anInt9853 >= i_2) {
                        break;
                    }

                    this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853;
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
                }
            } else if (this.sampleRate < 0) {
                if (this.anInt9853 < i_2) {
                    this.anInt9853 = i_3 - 1 - (i_3 - 1 - this.anInt9853) % i_5;
                }
            } else if (this.anInt9853 >= i_3) {
                this.anInt9853 = i_2 + (this.anInt9853 - i_2) % i_5;
            }
        } else {
            if (this.anInt9854 > 0) {
                if (this.enableLoop) {
                    label123:
                    {
                        if (this.sampleRate < 0) {
                            if (this.anInt9853 >= i_2) {
                                return;
                            }

                            this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853;
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
                            if (--this.anInt9854 == 0) {
                                break label123;
                            }
                        }

                        do {
                            if (this.anInt9853 < i_3) {
                                return;
                            }

                            this.anInt9853 = i_3 + i_3 - 1 - this.anInt9853;
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
                            if (--this.anInt9854 == 0) {
                                break;
                            }

                            if (this.anInt9853 >= i_2) {
                                return;
                            }

                            this.anInt9853 = i_2 + i_2 - 1 - this.anInt9853;
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111;
                        } while (--this.anInt9854 != 0);
                    }
                } else {
                    int i_6;
                    if (this.sampleRate < 0) {
                        if (this.anInt9853 >= i_2) {
                            return;
                        }

                        i_6 = (i_3 - 1 - this.anInt9853) / i_5;
                        if (i_6 < this.anInt9854) {
                            this.anInt9853 += i_5 * i_6;
                            this.anInt9854 -= i_6;
                            return;
                        }

                        this.anInt9853 += this.anInt9854 * i_5;
                    } else {
                        if (this.anInt9853 < i_3) {
                            return;
                        }

                        i_6 = (this.anInt9853 - i_2) / i_5;
                        if (i_6 < this.anInt9854) {
                            this.anInt9853 -= i_5 * i_6;
                            this.anInt9854 -= i_6;
                            return;
                        }

                        this.anInt9853 -= i_5 * this.anInt9854;
                    }
                    this.anInt9854 = 0;
                }
            }

            if (this.sampleRate < 0) {
                if (this.anInt9853 < 0) {
                    this.anInt9853 = -1;
                    method15354();
                    unlink();
                }
            } else if (this.anInt9853 >= i_4) {
                this.anInt9853 = i_4;
                method15354();
                unlink();
            }
        }

    }

    @Override
    public PcmStream getFirstStream() {
        return null;
    }

    public synchronized void setNumLoops(int i_1) {
        this.anInt9854 = i_1;
    }

    public synchronized void method15326() {
        method15364(0, method15316());
    }

    void method15332() {
        this.anInt9848 = this.volume;
        this.anInt9845 = StaticMethods.method488(this.volume, this.anInt9847);
        this.anInt9850 = StaticMethods.method15390(this.volume, this.anInt9847);
    }

    @Override
    public int getPriority() {
        int i_1 = this.anInt9848 * 3 >> 6;
        i_1 = (i_1 ^ i_1 >> 31) + (i_1 >>> 31);
        if (this.anInt9854 == 0) {
            i_1 -= i_1 * this.anInt9853 / (((AbstractAudioFile) super.buffer).getTotalSamples() << 8);
        } else if (this.anInt9854 >= 0) {
            i_1 -= i_1 * this.start / ((AbstractAudioFile) super.buffer).getTotalSamples();
        }

        return Math.min(255, i_1);
    }

    public synchronized void method15340(int i_1) {
        method15364(method15315(), i_1);
    }

    @Override
    public abstract void fill(int[] var1, int var2, int var3);

    @Override
    public PcmStream getNextStream() {
        return null;
    }

    void method15354() {
        if (this.anInt9858 != 0) {
            if (this.volume == Integer.MIN_VALUE) {
                this.volume = 0;
            }

            this.anInt9858 = 0;
            method15332();
        }

    }

    public synchronized void method15364(int i_1, int i_2) {
        this.volume = i_1;
        this.anInt9847 = i_2;
        this.anInt9858 = 0;
        method15332();
    }

}
