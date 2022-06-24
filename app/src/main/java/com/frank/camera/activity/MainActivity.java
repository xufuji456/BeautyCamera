package com.frank.camera.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.frank.camera.R;
import com.frank.camera.beauty.activity.BeautyFilterActivity;

public class MainActivity extends AppCompatActivity {

    private final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(PERMISSIONS, 54321);
        initView();
    }

    private void initView() {
        Button btnFace = findViewById(R.id.btn_face);
        btnFace.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FaceBeautyActivity.class)));

        Button btnFilter = findViewById(R.id.btn_filter);
        btnFilter.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BeautyFilterActivity.class)));

        Button btnPhoto = findViewById(R.id.btn_photo);
    }

}
