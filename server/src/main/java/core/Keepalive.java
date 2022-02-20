package core;

import handler.ServerHandler;
import io.netty.channel.Channel;
import utils.MessageBuild;
import utils.Status;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description 与客户端保持通讯
 **/
public class Keepalive {
    //ScheduledThreadPoolExecutor继承ThreadPoolExecutor来重用线程池的功能
    //ServerThreadFactory是ThreadFactory的一个实现类
    private final ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1, new ServerThreadFactory("keepalive"));

    public void ping() {
        //command：执行线程
        //initialDelay：初始化延时
        //period：两次开始执行最小间隔时间
        //unit：计时单位
        threadPoolExecutor.scheduleAtFixedRate(() -> {
            //清理已经断开的数据
            //ServerHandler是读msg的管道
            ServerHandler.clientChannel.values()
                    //下面这个语法糖有点恶心人
                    //goClientTable是传入参数，执行goClientTable.getConn().isActive()方法，然后取反。
                    //如果已经断开连接，那么删除
                    .removeIf(goClientTable -> !goClientTable.getConn().isActive());
            //发送心跳
            for (Channel channel : ServerHandler.keepaliveChannel) {
                if (channel.isOpen() && channel.isActive()) {
                    //格式化的ping
                    channel.writeAndFlush(MessageBuild.onlyType(Status.ping));
                } else {
                    channel.close();
                }
            }
            //第一次5s后ping，之后每隔3s钟ping一下
        }, 5, 3, TimeUnit.SECONDS);
    }
}
