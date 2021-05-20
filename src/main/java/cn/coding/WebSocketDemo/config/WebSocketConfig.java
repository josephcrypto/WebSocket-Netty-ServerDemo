package cn.coding.WebSocketDemo.config;

import cn.coding.WebSocketDemo.core.WebSocketServerHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {

//    @Value("${netty.server.host}")
//    public String host;
//
//    @Value("${netty.server.port}")
//    public Integer port;
//
//    @Value("${netty.server.uri}")
//    public String uri;
//
//    @Bean
//    public ServerEndpointExporter serverEndpointExporter() {
//
//        return new ServerEndpointExporter();
//    }
//
//    @Bean
//    public WebSocketServerHandler webSocketServerHandler() {
//        return new WebSocketServerHandler(host, port, uri);
//    }
//
}
