package com.rs.game.content.skills.farming

import com.rs.game.model.entity.player.Player
import org.darkan.core.model.Item

enum class StorableItem(val maxAmount: Int, vararg val validIds: Int) {
    RAKE(1, 5341) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_RAKE, if (player.getLeprechaunStorage().containsKey(RAKE)) 1 else 0)
        }
    },
    SEED_DIBBER(1, 5343) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_SEED_DIBBER, if (player.getLeprechaunStorage().containsKey(SEED_DIBBER)) 1 else 0)
        }
    },
    SPADE(1, 952) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_SPADE, if (player.getLeprechaunStorage().containsKey(SPADE)) 1 else 0)
        }
    },
    TROWEL(1, 5325) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_TROWEL, if (player.getLeprechaunStorage().containsKey(TROWEL)) 1 else 0)
        }
    },
    SCARECROW(7, 6059) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_SCARECROW, player.getNumInLeprechaun(SCARECROW))
        }
    },
    BUCKET(255, 1925) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_BUCKET, player.getNumInLeprechaun(BUCKET) % 32)
            player.vars.setVarBit(VB_EXTRA_BUCKETS, player.getNumInLeprechaun(BUCKET) / 32)
        }
    },
    COMPOST(255, 6032) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_COMPOST, player.getNumInLeprechaun(COMPOST))
        }
    },
    SUPERCOMPOST(255, 6034) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_SUPERCOMPOST, player.getNumInLeprechaun(SUPERCOMPOST))
        }
    },
    SECATEURS(1, 5329, 7409) {
        override fun updateVars(player: Player) {
            val item: Item = player.getLeprechaunStorage()[SECATEURS] ?: return
            player.vars.setVarBit(VB_SECATEURS, if (player.getLeprechaunStorage().containsKey(SECATEURS)) 1 else 0)
            player.vars.setVarBit(VB_IS_MAGIC_SECATEURS, if ((item.id == 5329)) 0 else 1)
        }
    },
    WATERING_CAN(1, 5331, 5333, 5334, 5335, 5336, 5337, 5338, 5339, 5340, 18682) {
        override fun updateVars(player: Player) {
            if (!player.getLeprechaunStorage().containsKey(WATERING_CAN)) {
                player.vars.setVarBit(VB_WATERING_CAN, 0)
                return
            }
            var index = 0
            for (i in validIds.indices) if ((player.getLeprechaunStorage()[WATERING_CAN]?.id ?: 0) == validIds[i]) {
                index = i
                break
            }
            player.vars.setVarBit(VB_WATERING_CAN, index + 1)
        }
    },
    PLANT_CURE(255, 6036) {
        override fun updateVars(player: Player) {
            player.vars.setVarBit(VB_PLANT_CURE, player.getNumInLeprechaun(PLANT_CURE))
        }
    };

    abstract fun updateVars(player: Player)

    companion object {
        private const val VB_RAKE = 1435
        private const val VB_SEED_DIBBER = 1436
        private const val VB_SPADE = 1437
        private const val VB_TROWEL = 1440
        private const val VB_SCARECROW = 1778
        private const val VB_BUCKET = 1441
        private const val VB_COMPOST = 1442
        private const val VB_SUPERCOMPOST = 1443
        private const val VB_SECATEURS = 1438
        private const val VB_IS_MAGIC_SECATEURS = 1848
        private const val VB_WATERING_CAN = 1439
        private const val VB_EXTRA_BUCKETS = 10204
        private const val VB_PLANT_CURE = 10205

        private val MAP: MutableMap<Int, StorableItem> = HashMap()

        init {
            for (item in entries) for (id in item.validIds) MAP[id] = item
        }

        fun forId(itemId: Int): StorableItem? {
            return MAP[itemId]
        }
    }
}
