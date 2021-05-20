package cn.coding.WebSocketDemo.core;

import cn.coding.WebSocketDemo.util.GlobalUserUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MyChannelHandler extends SimpleChannelInboundHandler<Object> {

    private static final String URI = "websocket";

    private WebSocketServerHandshaker handshaker;

    /**
     * Connect to the Server
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("[handlerAdded] ====>" + ctx.channel().id());
        GlobalUserUtil.channels.add(ctx.channel());
    }

    /**
     * Disconnect
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.info("[handlerRemoved] ====>" + ctx.channel().id());
        GlobalUserUtil.channels.remove(ctx);
    }

    /**
     * The connection is abnormal, and the related resources need to be closed
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[System abnormal]=====>" + cause.toString());
        ctx.close();
        ctx.channel().close();
    }

    /**
     * An active channel can also be used as a user to connect to the client
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("[channelActive] ====>" + ctx.channel());
    }

    /**
     * An inactive channel means that the user has lose the connection
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    /**
     *Just complete flush here
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     *Here is to maintain a long connection between the server and client,
     * perform heartBeat detection, and avoid disconnection
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            PingWebSocketFrame ping = new PingWebSocketFrame();
            switch (stateEvent.state()) {
            // Read idle (Server side)
                case READER_IDLE:
                    log.info("[" + ctx.channel().remoteAddress() + "] Read idle (Server side)");
                    ctx.writeAndFlush(ping);
                    break;
            //Write idle (Client side)
                case WRITER_IDLE:
                    log.info("[" + ctx.channel().remoteAddress() + "] Write idle (Client side)");
                    ctx.writeAndFlush(ping);
                    break;
                case ALL_IDLE:
                    log.info("[" + ctx.channel().remoteAddress() + "] Read and write idle");
                    break;
            }
        }
    }
    /**
     *Send and receive messages processing
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            doHandlerHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            doHandlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
    /**
     * webSocket message processing
     */
    private void doHandlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame msg){
        //Determine which type of message is and react differently
        if (msg instanceof CloseWebSocketFrame) {
            log.info("[ShutDown]");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg);
            return;
        }
        if (msg instanceof PingWebSocketFrame) {
            log.info("[ping]");
            PongWebSocketFrame pong = new PongWebSocketFrame(msg.content().retain());
            ctx.channel().writeAndFlush(pong);
            return;
        }
        if (!(msg instanceof TextWebSocketFrame)) {
            log.info("[Does not support binary]");
            throw new UnsupportedOperationException("Dose not support binary");
        }
        //Group posting
        for (Channel channel : GlobalUserUtil.channels) {
            channel.writeAndFlush(new TextWebSocketFrame(((TextWebSocketFrame) msg).text()));
        }
    }
    /**
     * WebSocket first connection handshake
     */
    private void doHandlerHttpRequest(ChannelHandlerContext ctx, HttpRequest msg) {
        //http decoding failed
        if (!msg.getDecoderResult().isSuccess() || (!"websocket".equals(msg.headers().get("Upgrade")))){
            sendHttpResponse(ctx, (FullHttpRequest) msg, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
        }
        //Can get the uri of msg to judge
        String uri = msg.getUri();
        if (!uri.substring(1).equals(URI)) {
            ctx.close();
        }
        ctx.attr(AttributeKey.valueOf("type")).set(uri);
        //Other parameter can be obtained through
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory("ws://" + msg.headers().get("Host")
                + "/" + URI + "", null, false);
        handshaker = factory.newHandshaker(msg);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
        //Make a connection
        handshaker.handshake(ctx.channel(), (FullHttpRequest) msg);
        //Can do other processing
    }
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res){
        //Return the response to the client
        if (res.getStatus().code() !=200){
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        //If it not keep-alive, close the connection
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
