package com.example.usbtether;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class UsbTetherService extends AccessibilityService {

    public static final String ACTION_PERFORM = "com.example.usbtether.ACTION_PERFORM";
    private static final String TETHERING_SETTINGS = "android.settings.TETHERING_SETTINGS";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int attempt = 0;
    private BroadcastReceiver receiver;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        IntentFilter filter = new IntentFilter(ACTION_PERFORM);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                attempt = 0;
                openTetherSettings();
            }
        };
        registerReceiver(receiver, filter);
    }

    private void openTetherSettings() {
        try {
            Intent i = new Intent(TETHERING_SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
        handler.postDelayed(this::tryFindAndClick, 1500);
    }

    private void tryFindAndClick() {
        if (attempt >= 6) {
            toast("未找到 USB 共享网络开关，请手动开启");
            attempt = 0;
            return;
        }
        attempt++;
        boolean done = findAndClickToggle();
        if (!done) {
            handler.postDelayed(this::tryFindAndClick, 1200);
        } else {
            toast("已操作 USB 共享网络开关");
            attempt = 0;
        }
    }

    private boolean findAndClickToggle() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo node = findNodeByText(root);
        if (node != null) {
            if (!isRowChecked(node)) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            node.recycle();
            root.recycle();
            return true;
        }

        boolean scrolled = scrollForward(root);
        root.recycle();
        return false;
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node) {
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (text != null && isUsbTetherText(text.toString())) return node;
        if (desc != null && isUsbTetherText(desc.toString())) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByText(child);
                if (result != null) return result;
                child.recycle();
            }
        }
        return null;
    }

    private boolean isRowChecked(AccessibilityNodeInfo node) {
        if (node.isCheckable() && node.isChecked()) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (isRowChecked(child)) return true;
                child.recycle();
            }
        }
        return false;
    }

    private boolean scrollForward(AccessibilityNodeInfo node) {
        if (node.isScrollable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (scrollForward(child)) return true;
                child.recycle();
            }
        }
        return false;
    }

    private boolean isUsbTetherText(String s) {
        return s.contains("USB共享网络") || s.contains("USB网络共享")
                || s.contains("USB网络分享") || s.contains("USB tethering")
                || s.contains("USB网络") || s.contains("USB共享");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (receiver != null) unregisterReceiver(receiver);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}