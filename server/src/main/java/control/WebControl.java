package control;

import core.NettyServer;
import handler.ProxyServerHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class WebControl {
    public static boolean createProxy(Integer proxyPort, String targetName, Integer targetPort, Integer time, boolean sync) {
        try {
            ProxyServerHandler proxyServerHandler = new ProxyServerHandler(targetName, targetPort + "", time, sync);
            ChannelFuture start = NettyServer.start(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new ByteArrayEncoder());
                    ch.pipeline().addLast(new ByteArrayDecoder());
                    ch.pipeline().addLast(proxyServerHandler);
                }
            }, proxyPort);
            HttpServerHandler.closeCache.put(targetName + targetPort, start);
            System.out.println("创建主机" + targetName + "的代理，目标端口:" + targetPort + "，代理端口:" + proxyPort);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}