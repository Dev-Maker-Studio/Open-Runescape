package com.jagex.game.runetek5.icmp

class IcmpServiceRunnable : Runnable {
    override fun run() {
        try {
            IcmpServiceJaclib.icmpService!!.run()
        } catch (throwable: Throwable) {
        }
        IcmpServiceJaclib.icmpService = null
    }
}
