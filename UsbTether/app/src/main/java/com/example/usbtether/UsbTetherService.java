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

    private static final String[] USB_KEYWORDS = {
            "USB共享网络", "USB网络共享", "USB网络分享", "USB tethering", "USB共享"};
    private static final String[] MORE_KEYWORDS = {"更多共享设置", "更多共享"};
    private static final String[] HOTSPOT_KEYWORDS = {"个人热点", "热点与网络共享", "便携式热点", "网络共享"};
    private static final String[] MOBILE_KEYWORDS = {"移动网络", "移动数据"};

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
        if (attempt >= 10) {
            toast("没找到USB开关，请手动开一下");
            attempt = 0;
            return;
        }
        attempt++;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            handler.postDelayed(this::tryFindAndClick, 1000);
            return;
        }

        // 第1步：直接找 USB共享网络 开关
        AccessibilityNodeInfo usb = findNodeByText(root, USB_KEYWORDS);
        if (usb != null) {
            if (!isRowChecked(usb)) {
                usb.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            toast("已操作USB共享网络");
            attempt = 0;
            usb.recycle();
            root.recycle();
            return;
        }

        // 第2步：找"更多共享设置"并点进去
        AccessibilityNodeInfo more = findNodeByText(root, MORE_KEYWORDS);
        if (more != null) {
            more.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            toast("已进入更多共享设置");
            attempt = 0;
            more.recycle();
            root.recycle();
            handler.postDelayed(this::tryFindAndClick, 1500);
            return;
        }

        // 第3步：找"个人热点"并点进去
        AccessibilityNodeInfo hotspot = findNodeByText(root, HOTSPOT_KEYWORDS);
        if (hotspot != null) {
            hotspot.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            toast("已进入个人热点");
            attempt = 0;
            hotspot.recycle();
            root.recycle();
            handler.postDelayed(this::tryFindAndClick, 1500);
            return;
        }

        // 第4步：找"移动网络"并点进去
        AccessibilityNodeInfo mobile = findNodeByText(root, MOBILE_KEYWORDS);
        if (mobile != null) {
            mobile.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            toast("已进入移动网络");
            attempt = 0;
            mobile.recycle();
            root.recycle();
            handler.postDelayed(this::tryFindAndClick, 1500);
            return;
        }

        // 第5步：都找不到就滚动页面再找
        boolean scrolled = scrollForward(root);
        root.recycle();
        handler.postDelayed(this::tryFindAndClick, scrolled ? 1000 : 1200);
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String[] keywords) {
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (text != null && matches(text.toString(), keywords)) return node;
        if (desc != null && matches(desc.toString(), keywords)) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByText(child, keywords);
                if (result != null) return result;
                child.recycle();
            }
        }
        return null;
    }

    private boolean matches(String s, String[] keywords) {
        for (String k : keywords) {
            if (s.contains(k)) return true;
        }
        return false;
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
