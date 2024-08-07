package io.github.future0923.debug.power.client.thread;

import io.github.future0923.debug.power.base.logging.Logger;
import io.github.future0923.debug.power.base.utils.DebugPowerThreadUtils;
import io.github.future0923.debug.power.client.holder.ClientSocketHolder;
import io.github.future0923.debug.power.common.protocal.packet.request.HeartBeatRequestPacket;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author future0923
 */
public class HeartBeatRequestThread extends Thread {

    private static final Logger logger = Logger.getLogger(HeartBeatRequestThread.class);

    private final ClientSocketHolder holder;

    private final int interval;

    public HeartBeatRequestThread(ClientSocketHolder holder, int interval) {
        setDaemon(true);
        setName("DebugPower-HeartBeatRequest-Thread");
        this.holder = holder;
        this.interval = interval;
    }

    @Override
    public void run() {
        int retryCount = 0;
        while (!Thread.currentThread().isInterrupted() && retryCount < 30) {
            if (!DebugPowerThreadUtils.sleep(this.interval, TimeUnit.SECONDS)) {
                return;
            }
            if (!holder.isClosed()) {
                try {
                    HeartBeatRequestPacket.INSTANCE.writeAndFlush(holder.getOutputStream());
                    retryCount = 0;
                    holder.setRetry(ClientSocketHolder.INIT);
                    continue;
                } catch (IOException e) {
                    holder.closeSocket();
                    logger.error("HeartBeatRequest happen error", e);
                }
            }
            logger.warning("HeartBeatRequest reconnect debug power server");
            holder.setRetry(ClientSocketHolder.RETRYING);
            try {
                holder.connect();
            } catch (IOException e) {
                logger.error("connect server error, Try again in {} seconds.", e, this.interval);
            }
            retryCount++;
        }
        holder.setRetry(ClientSocketHolder.FAIL);
    }
}