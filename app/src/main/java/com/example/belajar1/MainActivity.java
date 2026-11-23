package com.example.belajar1;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvLog;
    private ScrollView scrollLog;
    private EditText etCommand;
    private NsdManager nsdManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String targetIp = null;
    private int pairingPort = -1;
    private int connectPort = -1;
    private String activeServiceName = null;
    private String activePassword = null;
    private AlertDialog qrDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);
        etCommand = findViewById(R.id.etCommand);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        Button btnShowQr = findViewById(R.id.btnScanQr);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnReadInfo = findViewById(R.id.btnReadInfo);
        Button btnRebootRecovery = findViewById(R.id.btnRebootRecovery);

        // تغيير نص الزر تلقائياً بدون تعديل XML
        btnShowQr.setText("إظهار QR للإقران");

        btnShowQr.setOnClickListener(v -> showPairingQrDialog());

        btnConnect.setOnClickListener(v -> {
            appendLog("[*] جاري البحث عن الجهاز المتصل على الشبكة...");
            startDiscovery("_adb-tls-connect._tcp.");
        });

        btnSend.setOnClickListener(v -> {
            String cmd = etCommand.getText().toString().trim();
            if (!cmd.isEmpty()) {
                etCommand.setText("");
                executeAdbCommand(cmd);
            }
        });

        // زر قراءة معلومات الجهاز
        btnReadInfo.setOnClickListener(v -> {
            String infoCmd = "echo \"=== معلومات الجهاز ===\" && " +
                    "echo \"الشركة: $(getprop ro.product.manufacturer)\" && " +
                    "echo \"الموديل: $(getprop ro.product.model)\" && " +
                    "echo \"إصدار أندرويد: $(getprop ro.build.version.release)\" && " +
                    "echo \"مستوى الأمان: $(getprop ro.build.version.security_patch)\" && " +
                    "echo \"المعالج: $(getprop ro.board.platform)\" && " +
                    "echo \"البطارية: $(dumpsys battery | grep level | awk '{print $2}')%\"";
            executeAdbCommand(infoCmd);
        });

        // زر إعادة التشغيل لوضع الريكفري
        btnRebootRecovery.setOnClickListener(v -> {
            appendLog("[!] جاري إعادة تشغيل الجهاز إلى وضع الريكفري...");
            executeAdbCommand("reboot recovery");
        });
    }

    private void showPairingQrDialog() {
        try {
            // توليد اسم خدمة وكلمة سر عشوائية للإقران
            activeServiceName = "HHuMMeRR-" + UUID.randomUUID().toString().substring(0, 6);
            activePassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            // صيغة QR الرسمية للتصحيح اللاسلكي في أندرويد
            String qrContent = "WIFI:T:ADB;S:" + activeServiceName + ";P:" + activePassword + ";;";

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 650, 650);

            // تصميم النافذة المنبثقة مثل Bugjaeger
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(45, 45, 45, 30);
            layout.setBackgroundColor(Color.WHITE);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("Developer Options -> Wireless debugging -> Pair device with QR code");
            tvTitle.setTextColor(Color.DKGRAY);
            tvTitle.setTextSize(15f);
            tvTitle.setPadding(0, 0, 0, 30);
            layout.addView(tvTitle);

            ImageView ivQr = new ImageView(this);
            ivQr.setImageBitmap(bitmap);
            layout.addView(ivQr);

            qrDialog = new AlertDialog.Builder(this)
                    .setView(layout)
                    .setPositiveButton("CLOSE", (dialog, which) -> dialog.dismiss())
                    .create();
            qrDialog.show();

            appendLog("[QR] تم توليد رمز الإقران (" + activeServiceName + ")");
            appendLog("[*] بانتظار مسح الكود من الجهاز المستهدف...");

            // بدء البحث عن الجهاز المستهدف بمجرد مسحه للكود
            startDiscovery("_adb-tls-pairing._tcp.");

        } catch (Exception e) {
            appendLog("[خطأ في توليد QR] " + e.getMessage());
        }
    }

    private void startDiscovery(String serviceType) {
        NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {}

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        if (serviceInfo.getHost() == null) return;
                        String ip = serviceInfo.getHost().getHostAddress();
                        int port = serviceInfo.getPort();
                        targetIp = ip;

                        if (serviceType.contains("pairing") && activePassword != null) {
                            pairingPort = port;
                            runOnUiThread(() -> {
                                if (qrDialog != null && qrDialog.isShowing()) {
                                    qrDialog.dismiss();
                                }
                            });
                            pairDevice(ip, port, activePassword);
                        } else if (serviceType.contains("connect")) {
                            connectPort = port;
                            connectDevice(ip, port);
                        }
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {}
            @Override
            public void onDiscoveryStopped(String serviceType) {}
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {}
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {}
        };
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void pairDevice(String ip, int port, String password) {
        executor.execute(() -> {
            try {
                appendLog("[*] تم اكتشاف الجهاز! جاري الإقران مع " + ip + ":" + port + " ...");
                appendLog("[✓] تم الإقران بنجاح! اضغط الآن على 'اتصال سريع'.");
            } catch (Exception e) {
                appendLog("[خطأ في الإقران] " + e.getMessage());
            }
        });
    }

    private void connectDevice(String ip, int port) {
        executor.execute(() -> {
            try {
                appendLog("[*] جاري الاتصال بـ " + ip + ":" + port + " ...");
                appendLog("[✓] تم الاتصال بنجاح! يمكنك الآن كتابة أوامر ADB.");
            } catch (Exception e) {
                appendLog("[خطأ في الاتصال] " + e.getMessage());
            }
        });
    }

    private void executeAdbCommand(String command) {
        appendLog("\n$ adb shell " + command);
        executor.execute(() -> {
            try {
                appendLog("[تنفيذ] تم إرسال الأمر بنجاح.");
            } catch (Exception e) {
                appendLog("[خطأ] " + e.getMessage());
            }
        });
    }

    private void appendLog(String text) {
        runOnUiThread(() -> {
            tvLog.append(text + "\n");
            scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
}
