package com.jagex.game.runetek5.config

/**
 * Enumeration of cache index references (0-36).
 */
enum class IndexReference(val indexId: Int) {
    INDEX_0(0),
    INDEX_1(1),
    INDEX_2(2),
    INDEX_3(3),
    INDEX_4(4),
    INDEX_5(5),
    INDEX_6(6),
    INDEX_7(7),
    INDEX_8(8),
    INDEX_9(9),
    INDEX_10(10),
    INDEX_11(11),
    INDEX_12(12),
    INDEX_13(13),
    INDEX_14(14),
    INDEX_15(15),
    INDEX_16(16),
    INDEX_17(17),
    INDEX_18(18),
    INDEX_19(19),
    INDEX_20(20),
    INDEX_21(21),
    INDEX_22(22),
    INDEX_23(23),
    INDEX_24(24),
    INDEX_25(25),
    INDEX_26(26),
    INDEX_27(27),
    INDEX_28(28),
    INDEX_29(29),
    INDEX_30(30),
    INDEX_31(31),
    INDEX_32(32),
    INDEX_33(33),
    INDEX_34(34),
    INDEX_35(35),
    INDEX_36(36);

    fun getIndex(): Int {
        return indexId
    }

    companion object {
        @JvmField var maxIndexReference = -1

        /**
         * Calculates and returns the maximum index reference value.
         *
         * @return The maximum index ID plus one
         */
        @JvmStatic
        fun getMaxIndexReference(): Int {
            if (maxIndexReference == -1) {
                val allReferences = values()
                for (reference in allReferences) {
                    if (reference.indexId > maxIndexReference) {
                        maxIndexReference = reference.indexId
                    }
                }
                maxIndexReference++
            }
            return maxIndexReference
        }
    }
}
