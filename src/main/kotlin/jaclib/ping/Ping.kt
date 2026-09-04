/* Ping - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.ping

import java.io.IOException
import java.net.InetAddress

/**
 * Ping utility with native implementation fallback to pure Java.
 */
class Ping {
    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        external fun init(): Boolean

        @JvmStatic
        @Throws(Throwable::class)
        fun ping(rawIp0: Byte, rawIp1: Byte, rawIp2: Byte, rawIp3: Byte, timeout: Long): Int {
            // Original native implementation disabled in favor of Java fallback
            val ipAddress = byteArrayOf(rawIp0, rawIp1, rawIp2, rawIp3)
            val address = InetAddress.getByAddress(ipAddress)
            try {
                val startTime = System.currentTimeMillis()
                if (address.isReachable(timeout.toInt())) {
                    val endTime = System.currentTimeMillis()
                    return (endTime - startTime).toInt()
                }
            } catch (ignored: IOException) {
            }

            try {
                val ip = String.format(
                    "%d.%d.%d.%d",
                    rawIp0.toInt() and 0xFF,
                    rawIp1.toInt() and 0xFF,
                    rawIp2.toInt() and 0xFF,
                    rawIp3.toInt() and 0xFF
                )
                val pingCmd: String
                val timeoutParam: String

                if (System.getProperty("os.name").lowercase().contains("win")) {
                    pingCmd = "ping"
                    timeoutParam = "-w"
                } else {
                    pingCmd = "ping"
                    timeoutParam = "-W"
                }

                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        pingCmd,
                        timeoutParam,
                        (timeout / 1000 + 1).toString(),
                        "-c",
                        "1",
                        ip
                    )
                )

                val exitCode = process.waitFor()
                val endTime = System.currentTimeMillis()

                return if (exitCode == 0)
                    (endTime - startTime).toInt()
                else
                    -1
            } catch (e: IOException) {
                throw Exception("Error during fallback ping: " + e.message)
            } catch (e: InterruptedException) {
                throw Exception("Error during fallback ping: " + e.message)
            }
        }

        @JvmStatic
        private external fun ping0(rawIp0: Byte, rawIp1: Byte, rawIp2: Byte, rawIp3: Byte, timeout: Long): Int

        @JvmStatic
        external fun quit()
    }
}
