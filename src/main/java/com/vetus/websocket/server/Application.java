package com.vetus.websocket.server;

import java.net.URI;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.Executors;

import com.sun.javafx.tools.packager.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.integration.websocket.WebSocketListener;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;


@Configuration
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
        System.out.println("Hit 'Enter' to terminate");
        System.in.read();
        ctx.close();
    }

    @Bean
    public ServerWebSocketContainer serverWebSocketContainer() {
        return new MessagesWebSocketContainer("/time/{userId}")
                .withSockJs();
    }

    /*@Bean
    @InboundChannelAdapter(value = "splitChannel", poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1"))
    public MessageSource<?> webSocketSessionsMessageSource() {
        return new MessageSource<Iterator<String>>() {
            public Message<Iterator<String>> receive() {
                return new GenericMessage<Iterator<String>>(serverWebSocketContainer().getSessions().keySet().iterator());
            }
        };
    }*/

    @Bean
    public MessageChannel splitChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "splitChannel")
    public MessageHandler splitter() {
        DefaultMessageSplitter splitter = new DefaultMessageSplitter();
        splitter.setOutputChannelName("headerEnricherChannel");
        return splitter;
    }

    @Bean
    public MessageChannel headerEnricherChannel() {
        return new ExecutorChannel(Executors.newCachedThreadPool());
    }

    @Bean
    @Transformer(inputChannel = "headerEnricherChannel", outputChannel = "transformChannel")
    public HeaderEnricher headerEnricher() {
        return new HeaderEnricher(Collections.singletonMap(SimpMessageHeaderAccessor.SESSION_ID_HEADER,
                new ExpressionEvaluatingHeaderValueMessageProcessor<Object>("payload", null)));
    }

    @Bean
    @Transformer(inputChannel = "transformChannel", outputChannel = "sendTimeChannel")
    public AbstractPayloadTransformer<?, ?> transformer() {
        return new AbstractPayloadTransformer<Object, Object>() {
            @Override
            protected Object transformPayload(Object payload) throws Exception {
                return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.DEFAULT).format(new Date());
            }

        };
    }


    @Bean
    public MessageChannel sendTimeChannel() {
        return new PublishSubscribeChannel();
    }


    @Bean
    @ServiceActivator(inputChannel = "sendTimeChannel")
    public MessageHandler webSocketOutboundAdapter() {
        return new WebSocketOutboundMessageHandler(serverWebSocketContainer());
    }

    @Bean
    @ServiceActivator(inputChannel = "sendTimeChannel")
    public MessageHandler loggingChannelAdapter() {
        LoggingHandler loggingHandler = new LoggingHandler("info");
        loggingHandler.setLoggerName(
                "'The time ' + payload + ' has been sent to the WebSocketSession ' + headers.simpSessionId");
        return loggingHandler;
    }

}
