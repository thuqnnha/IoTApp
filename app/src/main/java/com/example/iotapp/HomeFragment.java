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
    private String maDH;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Lấy MQTT handler từ ViewModel
        MqttViewModel mqttViewModel = new ViewModelProvider(requireActivity()).get(MqttViewModel.class);
        mqttHandler = mqttViewModel.getMqttHandler(getContext());

        // Lấy maDH từ arguments
        if (getArguments() != null) {
            maDH = getArguments().getString("maDH", null);
        }

//        // Kiểm tra maDH và mqttHandler
//        if (maDH != null && mqttHandler != null) {
            String topic = "0000503341_csdl_receive";
            mqttHandler.subscribe(topic);
            Log.d("HomeFragment", "Subscribed to topic: " + topic);

            mqttHandler.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        String payload = new String(message.getPayload()).trim();
                        String[] values = payload.split("\\s+");

                        if (values.length >= 2) {
                            try {
                                float heartRate = Float.parseFloat(values[0]);
                                float oxygenLevel = Float.parseFloat(values[1]);

                                // Giới hạn số điểm dữ liệu tối đa 50
                                if (dataSet1.getEntryCount() > 50) {
                                    dataSet1.removeFirst();
                                    dataSet2.removeFirst();
                                }

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
                                Log.e("HomeFragment", "Parse error: " + e.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("HomeFragment", "MQTT connection lost: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

//        } else {
//            Log.e("HomeFragment", "maDH hoặc mqttHandler null");
//        }

        // Gán biểu đồ
        lineChart1 = view.findViewById(R.id.lineChart1);
        lineChart2 = view.findViewById(R.id.lineChart2);
        setupLineCharts();

        return view;
    }

    private void setupLineCharts() {
        dataSet1 = new LineDataSet(new ArrayList<>(), "Nhịp tim");
        dataSet1.setColor(Color.RED);
        dataSet1.setValueTextColor(Color.BLACK);
        dataSet1.setDrawValues(false); // Ẩn số trên điểm

        dataSet2 = new LineDataSet(new ArrayList<>(), "Nồng độ Oxy");
        dataSet2.setColor(Color.BLUE);
        dataSet2.setValueTextColor(Color.BLACK);
        dataSet2.setDrawValues(false); // Ẩn số trên điểm

        lineData1 = new LineData(dataSet1);
        lineData2 = new LineData(dataSet2);

        lineChart1.setData(lineData1);
        lineChart2.setData(lineData2);

        lineChart1.getDescription().setEnabled(false);
        lineChart1.setTouchEnabled(true);
        lineChart1.setDragEnabled(true);
        lineChart1.setScaleEnabled(true);
        lineChart1.animateX(1000);

        lineChart2.getDescription().setEnabled(false);
        lineChart2.setTouchEnabled(true);
        lineChart2.setDragEnabled(true);
        lineChart2.setScaleEnabled(true);
        lineChart2.animateX(1000);
    }
}
