package handler;

import com.alibaba.fastjson.JSONObject;
import core.GoClientTable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import utils.Message;
import utils.Status;
import utils.TcpQueue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Description 端口转发处理类
 */
@ChannelHandler.Sharable
public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    public final static Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final String port;
    private final Integer time;
    private final boolean sync;
    private final String targetName;

    public ProxyServerHandler(String targetName, String port, Integer time, boolean sync) {
        this.port = port;
        this.time = time;
        this.sync = sync;
        this.targetName = targetName;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (!sync) {
            channels.put(targetName + port, ctx.channel());
            if (ServerHandler.cache.containsKey(targetName + port)) {
                Set<byte[]> bytes = ServerHandler.cache.get(targetName + port);
                if (bytes != null && bytes.size() > 0) {
                    for (byte[] b : bytes) {
                        ctx.channel().writeAndFlush(b);
                    }
                }
            } else if ("22".equals(port)) {
                System.out.printf("%s与目标主机的SSH隧道未建立，请稍微重试...", targetName);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.channel().close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object data) throws Exception {
        byte[] msg = (byte[]) data;
        GoClientTable goClientTable = ServerHandler.clientChannel.get(targetName + port);
        if (goClientTable == null) {
            System.out.println("目标主机" + targetName + "不存在");
            return;
        }
        goClientTable.setWriteBytes(goClientTable.getWriteBytes() + msg.length);
        Channel channel = goClientTable.getConn();
        if (channel != null) {
            Message message = new Message();
            message.setType(Status.data);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("port", port);
            message.setLength(jsonObject.toJSONString().getBytes(CharsetUtil.UTF_8).length);
            message.setInfo(jsonObject);
            message.setData(msg);
            channel.writeAndFlush(message);
            if (this.sync) {
                Object take = TcpQueue.getQueue(targetName + port).poll(checkTime(time), TimeUnit.SECONDS);
                if (take == null) {
                    String response = time + "秒超时未响应";
                    ctx.channel().writeAndFlush(response.getBytes());
                } else {
                    ctx.channel().writeAndFlush(take);
                }
                ctx.close();
            }
        }
    }

    public int checkTime(Integer time) {
        if (time > 30) {
            return 30;
        }
        return 3;
    }

}

