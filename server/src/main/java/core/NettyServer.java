package core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import utils.CommonUtils;
import utils.TypeEnum;

/**
 * @Description 端口代理服务端
 */
public class NettyServer {
    //我觉得这个类写成静态方法比较好
    //参数是netty的初始化器
    public static synchronized ChannelFuture start(ChannelHandler initializer, int port) {
        //根据操作系统返回系统调用的模型
        TypeEnum typeEnum = CommonUtils.useNio();
        //事件循环组
        //Reactor 主从多线程模型
        //处理socket连接的线程就是Boss线程
        EventLoopGroup bossGroup;
        //服务端接受了socket连接求后，会产生一个channel
        //并把这个channel交给Worker线程来处理,boss线程则继续处理socket的请求
        EventLoopGroup workerGroup;
        try {
            //ServerBootstrap负责初始化netty服务器，并且开始监听端口的socket请求
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            //配置服务
            //ChannelOption.SO_KEEPALIVE参数对应于套接字选项中的SO_KEEPALIVE，
            //该参数用于设置TCP连接，当设置该选项以后，连接会测试链接的状态，
            //这个选项用于可能长时间没有数据交流的
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            //ChannelOption.SO_REUSEADDR对应于套接字选项中的SO_REUSEADDR，
            //这个参数表示允许重复使用本地地址和端口，
            //因为可能有多个客户端连接本服务器
            serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
            if (typeEnum == TypeEnum.EPOLL) {
                //CommonUtils.nThread()返回线程数
                //EventLoopGroup就是一组EventLoop（事件循环对象）
                bossGroup = new EpollEventLoopGroup(CommonUtils.nThread(), new ServerThreadFactory("ePollBoss"));
                workerGroup = new EpollEventLoopGroup(CommonUtils.nThread(), new ServerThreadFactory("ePollWork"));
                serverBootstrap.group(bossGroup, workerGroup).channel(EpollServerSocketChannel.class);
                ;
            } else if (typeEnum == TypeEnum.KQUEUE) {
                bossGroup = new KQueueEventLoopGroup(CommonUtils.nThread(), new ServerThreadFactory("kQueueBoss"));
                workerGroup = new KQueueEventLoopGroup(CommonUtils.nThread(), new ServerThreadFactory("kQueueWork"));
                serverBootstrap.group(bossGroup, workerGroup).channel(KQueueServerSocketChannel.class);
            } else {
                bossGroup = new NioEventLoopGroup(CommonUtils.nThread(), new ServerThreadFactory("nioBoss"));
                workerGroup = new NioEventLoopGroup(CommonUtils.nThread(), new ServerThreadFactory("nioWork"));
                serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            }
            serverBootstrap.childHandler(initializer);
            ChannelFuture f = serverBootstrap.bind(port).sync();
            f.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            });
            return f;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
