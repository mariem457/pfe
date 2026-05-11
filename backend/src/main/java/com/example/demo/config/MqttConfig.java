package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttConfig {

    public static final String MQTT_INPUT_CHANNEL = "mqttInputChannel";

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client.id:springClient-wisetrash}")
    private String clientId;

    @Value("${mqtt.topic:bins/#}")
    private String topic;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    public MqttConfig() {
        System.out.println("🔥 MQTT CONFIG CONSTRUCTOR CALLED");
    }

    @PostConstruct
    public void debugMqttConfig() {
        System.out.println("🚀 MQTT CONFIG LOADED");
        System.out.println("MQTT brokerUrl = " + brokerUrl);
        System.out.println("MQTT clientId = " + clientId);
        System.out.println("MQTT topic = " + topic);
    }

    @Bean(name = MQTT_INPUT_CHANNEL)
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        if (username != null && !username.isBlank()) {
            options.setUserName(username);
        }

        if (password != null && !password.isBlank()) {
            options.setPassword(password.toCharArray());
        }

        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageProducer inbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        brokerUrl,
                        clientId + "_inbound",
                        mqttClientFactory,
                        topic
                );

        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);
        adapter.setAutoStartup(true);
        adapter.setOutputChannel(mqttInputChannel());

        System.out.println("MQTT ADAPTER CREATED FOR TOPIC = " + topic);

        return adapter;
    }
}