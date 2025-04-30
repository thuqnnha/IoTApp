package com.example.iotapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.app.DatePickerDialog;

import android.database.SQLException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;

public class RegisterFragment extends Fragment {
    private Spinner spinnerKhoa, spinnerBacSi,spinnerTime;
    private Button  btnRegister;
    private ArrayAdapter<Khoa> adapterKhoa;
    private ArrayAdapter<String> adapterBacSi;
    private ArrayAdapter<String> adapterTime;
    private ArrayList<Khoa> listKhoa;
    private String selectedDate = "";
    private String selectedTime = "";
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int NOTIFICATION_PERMISSION_CODE = 1001;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        createNotificationChannel();  // Tạo notification channel
        checkAndRequestNotificationPermission();  // Kiểm tra quyền nếu Android 13+
        // Tìm Button chọn ngày
        Button btnPickDate = view.findViewById(R.id.btnPickDate);
        //btnPickDate.setText("Ngày: --/--/----"); // Giá trị mặc định
        // Bắt sự kiện khi nhấn vào nút
        //scheduleReminder("2025-04-30", "14:50", 101);
        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Hiển thị hộp thoại DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                         selectedDate = String.format("%d-%02d-%02d", selectedYear , selectedMonth + 1, selectedDay);
                        btnPickDate.setText("Ngày: " + selectedDate);
                        // Sau khi chọn ngày xong → enable spinnerTime
                        spinnerTime.setEnabled(true);
                        //
                        BacSi selectedBacSi = (BacSi) spinnerBacSi.getSelectedItem();
                        if (selectedBacSi != null) {
                            loadAvailableTimeSlots(selectedBacSi.getMaBS(), selectedDate);
                        }
                        //
                    },
                    year, month, day
            );

            datePickerDialog.show();
        });
        // Khởi tạo Spinner
        spinnerTime = view.findViewById(R.id.spinnerTime);
        spinnerKhoa = view.findViewById(R.id.spinnerKhoa);
        spinnerBacSi = view.findViewById(R.id.spinnerBacSi);
        btnRegister = view.findViewById(R.id.btnRegister);
        //load spinner khoa
        loadKhoaFromDatabase();
        // Thiết lập Spinner Time từ string.xml
        setupTimeSpinner();

        // Thiết lập listener cho btnRegister để thực hiện insert
        btnRegister.setOnClickListener(v -> {
            Khoa selectedKhoa = (Khoa) spinnerKhoa.getSelectedItem();
            //String maKhoa = selectedKhoa.getMaKhoa();
            BacSi selectedBacSi = (BacSi) spinnerBacSi.getSelectedItem();
            String maBS = selectedBacSi.getMaBS(); // để insert vào lịch hẹn
            String maBN = "0019BN";  // ID bệnh nhân cố định
            String gio = selectedTime;
            String trangThai = "false"; // Trang thái cố định là 'false'

            insertLichHen(maBS, maBN, selectedDate, selectedTime, trangThai);

            // Đặt lịch thông báo trước 30 phút
            scheduleReminder(selectedDate, selectedTime, 101);
        });
        return view;
    }
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }
    // Gửi kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Đã cấp quyền gửi thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Không thể gửi thông báo nếu không có quyền", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "ReminderChannel";
            String description = "Kênh thông báo nhắc lịch hẹn";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("reminder_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void loadKhoaFromDatabase() {
        executorService.execute(() -> {
            listKhoa = DatabaseHelper.getListKhoa();

            mainHandler.post(() -> {
                adapterKhoa = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, listKhoa);
                adapterKhoa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerKhoa.setAdapter(adapterKhoa);

                // Thiết lập sự kiện khi chọn Khoa
                spinnerKhoa.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Khoa selectedKhoa = listKhoa.get(position);
                        loadBacSiFromDatabase(selectedKhoa.getMaKhoa());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });
            });
        });
    }

    private void loadBacSiFromDatabase(String maKhoa) {
        executorService.execute(() -> {
            ArrayList<BacSi> listBacSi = DatabaseHelper.getListBacSiByMaKhoa(maKhoa);

            mainHandler.post(() -> {
                ArrayAdapter<BacSi> adapterBacSi = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        listBacSi
                );
                adapterBacSi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerBacSi.setAdapter(adapterBacSi);
            });
        });
    }

    private void setupTimeSpinner() {
        // Lấy giá trị giờ từ string.xml
        String[] timeArray = getResources().getStringArray(R.array.time_slots); // Ví dụ: <string-array name="time_slots">...</string-array>
        adapterTime = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, timeArray);
        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(adapterTime);

        spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTime = (String) spinnerTime.getSelectedItem();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        //disable chọn giờ
        spinnerTime.setEnabled(false); // disable ban đầu
    }

    private void insertLichHen(String maBS, String maBN, String ngay, String gio, String trangThai) {
        executorService.execute(() -> {
            String query = "INSERT INTO lichhen (maBS, maBN, ngay, gio, trangThai) VALUES ('" +
                    maBS + "', '" + maBN + "', '" + ngay + "', '" + gio + "', '" + trangThai + "')";
            try (Connection conn = DatabaseHelper.connect()) {
                if (conn != null) {
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate(query);
                    mainHandler.post(() -> Toast.makeText(getContext(), "Lịch hẹn đã được thêm", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException | java.sql.SQLException e) {
                mainHandler.post(() -> Toast.makeText(getContext(), "Lỗi khi thêm lịch hẹn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    private void loadAvailableTimeSlots(String maBS, String ngay) {
        executorService.execute(() -> {
            ArrayList<String> usedSlots = DatabaseHelper.getUsedTimeSlots(maBS, ngay);
            String[] allSlots = getResources().getStringArray(R.array.time_slots);
            ArrayList<String> availableSlots = new ArrayList<>();

            for (String slot : allSlots) {
                if (!usedSlots.contains(slot)) {
                    availableSlots.add(slot);
                }
            }

            mainHandler.post(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        availableSlots
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTime.setAdapter(adapter);
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Đừng quên tắt khi fragment bị destroy
    }
    private void scheduleReminder(String ngay, String khungGio, int requestCode) {
        String[] timeParts = khungGio.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = sdf.parse(ngay);
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // Trừ 30 phút
            calendar.add(Calendar.MINUTE, -30);//thoi gian

            Intent intent = new Intent(requireContext(), ReminderReceiver.class);
            intent.putExtra("info", "Bạn có lịch hẹn lúc " + khungGio);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
