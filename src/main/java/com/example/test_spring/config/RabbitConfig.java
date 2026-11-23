package com.example.test_spring.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbit.exchange}")
    private String exchangeName;

//    @Value("${app.rabbit.queue:auth.events.queue}")
//    private String queueName;

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(exchangeName);
    }

//    @Bean
//    public Queue authQueue() {
//        return new Queue(queueName, true);
//    }

//    @Bean
//    public Binding authBinding(Queue authQueue, TopicExchange authExchange) {
//        return BindingBuilder.bind(authQueue).to(authExchange).with("auth.user.*");
//    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
