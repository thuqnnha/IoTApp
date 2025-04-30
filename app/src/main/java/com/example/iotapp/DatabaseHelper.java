package com.example.iotapp;

import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public static Connection connect() {
        Connection conn = null;
        String ip = "171.244.38.118";
        String port = "1433"; // Port mặc định SQL Server
        String dbName = "iot2024"; // Hoặc tên database bạn muốn
        String username = "NCKH_2025";
        String password = "12345678";
        String instance = "SQLEXPRESS";

        // Kích hoạt chính sách thread để cho phép kết nối mạng
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            String connectionUrl = "jdbc:jtds:sqlserver://" + ip + ":" + port + "/" + dbName + ";instance=" + instance + ";user=" + username + ";password=" + password + ";";
            conn = DriverManager.getConnection(connectionUrl);
            Log.i("DBConnection", "Kết nối thành công!");
        } catch (SQLException se) {
            Log.e("DBConnection", "Lỗi kết nối: " + se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("DBConnection", "Không tìm thấy driver JDBC: " + e.getMessage());
        }
        return conn;
    }
    public static ArrayList<Khoa> getListKhoa() {
        ArrayList<Khoa> list = new ArrayList<>();
        Connection conn;

        try {
            conn = connect();
            if (conn != null) {
                String query = "SELECT maKhoa, tenKhoa FROM khoa";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    list.add(new Khoa(
                            rs.getString("maKhoa"),
                            rs.getString("tenKhoa")));
                }

                rs.close();
                stmt.close();
                conn.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getListKhoa Error: " + e.getMessage());
        }

        return list;
    }

    public static ArrayList<BacSi> getListBacSiByMaKhoa(String maKhoa) {
        ArrayList<BacSi> list = new ArrayList<>();
        Connection conn;

        try {
            conn = connect();
            if (conn != null) {
                String query = "SELECT maBS, tenBS FROM bacsi WHERE maKhoa = '" + maKhoa + "'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    list.add(new BacSi(
                            rs.getString("maBS"),
                            rs.getString("tenBS")
                    ));
                }

                rs.close();
                stmt.close();
                conn.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getListBacSi Error: " + e.getMessage());
        }

        return list;
    }

    public static ArrayList<String> getUsedTimeSlots(String maBS, String ngay) {
        ArrayList<String> list = new ArrayList<>();
        Connection conn;

        try {
            conn = connect();
            if (conn != null) {
                String query = "SELECT gio FROM lichhen WHERE maBS = '" + maBS + "' AND ngay = '" + ngay + "' AND trangThai = 'false'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    list.add(rs.getString("gio"));
                }

                rs.close();
                stmt.close();
                conn.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getUsedTimeSlots Error: " + e.getMessage());
        }

        return list;
    }

    public static boolean checkLogin(String inputMaDH, String inputPass) {
        Connection conn;
        boolean isValid = false;

        try {
            conn = connect();
            if (conn != null) {
                String query = "SELECT * FROM dongho WHERE maDH = '" + inputMaDH + "' AND pass = '" + inputPass + "'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                if (rs.next()) {
                    // Có ít nhất một kết quả khớp => hợp lệ
                    isValid = true;
                }

                rs.close();
                stmt.close();
                conn.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "checkLogin Error: " + e.getMessage());
        }

        return isValid;
    }



}

