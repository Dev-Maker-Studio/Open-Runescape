package com.jagex.game.runetek5.config.staticelements

class StaticElements(count: Int) {
    @JvmField
    var elementCount: Int = count

    @JvmField
    val bitpackedPlacement: IntArray = IntArray(elementCount)

    @JvmField
    val mapCategoryIds: IntArray = IntArray(elementCount)

    companion object {
        @JvmField
        var CS2_QUERY_RESULT_IDX: Int = 0
    }
}
