package com.example.iotapp;

import android.content.Context;

import androidx.lifecycle.ViewModel;

public class MqttViewModel extends ViewModel {
    private MqttHandler mqttHandler;

    public MqttHandler getMqttHandler(Context context) {
        if (mqttHandler == null) {
            mqttHandler = new MqttHandler(context);
            mqttHandler.connect();
        }
        return mqttHandler;
    }
}

