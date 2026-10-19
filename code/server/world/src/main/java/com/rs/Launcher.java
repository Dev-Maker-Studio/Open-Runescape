package com.rs;

import world.gregs.voidps.cache.Cache;
import com.rs.db.WorldDB;
import com.rs.engine.thread.AsyncTaskExecutor;
import com.rs.engine.thread.WorldThread;
import com.rs.game.World;
import com.rs.game.map.ChunkManager;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.JsonFileManager;
import org.darkan.core.Logger;
import com.rs.plugin.PluginManager;
import org.darkan.core.net.prot.handler.PacketHandlers;
import org.darkan.world.AutoUpdate;
import org.darkan.world.server.WorldServer;
import com.rs.utils.MapXTEAs;
import com.rs.utils.json.WorldGson;
import com.rs.utils.Ticks;
import com.rs.utils.WorldPersistentData;
import com.rs.utils.WorldUtil;
import org.darkan.core.map.MapXteas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static org.darkan.core.net.prot.revision.rev727.Rev727CodecKt.register727;

public final class Launcher {

	private static final long WORLD_DRAIN_TIMEOUT_MS = 10000;
	private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean();

    public static void main(String[] args) throws Exception {
		Logger.setupFormat();
		Logger.setLevel(Level.FINE); //FINER for traces
		JsonFileManager.setGSON(WorldGson.create());

		Settings.loadConfig();
		if (!Settings.getConfig().isDebug())
			Logger.setLevel(Level.WARNING);

		long currentTime = System.currentTimeMillis();

		Cache.init(Settings.getConfig().getCachePath());
		MapXTEAs.loadKeys();
		MapXteas.INSTANCE.load("./data/map/xteaKeys.json");

        WorldDB DB = new WorldDB();

		register727();

		AsyncTaskExecutor.initExecutors();
		PluginManager.loadPlugins();
		PluginManager.executeStartupHooks();

		try {
			PacketHandlers.INSTANCE.loadHandlersFromPackage("org.darkan.world.server.packet");
			WorldServer.INSTANCE.start(Settings.getConfig().getWorldInfo().port());
		} catch (Throwable e) {
			Logger.handle(Launcher.class, "main", e);
			Logger.error(Launcher.class, "main", "Failed to initialize world server. Shutting down...");
			System.exit(1);
			return;
		}
		WorldThread.init();
		AutoUpdate.start();
		Logger.info(Launcher.class, "main", "Server launched in " + (System.currentTimeMillis() - currentTime) + " ms...");
		Logger.info(Launcher.class, "main", "Server is listening at " + InetAddress.getLocalHost().getHostAddress() + ":" + Settings.getConfig().getWorldInfo().port() + "...");
		Logger.info(Launcher.class, "main", "Player will be directed to "+Settings.getConfig().getWorldInfo()+"...");
		Logger.info(Launcher.class, "main", "World registration handled by SocialClient via gateway.");
		addCleanMemoryTask();
		Runtime.getRuntime().addShutdownHook(new Thread(Launcher::gracefulShutdown, "Shutdown Hook"));
	}

	private static void addCleanMemoryTask() {
		AsyncTaskExecutor.schedule(() -> {
			try {
				cleanMemory(WorldUtil.getMemUsedPerc() > Settings.HIGH_MEM_USE_THRESHOLD);
			} catch (Throwable e) {
				Logger.handle(Launcher.class, "addCleanMemoryTask", e);
			}
		}, 0, Ticks.fromMinutes(10));
		//}, 0, Ticks.fromSeconds(10));
	}

	private static final int AUTOSAVE_PERIOD_TICKS = Ticks.fromSeconds(30);

	public static void saveFilesSync() {
		for (Player player : World.getPlayers()) {
			if (player == null || !player.hasStarted() || player.hasFinished())
				continue;
			WorldDB.getPlayers().saveSync(player);
		}
		WorldPersistentData.save();
	}

	/** Each player saves on the one tick in the period that matches their index, so no tick serializes everybody. */
	public static void autosaveSlice() {
		long thisTick = World.getServerTicks() % AUTOSAVE_PERIOD_TICKS;
		for (Player player : World.getPlayers()) {
			if (player == null || !player.hasStarted() || player.hasFinished())
				continue;
			if (player.getIndex() % AUTOSAVE_PERIOD_TICKS == thisTick)
				WorldDB.getPlayers().save(player);
		}
		if (thisTick == 0)
			WorldPersistentData.save();
	}

	public static void cleanMemory(boolean force) {
		if (force) {
			ChunkManager.clearUnusedMemory();
			Logger.debug(Launcher.class, "cleanMemory", "Force cleaning cached data.");
		}
		System.gc(); System.gc();
	}

	public static void shutdown() {
		try {
			gracefulShutdown();
		} finally {
			System.exit(0);
		}
	}

	/**
	 * Safe to call from a shutdown hook, so it must never call {@link System#exit}. Everything that
	 * ends the process routes through here, so it runs at most once.
	 */
	public static void gracefulShutdown() {
		if (!SHUTTING_DOWN.compareAndSet(false, true))
			return;
		long start = System.currentTimeMillis();
		shutdownLog("draining world");
		int saved = 0;
		if (!WorldThread.isTicking())
			awaitSettledPlayers();
		try {
			WorldThread.requestStop();
			if (!WorldThread.isTicking() && !WorldThread.awaitStop(WORLD_DRAIN_TIMEOUT_MS))
				shutdownLog("world thread did not stop in " + WORLD_DRAIN_TIMEOUT_MS + "ms; saving anyway");
			saved = logoutAndSaveAll();
			WorldPersistentData.save();
		} catch (Throwable e) {
			shutdownLog("drain failed: " + e);
			e.printStackTrace();
		}
		closeServices();
		shutdownLog("drained " + saved + " player(s) in " + (System.currentTimeMillis() - start) + "ms");
	}

	private static void awaitSettledPlayers() {
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(World.SHUTDOWN_SETTLE_SECONDS);
		try {
			while (World.hasUnsettledPlayers()) {
				if (System.currentTimeMillis() >= deadline) {
					shutdownLog("players still dying or locked after " + World.SHUTDOWN_SETTLE_SECONDS + "s; shutting down anyway");
					return;
				}
				Thread.sleep(100);
			}
		} catch (Throwable e) {
			shutdownLog("waiting for deaths to settle failed: " + e);
		}
	}

	/**
	 * The logger is gone by now: java.util.logging closes its handlers from its own shutdown hook,
	 * concurrently with this one, so anything logged here is silently dropped.
	 */
	private static void shutdownLog(String message) {
		System.out.println("[shutdown] " + message);
		System.out.flush();
	}

	private static int logoutAndSaveAll() {
		int saved = 0;
		for (Player player : World.getPlayers()) {
			if (player == null || !player.hasStarted())
				continue;
			try {
				player.getPackets().sendLogout(true);
				player.realFinish();
			} catch (Throwable e) {
				shutdownLog("logout failed for " + player.getUsername() + ": " + e);
			}
			try {
				WorldDB.getPlayers().saveSync(player);
				saved++;
				shutdownLog("saved " + player.getUsername());
			} catch (Throwable e) {
				shutdownLog("save failed for " + player.getUsername() + ": " + e);
			}
		}
		return saved;
	}

	public static void closeServices() {
		WorldServer.INSTANCE.stop();
		AsyncTaskExecutor.shutdown();
	}

	private Launcher() {

	}

	public static void executeCommand(String cmd) {
		executeCommand(null, cmd);
	}

	public static void executeCommand(Player player, String cmd) {
		AsyncTaskExecutor.execute(() -> {
			try {
				String line;
				ProcessBuilder builder = new ProcessBuilder(cmd.split(" "));
				Process proc = builder.start();
				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				while ((line = in.readLine()) != null) {
					if (player != null)
						player.getPackets().sendDevConsoleMessage(line);
					Logger.debug(Launcher.class, "executeCommand", line);
				}
				proc.waitFor();
				in.close();
			} catch (IOException | InterruptedException e) {
				if (player != null)
					player.getPackets().sendDevConsoleMessage("Error: " + e.getMessage());
				Logger.handle(Launcher.class, "executeCommand", e);
			}
		});
	}
}
