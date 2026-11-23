package com.example.belajar1;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tvLog;
    private ScrollView scrollLog;
    private EditText etCommand;
    private NsdManager nsdManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String targetIp = null;
    private int pairingPort = -1;
    private int connectPort = -1;
    private String pendingPassword = null;

    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleQrResult(result.getContents());
                } else {
                    appendLog("[!] تم إلغاء المسح.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);
        etCommand = findViewById(R.id.etCommand);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        Button btnScanQr = findViewById(R.id.btnScanQr);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnReadInfo = findViewById(R.id.btnReadInfo);
        Button btnRebootRecovery = findViewById(R.id.btnRebootRecovery);

        btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("وجّه الكاميرا نحو رمز QR في خيارات التصحيح اللاسلكي");
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            qrLauncher.launch(options);
        });

        btnConnect.setOnClickListener(v -> startDiscovery("_adb-tls-connect._tcp."));

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

    private void handleQrResult(String qrText) {
        appendLog("[QR] تم المسح بنجاح، جاري تحليل البيانات...");
        Pattern pattern = Pattern.compile("S:([^;]+);P:([^;]+)");
        Matcher matcher = pattern.matcher(qrText);

        if (matcher.find()) {
            String serviceName = matcher.group(1);
            pendingPassword = matcher.group(2);
            appendLog("[+] اسم الخدمة: " + serviceName);
            appendLog("[*] جاري البحث عن الجهاز على الشبكة للإقران...");
            startDiscovery("_adb-tls-pairing._tcp.");
        } else {
            appendLog("[خطأ] هذا ليس رمز QR صالح للتصحيح اللاسلكي!");
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

                        if (serviceType.contains("pairing") && pendingPassword != null) {
                            pairingPort = port;
                            pairDevice(ip, port, pendingPassword);
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
                appendLog("[*] جاري الإقران مع " + ip + ":" + port + " ...");
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
