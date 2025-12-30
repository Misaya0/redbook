package com.itcast.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NOTE_TYPE_EXCHANGE = "note.type.exchange";
    public static final String NOTE_TYPE_QUEUE = "note.type.queue";
    public static final String NOTE_TYPE_ROUTING_KEY = "note.type.analyze";

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Queue noteTypeQueue() {
        return QueueBuilder.durable(NOTE_TYPE_QUEUE).build();
    }

    @Bean
    public TopicExchange noteTypeExchange() {
        return ExchangeBuilder.topicExchange(NOTE_TYPE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding noteTypeBinding(Queue noteTypeQueue, TopicExchange noteTypeExchange) {
        return BindingBuilder.bind(noteTypeQueue).to(noteTypeExchange).with(NOTE_TYPE_ROUTING_KEY);
    }
}
