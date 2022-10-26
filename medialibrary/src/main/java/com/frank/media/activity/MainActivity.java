package com.frank.media.activity;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import com.frank.media.R;
import com.frank.media.viewcontroller.PlayerViewController;

public class MainActivity extends Activity {

    private final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private PlayerViewController viewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 123);
        }

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        viewController = new PlayerViewController(this);
        viewController.initView(getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewController.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewController.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewController.onStop();
    }

}