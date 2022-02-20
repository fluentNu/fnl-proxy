package control;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import core.GoClientTable;
import handler.ServerHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.StringUtil;
import org.apache.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description 服务端处理器
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    public static final Map<String, String> cacheSettings = new ConcurrentHashMap<>();
    public static final Map<String, ChannelFuture> closeCache = new ConcurrentHashMap<>();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (HttpMethod.OPTIONS.name().equals(request.method().name().toUpperCase())) {
            ctx.channel().writeAndFlush(response(HttpStatus.SC_OK, ""));
            return;
        }
        if (HttpMethod.POST.name().equals(request.method().name().toUpperCase())) {
            ByteBuf content = request.content();
            byte[] reqContent = new byte[content.readableBytes()];
            content.readBytes(reqContent);
            String strContent = new String(reqContent, StandardCharsets.UTF_8);
            JSONObject json = JSONObject.parseObject(strContent);
            switchMethod(json, ctx.channel());
        }
    }

    /**
     * @param json    请求数据
     * @param channel socket
     * @Description 路由
     */
    public void switchMethod(JSONObject json, Channel channel) {
        String method = (String) json.get("method");
        if (StringUtil.isNullOrEmpty(method)) {
            channel.writeAndFlush(response(HttpStatus.SC_BAD_REQUEST, "can not found method"));
            return;
        }
        JSONObject params = (JSONObject) json.get("params");
        switch (method.toLowerCase()) {
            //新建端口代理
            case "create":
                params.getBoolean("sync");
                boolean ok = WebControl.createProxy(params.getInteger("proxyPort"), params.getString("targetName"), params.getInteger("targetPort"), params.getInteger("time"),
                        params.getBoolean("sync") != null && params.getBoolean("sync"));
                if (ok) {
                    String member = params.getString("targetName") + params.getInteger("targetPort");
                    //cache 初始化设置
                    cacheSettings.put(member + "proxyPort", String.valueOf(params.getInteger("proxyPort")));
                    cacheSettings.put(member + "desc", params.getString("desc"));
                    //配置setting
                    GoClientTable goClientTable = ServerHandler.clientChannel.get(member);
                    if (goClientTable != null) {
                        goClientTable.setProxyPort(String.valueOf(params.getInteger("proxyPort")));
                        goClientTable.setDesc(params.getString("desc"));
                    }
                    channel.writeAndFlush(response(HttpStatus.SC_OK, ""));
                } else {
                    channel.writeAndFlush(response(HttpStatus.SC_INTERNAL_SERVER_ERROR, ""));
                }
                break;
            //关闭原端口代理
            case "delete":
                String member = params.getString("targetName") + params.getInteger("targetPort");
                GoClientTable goClientTable = ServerHandler.clientChannel.get(member);
                if (goClientTable != null) {
                    goClientTable.setProxyPort("0");
                    goClientTable.setDesc("");
                }
                //关闭代理端口
                if (closeCache.get(member) != null) {
                    closeCache.get(member).channel().disconnect();
                    closeCache.remove(member);
                }
                channel.writeAndFlush(response(HttpStatus.SC_OK, ""));
            case "select":
                channel.writeAndFlush(response(HttpStatus.SC_OK, JSON.toJSONString(ServerHandler.clientChannel.values(),
                        SerializerFeature.DisableCircularReferenceDetect)));
                break;
            default:
                channel.writeAndFlush(response(HttpStatus.SC_NOT_FOUND, ""));
                break;
        }
    }

    public FullHttpResponse response(int status, String jsonContent) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(jsonContent.getBytes()));
        response.headers().set("Content-Type", "text/plain;charset=UTF-8");
        response.headers().set("Content-Length", response.content().readableBytes());
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Access-Control-Allow-Headers", "*");//允许headers自定义
        response.headers().set("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS");
        return response;
    }
}

