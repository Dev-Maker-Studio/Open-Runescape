package com.rs.engine.thread;

import com.rs.Launcher;
import com.rs.Settings;
import com.rs.game.World;
import com.rs.game.map.ChunkManager;
import com.rs.game.map.instance.InstanceBuilder;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.OwnedObject;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import com.rs.utils.Timer;
import com.rs.utils.WorldUtil;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class WorldThread extends Thread {

	public static long START_CYCLE;
	public static volatile long WORLD_CYCLE;
	private static long LOWEST_TICK = 50000;
	private static long TICKS;
    private static long TOTAL_TIME = 0;
	private static long HIGHEST_TICK = 0;

	protected WorldThread() {
		setPriority(Thread.MAX_PRIORITY);
		setName("World Thread");
		setUncaughtExceptionHandler((th, ex) -> Logger.handle(WorldThread.class, "uncaughtExceptionHandler", ex));
	}

	private static volatile boolean running = true;
	private static volatile Thread ticking;
	private static final CountDownLatch STOPPED = new CountDownLatch(1);

	public static void init() {
		WORLD_CYCLE = START_CYCLE = System.currentTimeMillis() / 600L;
		AsyncTaskExecutor.getWorldThreadExecutor().execute(new WorldThread());
	}

	public static void requestStop() {
		running = false;
	}

	public static boolean isTicking() {
		return Thread.currentThread() == ticking;
	}

	/** False when the tick did not finish in time; the caller drains anyway rather than hang a shutdown. */
	public static boolean awaitStop(long timeoutMs) {
		try {
			return STOPPED.await(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public static Set<String> NAMES = new HashSet<>();

	@Override
	public void run() {
		Configuration config = null;
		if (Settings.getConfig().isEnableJFR()) {
			try {
				config = Configuration.create(Paths.get("./darkan.jfc"));
			} catch (IOException | ParseException e) {
				Logger.handle(WorldThread.class, "run", e);
				throw new RuntimeException(e);
			}
			if (config == null) {
				RuntimeException e = new RuntimeException("Unable to load darkan flight recorder template.");
				Logger.handle(WorldThread.class, "run", e);
				throw e;
			}
		}
		Logger.debug(WorldThread.class, "run", "WorldThread initialized...");
		ticking = Thread.currentThread();
		while(running) {
			long startTime = System.currentTimeMillis();
			Recording tickRecording = Settings.getConfig().isEnableJFR() ? new Recording(config) : null;
			try {
				if (Settings.getConfig().isEnableJFR()) {
					Timer timerJFR = new Timer().start();
					tickRecording.start();
					Logger.trace(WorldThread.class, "tick", "JFR recording() - " + timerJFR.stop());
				}
				Timer timerChunk = new Timer().start();
				ChunkManager.processChunks();
				Logger.trace(WorldThread.class, "tick", "processChunks() - " + timerChunk.stop());
				Timer timerTask = new Timer().start();
				WorldTasks.processTasks();
				Logger.trace(WorldThread.class, "tick", "processTasks() - " + timerTask.stop());
				OwnedObject.processAll();
				Timer timerNpcProcTasks = new Timer().start();
				for (NPC npc : World.getNPCs()) {
					try {
						if (npc == null || npc.hasFinished())
							continue;
						npc.processTasks();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "run:npcProcessEntityTasks", "Error processing NPC: " + (npc == null ? "NULL NPC" : npc.getId()), e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "npcProcessEntityTasks() - " + timerNpcProcTasks.stop());

				Timer timerPlayerProcTasks = new Timer().start();
				for (Player player : World.getPlayers()) {
					try {
						player.processTasks();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "run:playerProcessEntityTasks", "Error processing player: " + (player == null ? "NULL PLAYER" : player.getUsername()), e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "playerProcessEntityTasks() - " + timerPlayerProcTasks.stop());
				NAMES.clear();
				Timer timerNpcProc = new Timer().start();
				for (NPC npc : World.getNPCs()) {
					try {
						if (npc == null || npc.hasFinished())
							continue;
						npc.processEntity();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "run:npcProcessEntity", "Error processing NPC: " + (npc == null ? "NULL NPC" : npc.getId()), e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "npcProcessEntity() - " + timerNpcProc.stop());

				Timer timerPlayerProc = new Timer().start();
				for (Player player : World.getPlayers()) {
					try {
						if (player != null && player.getTempAttribs().getB("realFinished"))
							player.realFinish();
						if (player == null || !player.hasStarted() || player.hasFinished())
							continue;
						if (NAMES.contains(player.getUsername()))
							player.logout(false);
						else
							NAMES.add(player.getUsername());
						player.processEntity();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "run:playerProcessEntity", "Error processing player: " + (player == null ? "NULL PLAYER" : player.getUsername()), e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "playerProcessEntity() - " + timerPlayerProc.stop());

				Timer timerNpcMove = new Timer().start();
				for (NPC npc : World.getNPCs()) {
					if (npc == null || npc.hasFinished())
						continue;
					try {
						npc.processMovement();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "processNPCMovement", "NPC " + npc.getId() + " at " + npc.getTile() + " (spawned at " + npc.getRespawnTile() + ")", e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "processNPCMovement() - " + timerNpcMove.stop());

				Timer timerPlayerMove = new Timer().start();
				for (Player player : World.getPlayers()) {
					if (player == null || !player.hasStarted() || player.hasFinished())
						continue;
					try {
						player.processMovement();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "processPlayerMovement", e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "processPlayerMovement() - " + timerPlayerMove.stop());

				Timer timerEntityUpdate = new Timer().start();
				for (Player player : World.getPlayers()) {
					if (player == null || !player.hasStarted() || player.hasFinished())
						continue;
					try {
						player.getPackets().sendLocalPlayersUpdate();
						player.getPackets().sendLocalNPCsUpdate();
						player.postSync();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "processPlayersPostSync", e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "processPlayersPostSync() - " + timerEntityUpdate.stop());

				Timer timerUpdateZones = new Timer().start();
				ChunkManager.processUpdateZones();
				Logger.trace(WorldThread.class, "tick", "processUpdateZones() - " + timerUpdateZones.stop());

				Timer timerFlushPackets = new Timer().start();
				for (Player player : World.getPlayers()) {
					if (player == null || !player.hasStarted() || player.hasFinished())
						continue;
					try {
						player.getSession().requestFlush();
					} catch (Throwable e) {
						Logger.handle(WorldThread.class, "flushPlayerPackets", e);
					}
				}
				Logger.trace(WorldThread.class, "tick", "flushPlayerPackets() - " + timerFlushPackets.stop());

				for (Player player : World.getPlayers()) {
					if (player == null || !player.hasStarted() || player.hasFinished())
						continue;
					player.resetMasks();
				}
				for (NPC npc : World.getNPCs()) {
					if (npc == null || npc.hasFinished())
						continue;
					npc.resetMasks();
				}
				World.processEntityLists();
				Launcher.autosaveSlice();
				long time = (System.currentTimeMillis() - startTime);
				Logger.trace(WorldThread.class, "tick", "Tick finished - Mem: " + (Utils.formatDouble(WorldUtil.getMemUsedPerc())) + "% - " + time + "ms - Players online: " + World.getPlayerCount());

				if (time > HIGHEST_TICK)
					HIGHEST_TICK = time;
				if (time < LOWEST_TICK)
					LOWEST_TICK = time;
				TICKS++;
				TOTAL_TIME += time;
                long AVERAGE_TICK = TOTAL_TIME / TICKS;

				if (time > 300L) {
					if (Settings.getConfig().isEnableJFR() && tickRecording != null)
						tickRecording.stop();
					Logger.warn(WorldThread.class, "tick", "Tick took " + time + "ms (threshold: 300ms)");
				}
			} catch (Throwable e) {
				Logger.handle(WorldThread.class, "tick", e);
			}
			long endTime = System.currentTimeMillis();
			long timeToSleep = Math.max(600 - (endTime - startTime), 0);
			try {
				if (timeToSleep > 0)
					Thread.sleep(timeToSleep);
			} catch (InterruptedException e) { }
			WORLD_CYCLE++;
		}
		Logger.info(WorldThread.class, "run", "WorldThread stopped.");
		STOPPED.countDown();
	}
}
