package com.example.iotapp;

public class Khoa {
    private String maKhoa;
    private String tenKhoa;

    public Khoa(String maKhoa, String tenKhoa) {
        this.maKhoa = maKhoa;
        this.tenKhoa = tenKhoa;
    }

    public String getMaKhoa() {
        return maKhoa;
    }

    public String getTenKhoa() {
        return tenKhoa;
    }

    @Override
    public String toString() {
        return tenKhoa; // rất quan trọng để hiển thị đúng trong Spinner
    }
}


