package core;

import handler.ServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import utils.ProxyDecode;
import utils.ProxyEncode;

/**
 * @Description 服务端管道
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final boolean sync;
    public ServerInitializer(boolean sync){
        this.sync = sync;
    }
    @Override
    protected void initChannel(SocketChannel ch){
        //自定义长度帧解码器
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,12,4,0,0));
        ch.pipeline().addLast(new ProxyEncode());
        ch.pipeline().addLast(new ProxyDecode());
        ch.pipeline().addLast(new ServerHandler(sync));
    }
}
