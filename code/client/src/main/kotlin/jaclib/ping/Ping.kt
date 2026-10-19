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
                val os = System.getProperty("os.name").lowercase()
                val command = when {
                    os.contains("win") -> arrayOf("ping", "-n", "1", "-w", timeout.toString(), ip)
                    os.contains("mac") -> arrayOf("ping", "-c", "1", "-W", timeout.toString(), ip)
                    else -> arrayOf("ping", "-c", "1", "-W", (timeout / 1000 + 1).toString(), ip)
                }

                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec(command)

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
