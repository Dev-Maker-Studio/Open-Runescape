package com.jagex.game.runetek5.client

class Game(
    @JvmField val name: String,
    @JvmField val formattedName: String,
    @JvmField val id: Int
) {

    companion object {
        @JvmField val RUNESCAPE: Game = Game("darkan", "Darkan", 0)
        @JvmField val STELLAR_DAWN: Game = Game("stellardawn", "Stellar Dawn", 1)
        @JvmField val GAME3: Game = Game("game3", "Game 3", 2)
        @JvmField val GAME4: Game = Game("game4", "Game 4", 3)
        @JvmField val GAME5: Game = Game("game5", "Game 5", 4)

        @JvmStatic
        fun values(): Array<Game> {
            return arrayOf(GAME3, GAME4, STELLAR_DAWN, GAME5, RUNESCAPE)
        }

        @JvmStatic
        fun forid(id: Int): Game? {
            val values = values()
            for (index in values.indices) {
                val game = values[index]
                if (id == game.id) {
                    return game
                }
            }
            return null
        }
    }
}
