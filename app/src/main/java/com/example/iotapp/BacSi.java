package com.example.iotapp;

public class BacSi {
    private String maBS;
    private String tenBS;

    public BacSi(String maBS, String tenBS) {
        this.maBS = maBS;
        this.tenBS = tenBS;
    }

    public String getMaBS() {
        return maBS;
    }

    public String getTenBS() {
        return tenBS;
    }

    @Override
    public String toString() {
        return tenBS; // để Spinner hiển thị tên
    }
}

