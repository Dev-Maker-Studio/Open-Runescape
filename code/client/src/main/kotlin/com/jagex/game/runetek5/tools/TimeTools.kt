package com.jagex.game.runetek5.tools

class TimeTools {
    companion object {
        @JvmStatic
        fun sleepDirect(ms: Long) {
            try {
                Thread.sleep(ms)
            } catch (exception: InterruptedException) {
                exception.printStackTrace()
            }
        }

        @JvmStatic
        fun sleep(ms: Long) {
            if (ms > 0L) {
                if (ms % 10L == 0L) {
                    sleepDirect(ms - 1L)
                    sleepDirect(1L)
                } else {
                    sleepDirect(ms)
                }
            }
        }
    }
}
