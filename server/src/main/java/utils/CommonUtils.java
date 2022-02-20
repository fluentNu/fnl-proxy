package utils;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;

public class CommonUtils {
    public static TypeEnum useNio() {
        //获取操作系统名称
        String osName = System.getProperty("os.name").toLowerCase();

        //目前并不知道为什么要这么选择调度模式
        if (osName.contains("linux") && Epoll.isAvailable()) {
            return TypeEnum.EPOLL;
        } else if (osName.contains("mac") && KQueue.isAvailable()) {
            return TypeEnum.KQUEUE;
        } else {
            return TypeEnum.SELECT;
        }
    }

    public static int nThread() {
        return Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    }
}
