package com.nexis.recording_service.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Enforces that all objects sent/received via RabbitTemplate are converted to/from JSON bytes
        return new Jackson2JsonMessageConverter();
    }

    public static final String CODE_SHARD_0 = "nexis.code.queue.shard0";
    public static final String CODE_SHARD_1 = "nexis.code.queue.shard1";
    public static final String CODE_SHARD_2 = "nexis.code.queue.shard2";

    public static final String CHAT_QUEUE = "nexis.chat.queue";

    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true);
    }

    @Bean
    public Queue codeShard0() {
        return new Queue(CODE_SHARD_0, true);
    }

    @Bean
    public Queue codeShard1() {
        return new Queue(CODE_SHARD_1, true);
    }

    @Bean
    public Queue codeShard2() {
        return new Queue(CODE_SHARD_2, true);
    }
}