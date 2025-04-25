package com.example.iotapp;

import android.graphics.Color;
import android.os.Bundle;
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

        // Sử dụng ViewModel để tránh tạo lại MQTT mỗi lần vào Fragment
        MqttViewModel mqttViewModel = new ViewModelProvider(requireActivity()).get(MqttViewModel.class);
        mqttHandler = mqttViewModel.getMqttHandler(getContext());

        // Khởi tạo LineChart
        lineChart1 = view.findViewById(R.id.lineChart1);
        lineChart2 = view.findViewById(R.id.lineChart2);
        setupLineCharts();

        // Đăng ký nhận dữ liệu từ MQTT (chỉ cần thực hiện một lần)
        mqttHandler.subscribe("0000503341_csdl_receive");

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

                                // Xóa dữ liệu cũ nếu vượt quá 50 điểm
                                if (dataSet1.getEntryCount() > 50) {
                                    dataSet1.removeFirst();
                                }
                                if (dataSet2.getEntryCount() > 50) {
                                    dataSet2.removeFirst();
                                }

                                // Thêm dữ liệu vào biểu đồ
                                dataSet1.addEntry(new Entry(timeIndex, heartRate));
                                dataSet2.addEntry(new Entry(timeIndex, oxygenLevel));

                                // Cập nhật biểu đồ (tối ưu hiệu suất)
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
                // Xử lý mất kết nối nếu cần
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Không disconnect MQTT để tránh mất dữ liệu khi quay lại Fragment
    }
}
