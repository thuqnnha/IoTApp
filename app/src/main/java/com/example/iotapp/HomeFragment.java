package com.example.iotapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private LineChart lineChart1, lineChart2;
    private LineDataSet dataSet1, dataSet2;
    private LineData lineData1, lineData2;
    private int timeIndex = 0;
    private MqttHandler mqttHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Sử dụng ViewModel để lấy lại MQTT handler
        MqttViewModel mqttViewModel = new ViewModelProvider(requireActivity()).get(MqttViewModel.class);
        mqttHandler = mqttViewModel.getMqttHandler(getContext());

        // Lấy maDH từ Bundle truyền từ MainActivity
        String maDH = getArguments() != null ? getArguments().getString("maDH") : null;

        if (maDH != null && mqttHandler != null) {
            String topic = maDH + "_csdl_receive";
            mqttHandler.subscribe(topic); // Đăng ký topic theo tài khoản đăng nhập
            Log.d("HomeFragment", "Subscribed to topic: " + topic);
        } else {
            Log.e("HomeFragment", "maDH null hoặc mqttHandler null");
        }

        // Khởi tạo LineChart
        lineChart1 = view.findViewById(R.id.lineChart1);
        lineChart2 = view.findViewById(R.id.lineChart2);
        setupLineCharts();

        // Thiết lập callback nhận dữ liệu MQTT
        mqttHandler.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String receivedMessage = new String(message.getPayload()).trim();
                        String[] values = receivedMessage.split("\\s+");

                        if (values.length >= 2) {
                            try {
                                float heartRate = Float.parseFloat(values[0]);
                                float oxygenLevel = Float.parseFloat(values[1]);

                                if (dataSet1.getEntryCount() > 50) dataSet1.removeFirst();
                                if (dataSet2.getEntryCount() > 50) dataSet2.removeFirst();

                                dataSet1.addEntry(new Entry(timeIndex, heartRate));
                                dataSet2.addEntry(new Entry(timeIndex, oxygenLevel));

                                lineData1.notifyDataChanged();
                                lineData2.notifyDataChanged();
                                lineChart1.notifyDataSetChanged();
                                lineChart2.notifyDataSetChanged();
                                lineChart1.moveViewToX(timeIndex);
                                lineChart2.moveViewToX(timeIndex);

                                timeIndex++;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.e("HomeFragment", "MQTT connection lost: " + cause.getMessage());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        return view;
    }

    private void setupLineCharts() {
        dataSet1 = new LineDataSet(new ArrayList<>(), "Nhịp tim");
        dataSet1.setColor(Color.RED);
        dataSet1.setValueTextColor(Color.BLACK);

        dataSet2 = new LineDataSet(new ArrayList<>(), "Nồng độ Oxy");
        dataSet2.setColor(Color.BLUE);
        dataSet2.setValueTextColor(Color.BLACK);

        lineData1 = new LineData(dataSet1);
        lineData2 = new LineData(dataSet2);

        lineChart1.setData(lineData1);
        lineChart2.setData(lineData2);

        lineChart1.getDescription().setEnabled(false);
        lineChart1.animateX(1000);
        lineChart2.getDescription().setEnabled(false);
        lineChart2.animateX(1000);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Không ngắt kết nối MQTT để giữ kết nối liên tục
    }
}
