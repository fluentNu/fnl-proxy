package handler;

import com.alibaba.fastjson.JSONObject;
import control.HttpServerHandler;
import core.GoClientTable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import utils.Message;
import utils.Status;
import utils.TcpQueue;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @Description 服务端处理器
 */
@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    //注意保证线程安全
    public static volatile Map<String, GoClientTable> clientChannel = new ConcurrentHashMap<>();
    public static volatile Map<String, Set<byte[]>> cache = new ConcurrentHashMap<>();
    public static volatile Set<Channel> keepaliveChannel = new CopyOnWriteArraySet<>();
    private final boolean sync;

    public ServerHandler(boolean sync) {
        this.sync = sync;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        keepaliveChannel.add(ctx.channel());

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        int type = msg.getType();
        //如果是客户端的连接请求
        if (type == Status.conn) {
            //那么注册
            this.register(msg, ctx);
            //如果是back类型
        } else if (type == Status.back) {
            //那么……
            JSONObject info = msg.getInfo();
            String targetName = (String) info.get("targetName");
            String port = String.valueOf(info.get("port"));
            GoClientTable goClientTable = ServerHandler.clientChannel.get(targetName + port);
            //check是做什么用的目前不清楚
            if (msg.getCheck() > msg.getLength()) {
                goClientTable.setReadBytes(goClientTable.getReadBytes() + (msg.getCheck() - msg.getLength()));
            }
            if (sync) {
                //放到该端口对应的消息队列中去
                TcpQueue.getQueue(port).add(msg.getData());
            } else if (!ProxyServerHandler.channels.containsKey(targetName + port)) {
                Set<byte[]> bytes = cache.get(targetName + port);
                //异常检查
                if (bytes == null || bytes.size() == 0) {
                    bytes = new CopyOnWriteArraySet<>();
                    bytes.add(msg.getData());
                    cache.put(targetName + port, bytes);
                }
            } else {
                Set<byte[]> bytes = cache.get(targetName + port);
                if (bytes != null) {
                    for (byte[] aByte : bytes) {
                        if (Arrays.equals(aByte, msg.getData())) {
                            return;
                        }
                    }
                }
                ProxyServerHandler.channels.forEach((key, value) -> {
                    //遍历，找到正确的客户端
                    if (key.contains(targetName + port)) {
                        //向客户端发送消息
                        if (msg.getData() != null) {
                            value.writeAndFlush(msg.getData());
                        } else {
                            value.writeAndFlush("".getBytes());
                        }
                    }
                });
            }
        }
        //如果是客户端的pong消息，那么回应ping
        else if (type == Status.pong) {
            JSONObject info = msg.getInfo();
            String targetName = (String) info.get("targetName");
            System.out.println("收到" + targetName + "的pong...");
        }
    }

    /**
     * @param message tcp协议
     * @param ctx     上下文
     * @Description 注册客户端
     */
    private void register(Message message, ChannelHandlerContext ctx) {
        //获取GoClientTable对象
        GoClientTable gct = JSONObject.toJavaObject(message.getInfo(), GoClientTable.class);
        //注册别名 + socket
        gct.setCreateTime(System.currentTimeMillis());
        //目的IP
        gct.setDesc(HttpServerHandler.cacheSettings.get(gct.getName() + gct.getPort() + "desc"));
        //代理端口
        gct.setProxyPort(HttpServerHandler.cacheSettings.get(gct.getName() + gct.getPort() + "proxyPort") == null ? "0" : HttpServerHandler.cacheSettings.get(gct.getName() + gct.getPort() + "proxyPort"));
        if (clientChannel.get(gct.getName() + gct.getPort()) != null) {
            //如果之前的客户端连接还在，那么就把它关闭，因为要重新连接了
            if (clientChannel.get(gct.getName() + gct.getPort()).getConn().isActive()) {
                clientChannel.get(gct.getName() + gct.getPort()).getConn().close();
            }
            System.out.printf("%s与服务器重新连接,并重新注册了端口%s%n", gct.getName(), gct.getPort());
        } else {
            System.out.printf("%s向服务器注册了端口%s%n", gct.getName(), gct.getPort());
        }
        gct.setConn(ctx.channel());
        clientChannel.put(gct.getName() + gct.getPort(), gct);

        Message sender = new Message();
        //连接成功的相应代码
        sender.setType(Status.localConn);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("port", gct.getPort());
        sender.setInfo(jsonObject);
        //由于这个地方一定是0，所以可以删了
        sender.setData(message.getData());
        sender.setLength(jsonObject.toString().getBytes(CharsetUtil.UTF_8).length);
        ctx.channel().writeAndFlush(sender);
    }

}

