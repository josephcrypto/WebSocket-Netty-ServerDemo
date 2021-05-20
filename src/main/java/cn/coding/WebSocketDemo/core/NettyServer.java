package cn.coding.WebSocketDemo.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@Data
@Slf4j
public class NettyServer {

    @Value("${netty.server.host}")
    public String host;
    @Value("${netty.server.port}")
    public Integer port;
    @Value("${netty.server.uri}")
    public String uri;


    private void startServer() {
        //The Server needs 2 thread groups boss to handle
        // the client connection work for processing after the client connection.
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, work).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //HttpServerCodec: decode request and response messages into HTTP messages
                            socketChannel.pipeline().addLast("http-codec", new HttpServerCodec());
                            //HttpObjectAggregator: combine multiple parts of the HTTP message into a complete HTTP message
                            socketChannel.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                            //ChunkedWriteHandler: Send HTML5 files to the client
                            socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            //Set up heartbeat detection
                            socketChannel.pipeline().addLast(new IdleStateHandler(60, 30, 60 * 30, TimeUnit.SECONDS));
                            //Configure channel processing for business processing
                            socketChannel.pipeline().addLast(new WebSocketServerHandler(host, port, uri));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true);
            //Bind ports, enable event-driven
            log.info("[Server Started Successfully========Port: " + port + "]");
            Channel channel = bootstrap.bind(port).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //Close resource
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    @PostConstruct
    public void init() {
        //Need to open a new thread to execute the netty service
        new Thread(() -> startServer()).start();
    }
}

