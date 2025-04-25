package com.example.iotapp;

import android.content.Context;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {
    private MqttClient mqttClient;
    private Context context;
    private boolean isConnected = false; // Biến kiểm tra trạng thái kết nối

    public MqttHandler(Context context) {
        this.context = context;
    }

    public void connect() {
        String serverURI = "ssl://1cbe44e0762541ce800bd4fb8b250296.s1.eu.hivemq.cloud:8883";
        String clientId = MqttClient.generateClientId();
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            mqttClient = new MqttClient(serverURI, clientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("IOT_BL");
            options.setPassword("123456789".toCharArray());
            options.setCleanSession(true);

            mqttClient.connect(options);
            isConnected = true; // Đánh dấu là đã kết nối

        } catch (MqttException e) {
            e.printStackTrace();
            isConnected = false;
        }
    }

    public void subscribe(String topic) {
        if (mqttClient != null && isConnected) {
            try {
                mqttClient.subscribe(topic, 0);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void publish(String topic, String message) {
        if (mqttClient != null && isConnected) {
            try {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttClient.publish(topic, mqttMessage);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        if (mqttClient != null && isConnected) {
            try {
                mqttClient.disconnect();
                isConnected = false;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void setCallback(MqttCallback callback) {
        if (mqttClient != null) {
            mqttClient.setCallback(callback);
        }
    }
}
