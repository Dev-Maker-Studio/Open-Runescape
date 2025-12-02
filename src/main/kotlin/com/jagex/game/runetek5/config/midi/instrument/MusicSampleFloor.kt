package com.jagex.game.runetek5.config.midi.instrument

import com.jagex.game.runetek5.utils.BufferUtils
import kotlin.math.min

/**
 *
 * @author Brainpower 23/03/2023
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 *
 */
class MusicSampleFloor {

    var partitionClassList: IntArray
    var classDimensions: IntArray
    var classSubclasses: IntArray
    var classMasterbooks: IntArray
    var subclassBooks: Array<IntArray>
    var floor1Multiplier: Int = 0
    var xList: IntArray

    init {
        val floorTypes = MusicSample.getBits(16)
        if (floorTypes != 1) {
            throw RuntimeException()
        }
        val partitions = MusicSample.getBits(5)
        var maxClass = 0
        partitionClassList = IntArray(partitions)

        for (j in 0 until partitions) {
            partitionClassList[j] = MusicSample.getBits(4)
            if (partitionClassList[j] >= maxClass) {
                maxClass = partitionClassList[j] + 1
            }
        }

        classDimensions = IntArray(maxClass)
        classSubclasses = IntArray(maxClass)
        classMasterbooks = IntArray(maxClass)
        subclassBooks = Array(maxClass) { IntArray(0) }

        for (j in 0 until maxClass) {
            classDimensions[j] = MusicSample.getBits(3) + 1
            var subclass = MusicSample.getBits(2)
            classSubclasses[j] = subclass
            if (subclass != 0) {
                classMasterbooks[j] = MusicSample.getBits(8)
            }

            subclass = 1 shl subclass
            subclassBooks[j] = IntArray(subclass)
            for (k in 0 until subclass) {
                subclassBooks[j][k] = MusicSample.getBits(8) - 1
            }
        }

        floor1Multiplier = MusicSample.getBits(2) + 1
        val rangebits = MusicSample.getBits(4)

        var values = 2
        for (j in 0 until partitions) {
            values += classDimensions[partitionClassList[j]]
        }

        xList = IntArray(values)
        xList[0] = 0
        xList[1] = 1 shl rangebits

        var size = 2
        for (j in 0 until partitions) {
            val c = partitionClassList[j]
            for (k in 0 until classDimensions[c]) {
                xList[size++] = MusicSample.getBits(rangebits)
            }
        }

        if (finalX == null || finalX!!.size < size) {
            finalX = IntArray(size)
            finalY = IntArray(size)
            step2Flag = BooleanArray(size)
        }
    }

    fun predictPoint(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Int {
        val i_6 = i_4 - i_2
        val i_7 = i_3 - i_1
        val i_8 = if (i_6 < 0) -i_6 else i_6
        val i_9 = (i_5 - i_1) * i_8
        val i_10 = i_9 / i_7
        return if (i_6 < 0) i_2 - i_10 else i_10 + i_2
    }

    fun drawLine(lowX: Int, highX: Int, lowY: Int, highY: Int, output: FloatArray, i_6: Int) {
        var x1 = lowY
        val deltaX = highY - highX
        val adx = x1 - lowX
        var i_9 = if (deltaX < 0) -deltaX else deltaX
        val base = deltaX / adx
        var y = highX
        var err = 0
        val sy = if (deltaX < 0) base - 1 else base + 1
        i_9 -= (if (base < 0) -base else base) * adx
        output[lowX] *= INVERSE_DB_TABLE[highX]
        if (x1 > i_6) {
            x1 = i_6
        }

        for (x in lowX + 1 until x1) {
            err += i_9
            if (err >= adx) {
                err -= adx
                y += sy
            } else {
                y += base
            }

            output[x] *= INVERSE_DB_TABLE[y]
        }
    }

    fun decodeFloor(): Boolean {
        if (MusicSample.getBit() == 0) {
            return false
        }
        val length = xList.size
        System.arraycopy(xList, 0, finalX, 0, length)

        val range = RANGE_LIST[floor1Multiplier - 1]
        val bits = BufferUtils.ilog(range - 1)
        finalY!![0] = MusicSample.getBits(bits)
        finalY!![1] = MusicSample.getBits(bits)
        var offset = 2

        for (i in partitionClassList.indices) {
            val pclass = partitionClassList[i]
            val cdim = classDimensions[pclass]
            val cbits = classSubclasses[pclass]
            val csub = (1 shl cbits) - 1
            var cval = 0
            if (cbits > 0) {
                cval = MusicSample.codebooks[classMasterbooks[pclass]].decode()
            }

            for (k in 0 until cdim) {
                val book = subclassBooks[pclass][cval and csub]
                cval = cval ushr cbits
                finalY!![offset++] = if (book >= 0) MusicSample.codebooks[book].decode() else 0
            }
        }

        return true
    }

    fun computeFloor(target: FloatArray, b2: Int) {
        val values = xList.size
        val range = RANGE_LIST[floor1Multiplier - 1]
        step2Flag!![1] = true
        step2Flag!![0] = true

        for (j in 2 until values) {
            val low = lowNeighbor(finalX!!, j)
            val high = highNeighbor(finalX!!, j)
            val pred = predictPoint(finalX!![low], finalY!![low], finalX!![high], finalY!![high], finalX!![j])
            val val1 = finalY!![j]
            val highroom = range - pred
            val room = min(highroom, pred) shl 1
            if (val1 != 0) {
                step2Flag!![high] = true
                step2Flag!![low] = true
                step2Flag!![j] = true
                if (val1 >= room) {
                    finalY!![j] = if (highroom > pred) pred + (val1 - pred) else highroom + (pred - val1) - 1
                } else {
                    finalY!![j] = if (val1 and 0x1 != 0) pred - (val1 + 1) / 2 else val1 / 2 + pred
                }
            } else {
                step2Flag!![j] = false
                finalY!![j] = pred
            }
        }

        method6035(0, values - 1)
        var lx = 0
        var ly = finalY!![0] * floor1Multiplier

        for (q in 1 until values) {
            if (step2Flag!![q]) {
                val hx = finalX!![q]
                val hy = finalY!![q] * floor1Multiplier
                drawLine(lx, ly, hx, hy, target, b2)
                if (hx >= b2) {
                    return
                }

                lx = hx
                ly = hy
            }
        }

        for (j in lx until b2) {
            target[j] *= INVERSE_DB_TABLE[ly]
        }
    }

    fun method6035(i_1: Int, i_2: Int) {
        if (i_1 < i_2) {
            var i_3 = i_1
            val i_4 = finalX!![i_1]
            val i_5 = finalY!![i_1]
            val bool6 = step2Flag!![i_1]

            for (i_7 in i_1 + 1..i_2) {
                val i_8 = finalX!![i_7]
                if (i_8 < i_4) {
                    finalX!![i_3] = i_8
                    finalY!![i_3] = finalY!![i_7]
                    step2Flag!![i_3] = step2Flag!![i_7]
                    ++i_3
                    finalX!![i_7] = finalX!![i_3]
                    finalY!![i_7] = finalY!![i_3]
                    step2Flag!![i_7] = step2Flag!![i_3]
                }
            }

            finalX!![i_3] = i_4
            finalY!![i_3] = i_5
            step2Flag!![i_3] = bool6
            method6035(i_1, i_3 - 1)
            method6035(i_3 + 1, i_2)
        }
    }

    companion object {
        val RANGE_LIST = intArrayOf(256, 128, 86, 64)
        val INVERSE_DB_TABLE = floatArrayOf(
            1.0649863E-7f, 1.1341951E-7f, 1.2079015E-7f, 1.2863978E-7f,
            1.369995E-7f, 1.459025E-7f, 1.5538409E-7f, 1.6548181E-7f, 1.7623574E-7f, 1.8768856E-7f, 1.998856E-7f,
            2.128753E-7f, 2.2670913E-7f, 2.4144197E-7f, 2.5713223E-7f, 2.7384212E-7f, 2.9163792E-7f, 3.1059022E-7f,
            3.307741E-7f, 3.5226967E-7f, 3.7516213E-7f, 3.995423E-7f, 4.255068E-7f, 4.5315863E-7f, 4.8260745E-7f,
            5.1397E-7f, 5.4737063E-7f, 5.829419E-7f, 6.208247E-7f, 6.611694E-7f, 7.041359E-7f, 7.4989464E-7f,
            7.98627E-7f, 8.505263E-7f, 9.057983E-7f, 9.646621E-7f, 1.0273513E-6f, 1.0941144E-6f, 1.1652161E-6f,
            1.2409384E-6f, 1.3215816E-6f, 1.4074654E-6f, 1.4989305E-6f, 1.5963394E-6f, 1.7000785E-6f, 1.8105592E-6f,
            1.9282195E-6f, 2.053526E-6f, 2.1869757E-6f, 2.3290977E-6f, 2.4804558E-6f, 2.6416496E-6f, 2.813319E-6f,
            2.9961443E-6f, 3.1908505E-6f, 3.39821E-6f, 3.619045E-6f, 3.8542307E-6f, 4.1047006E-6f, 4.371447E-6f,
            4.6555283E-6f, 4.958071E-6f, 5.280274E-6f, 5.623416E-6f, 5.988857E-6f, 6.3780467E-6f, 6.7925284E-6f,
            7.2339453E-6f, 7.704048E-6f, 8.2047E-6f, 8.737888E-6f, 9.305725E-6f, 9.910464E-6f, 1.0554501E-5f,
            1.1240392E-5f, 1.1970856E-5f, 1.2748789E-5f, 1.3577278E-5f, 1.4459606E-5f, 1.5399271E-5f, 1.6400005E-5f,
            1.7465769E-5f, 1.8600793E-5f, 1.9809577E-5f, 2.1096914E-5f, 2.2467912E-5f, 2.3928002E-5f, 2.5482977E-5f,
            2.7139005E-5f, 2.890265E-5f, 3.078091E-5f, 3.2781227E-5f, 3.4911533E-5f, 3.718028E-5f, 3.9596467E-5f,
            4.2169668E-5f, 4.491009E-5f, 4.7828602E-5f, 5.0936775E-5f, 5.424693E-5f, 5.7772202E-5f, 6.152657E-5f,
            6.552491E-5f, 6.9783084E-5f, 7.4317984E-5f, 7.914758E-5f, 8.429104E-5f, 8.976875E-5f, 9.560242E-5f,
            1.0181521E-4f, 1.0843174E-4f, 1.1547824E-4f, 1.2298267E-4f, 1.3097477E-4f, 1.3948625E-4f, 1.4855085E-4f,
            1.5820454E-4f, 1.6848555E-4f, 1.7943469E-4f, 1.9109536E-4f, 2.0351382E-4f, 2.167393E-4f, 2.3082423E-4f,
            2.4582449E-4f, 2.6179955E-4f, 2.7881275E-4f, 2.9693157E-4f, 3.1622787E-4f, 3.3677815E-4f, 3.5866388E-4f,
            3.8197188E-4f, 4.0679457E-4f, 4.3323037E-4f, 4.613841E-4f, 4.913675E-4f, 5.2329927E-4f, 5.573062E-4f,
            5.935231E-4f, 6.320936E-4f, 6.731706E-4f, 7.16917E-4f, 7.635063E-4f, 8.1312325E-4f, 8.6596457E-4f,
            9.2223985E-4f, 9.821722E-4f, 0.0010459992f, 0.0011139743f, 0.0011863665f, 0.0012634633f, 0.0013455702f,
            0.0014330129f, 0.0015261382f, 0.0016253153f, 0.0017309374f, 0.0018434235f, 0.0019632196f, 0.0020908006f,
            0.0022266726f, 0.0023713743f, 0.0025254795f, 0.0026895993f, 0.0028643848f, 0.0030505287f, 0.003248769f,
            0.0034598925f, 0.0036847359f, 0.0039241905f, 0.0041792067f, 0.004450795f, 0.004740033f, 0.005048067f,
            0.0053761187f, 0.005725489f, 0.0060975635f, 0.0064938175f, 0.0069158226f, 0.0073652514f, 0.007843887f,
            0.008353627f, 0.008896492f, 0.009474637f, 0.010090352f, 0.01074608f, 0.011444421f, 0.012188144f,
            0.012980198f, 0.013823725f, 0.014722068f, 0.015678791f, 0.016697686f, 0.017782796f, 0.018938422f,
            0.020169148f, 0.021479854f, 0.022875736f, 0.02436233f, 0.025945531f, 0.027631618f, 0.029427277f,
            0.031339627f, 0.03337625f, 0.035545226f, 0.037855156f, 0.0403152f, 0.042935107f, 0.045725275f, 0.048696756f,
            0.05186135f, 0.05523159f, 0.05882085f, 0.062643364f, 0.06671428f, 0.07104975f, 0.075666964f, 0.08058423f,
            0.08582105f, 0.09139818f, 0.097337745f, 0.1036633f, 0.11039993f, 0.11757434f, 0.12521498f, 0.13335215f,
            0.14201812f, 0.15124726f, 0.16107617f, 0.1715438f, 0.18269168f, 0.19456401f, 0.20720787f, 0.22067343f,
            0.23501402f, 0.25028655f, 0.26655158f, 0.28387362f, 0.3023213f, 0.32196787f, 0.34289113f, 0.36517414f,
            0.3889052f, 0.41417846f, 0.44109413f, 0.4697589f, 0.50028646f, 0.53279793f, 0.5674221f, 0.6042964f,
            0.64356697f, 0.6853896f, 0.72993004f, 0.777365f, 0.8278826f, 0.88168305f, 0.9389798f, 1.0f
        )

        @JvmField
        var finalX: IntArray? = null
        @JvmField
        var finalY: IntArray? = null
        @JvmField
        var step2Flag: BooleanArray? = null

        @JvmStatic
        fun highNeighbor(ints_0: IntArray, i_1: Int): Int {
            val i_2 = ints_0[i_1]
            var highNeightbor = -1
            var i_4 = Int.MAX_VALUE

            for (i_5 in 0 until i_1) {
                val i_6 = ints_0[i_5]
                if (i_6 > i_2 && i_6 < i_4) {
                    highNeightbor = i_5
                    i_4 = i_6
                }
            }

            return highNeightbor
        }

        @JvmStatic
        fun lowNeighbor(ints_0: IntArray, i_1: Int): Int {
            val i_2 = ints_0[i_1]
            var lowNeightbor = -1
            var i_4 = Int.MIN_VALUE

            for (i_5 in 0 until i_1) {
                val i_6 = ints_0[i_5]
                if (i_6 < i_2 && i_6 > i_4) {
                    lowNeightbor = i_5
                    i_4 = i_6
                }
            }

            return lowNeightbor
        }
    }
}
