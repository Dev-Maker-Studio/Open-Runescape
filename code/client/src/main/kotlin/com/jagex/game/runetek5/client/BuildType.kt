package com.jagex.game.runetek5.client

enum class BuildType(private val nameValue: String, @JvmField val id: Int) {
    LIVE("LIVE", 0),
    BUILD_LIVE("BUILDLIVE", 3),
    RC("RC", 1),
    WIP("WIP", 2),
    INT_BETA("INTBETA", 4);

    fun getName(): String = nameValue

    companion object {
        @JvmStatic
        fun parse(id: Int): BuildType? {
            val allTypes = getAll()
            for (buildType in allTypes) {
                if (id == buildType.id) {
                    return buildType
                }
            }
            return null
        }

        @JvmStatic
        fun getAll(): Array<BuildType> {
            return arrayOf(BUILD_LIVE, WIP, INT_BETA, LIVE, RC)
        }
    }
}
