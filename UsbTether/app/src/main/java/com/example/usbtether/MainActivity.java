package com.example.usbtether;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnTether = findViewById(R.id.btnTether);
        Button btnAccessibility = findViewById(R.id.btnAccessibility);

        btnTether.setOnClickListener(v -> {
            if (!isServiceEnabled()) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(UsbTetherService.ACTION_PERFORM);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
            Toast.makeText(this, "已执行，请稍候查看设置页面", Toast.LENGTH_SHORT).show();
        });

        btnAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(
                getPackageName() + "/" + UsbTetherService.class.getName());
    }
}