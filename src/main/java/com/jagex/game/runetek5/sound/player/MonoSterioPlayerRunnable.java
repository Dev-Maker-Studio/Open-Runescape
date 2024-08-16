package com.jagex.game.runetek5.sound.player;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.tools.TimeTools;

public class MonoSterioPlayerRunnable extends AbstractMonoSterioPlayerRunnable {

    @Override
    public void run() {
        super.running = true;

        try {
            try {
                while (!super.exit) {
                    for (int index = 0; index < 2; index++) {
                        PcmPlayer player = super.monoSterioPlayers[index];
                        if (player != null) {
                            player.cycle();
                        }
                    }

                    TimeTools.sleep(10L);
                    GameShell.performDummyActionevent(null);
                }
            } catch (Exception innerException) {
                PacketEncoder.sendErrorToServer(null, innerException);
                super.running = false;
                return;
            }

            super.running = false;
        } catch (Exception outerException) {
            super.running = false;
        }

    }

}
