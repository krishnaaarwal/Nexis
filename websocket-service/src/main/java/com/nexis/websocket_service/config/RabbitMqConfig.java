package com.nexis.websocket_service.config;


import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    //1. QUEUE : A queue stores messages until some consumer reads them.
    public static final String CODE_QUEUE = "nexis.code.queue";
    public static final String CHAT_QUEUE = "nexis.chat.queue";

    //2. EXCHANGE : Producers usually do not send messages directly to queues. They send messages to an exchange.
    public static final String TOPIC_EXCHANGE = "nexis.exchange";

    //3. ROUTING KEY : This is just a string attached to the message when it is published.
    public static final String CODE_ROUTING_KEY = "nexis.code.routing.key";
    public static final String CHAT_ROUTING_KEY = "nexis.chat.routing.key";

    @Bean
    public Queue codeQueue(){
        return new Queue(CODE_QUEUE,true);
    }

    @Bean
    public Queue chatQueue(){
        return new Queue(CHAT_QUEUE,true);
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(TOPIC_EXCHANGE);   // TopicExchange = routes by pattern matching on routing keys
    }

    @Bean
    public Binding codeBinding(Queue codeQueue , TopicExchange topicExchange){
        return BindingBuilder.bind(codeQueue).to(topicExchange).with(CODE_ROUTING_KEY);
    }


    @Bean
    public Binding chatBinding(Queue chatQueue , TopicExchange topicExchange){
        return BindingBuilder.bind(chatQueue).to(topicExchange).with(CHAT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitConvertor(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitConvertor());
        return template;
    }
}
