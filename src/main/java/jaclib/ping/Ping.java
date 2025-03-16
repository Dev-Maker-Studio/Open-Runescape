/* Ping - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.ping;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

public class Ping {
	private Ping() throws Throwable {
		throw new Error();
	}

	public static native boolean init();

	public static int ping(byte rawIp0, byte rawIp1, byte rawIp2, byte rawIp3, long timeout) throws Throwable {
//		//Original trash native failing stuff
//		int response = ping0(rawIp0, rawIp1, rawIp2, rawIp3, timeout);
//		if (response < 0)
//			throw new Exception(String.valueOf(response));
//		return response;

		byte[] ipAddress = new byte[] { rawIp0, rawIp1, rawIp2, rawIp3 };
		InetAddress address = InetAddress.getByAddress(ipAddress);
		try {
			long startTime = System.currentTimeMillis();
			if (address.isReachable((int) timeout)) {
				long endTime = System.currentTimeMillis();
				return (int) (endTime - startTime);
			}
		} catch (IOException ignored) { }

		try {
			String ip = String.format("%d.%d.%d.%d", rawIp0 & 0xFF, rawIp1 & 0xFF, rawIp2 & 0xFF, rawIp3 & 0xFF);
			String pingCmd;
			String timeoutParam;

			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				pingCmd = "ping";
				timeoutParam = "-w";
			} else {
				pingCmd = "ping";
				timeoutParam = "-W";
			}

			long startTime = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec(new String[] { pingCmd, timeoutParam, String.valueOf(timeout / 1000 + 1), "-c", "1", ip });

			int exitCode = process.waitFor();
			long endTime = System.currentTimeMillis();

			if (exitCode == 0)
				return (int) (endTime - startTime);
			else
				return -1;
		} catch (IOException | InterruptedException e) {
			throw new Exception("Error during fallback ping: " + e.getMessage());
		}
	}

	private static native int ping0(byte rawIp0, byte rawIp1, byte rawIp2, byte rawIp3, long timeout);

	public static native void quit();
}
