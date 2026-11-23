package com.example.belajar1;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.sun.security.x509.AlgorithmId;
import android.sun.security.x509.CertificateAlgorithmId;
import android.sun.security.x509.CertificateExtensions;
import android.sun.security.x509.CertificateIssuerName;
import android.sun.security.x509.CertificateSerialNumber;
import android.sun.security.x509.CertificateSubjectName;
import android.sun.security.x509.CertificateValidity;
import android.sun.security.x509.CertificateVersion;
import android.sun.security.x509.CertificateX509Key;
import android.sun.security.x509.KeyIdentifier;
import android.sun.security.x509.PrivateKeyUsageExtension;
import android.sun.security.x509.SubjectKeyIdentifierExtension;
import android.sun.security.x509.X500Name;
import android.sun.security.x509.X509CertImpl;
import android.sun.security.x509.X509CertInfo;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;

public class MainActivity extends AppCompatActivity {

    private TextView tvLog;
    private ScrollView scrollLog;
    private EditText etCommand;
    private NsdManager nsdManager;
    private WifiManager.MulticastLock multicastLock;
    private NsdManager.DiscoveryListener currentDiscoveryListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private HHuMMeRRAdbManager adbManager;
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

        // فتح قفل استقبال إشارات الواي فاي (مهم جداً لأجهزة شاومي وسامسونج)
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("HHuMMeRR_mDNS");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        }

        executor.execute(() -> {
            try {
                adbManager = new HHuMMeRRAdbManager(getApplicationContext());
                appendLog("[✓] محرك HHuMMeRR ADB جاهز للعمل.");
            } catch (Exception e) {
                appendLog("[خطأ في تهيئة المحرك] " + e.getMessage());
            }
        });

        Button btnShowQr = findViewById(R.id.btnScanQr);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnReadInfo = findViewById(R.id.btnReadInfo);
        Button btnRebootRecovery = findViewById(R.id.btnRebootRecovery);

        btnShowQr.setText("إظهار QR للإقران");

        btnShowQr.setOnClickListener(v -> showPairingQrDialog());

        btnConnect.setOnClickListener(v -> {
            appendLog("[*] جاري البحث عن الجهاز المتصل على الشبكة...");
            startDiscovery("_adb-tls-connect._tcp");
        });

        btnSend.setOnClickListener(v -> {
            String cmd = etCommand.getText().toString().trim();
            if (!cmd.isEmpty()) {
                etCommand.setText("");
                executeAdbCommand(cmd, true);
            }
        });

        btnReadInfo.setOnClickListener(v -> {
            appendLog("\n[*] جاري قراءة معلومات الجهاز...");
            String infoCmd = "echo '=== Device Info ===' && " +
                    "echo 'Brand: '$(getprop ro.product.manufacturer) && " +
                    "echo 'Model: '$(getprop ro.product.model) && " +
                    "echo 'Android: '$(getprop ro.build.version.release) && " +
                    "echo 'Security: '$(getprop ro.build.version.security_patch) && " +
                    "echo 'CPU: '$(getprop ro.board.platform) && " +
                    "dumpsys battery | grep level";
            executeAdbCommand(infoCmd, false);
        });

        btnRebootRecovery.setOnClickListener(v -> {
            appendLog("\n[!] جاري إعادة تشغيل الجهاز إلى وضع الريكفري...");
            executeAdbCommand("reboot recovery", false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCurrentDiscovery();
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
    }

    private void showPairingQrDialog() {
        try {
            activeServiceName = "HHuMMeRR-" + UUID.randomUUID().toString().substring(0, 6);
            activePassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            String qrContent = "WIFI:T:ADB;S:" + activeServiceName + ";P:" + activePassword + ";;";

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 650, 650);

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

            startDiscovery("_adb-tls-pairing._tcp");

        } catch (Exception e) {
            appendLog("[خطأ في توليد QR] " + e.getMessage());
        }
    }

    private void stopCurrentDiscovery() {
        if (currentDiscoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(currentDiscoveryListener);
            } catch (Exception ignored) {}
            currentDiscoveryListener = null;
        }
    }

    private void startDiscovery(String serviceType) {
        stopCurrentDiscovery();

        currentDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {}

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        appendLog("[!] فشل قراءة عنوان الجهاز (كود: " + errorCode + ")");
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        if (serviceInfo.getHost() == null) return;
                        String ip = serviceInfo.getHost().getHostAddress();
                        int port = serviceInfo.getPort();

                        if (serviceType.contains("pairing") && activePassword != null) {
                            String pwd = activePassword;
                            activePassword = null;
                            stopCurrentDiscovery();
                            runOnUiThread(() -> {
                                if (qrDialog != null && qrDialog.isShowing()) {
                                    qrDialog.dismiss();
                                }
                            });
                            pairDevice(ip, port, pwd);
                        } else if (serviceType.contains("connect")) {
                            stopCurrentDiscovery();
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
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                appendLog("[!] فشل بدء البحث على الشبكة (كود: " + errorCode + ")");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {}
        };
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, currentDiscoveryListener);
    }

    private void pairDevice(String ip, int port, String password) {
        executor.execute(() -> {
            try {
                appendLog("[*] جاري الإقران الفعلي مع " + ip + ":" + port + " ...");
                boolean paired = adbManager.pair(ip, port, password);
                if (paired) {
                    appendLog("[✓] تم الإقران بنجاح! اضغط الآن على 'اتصال سريع'.");
                } else {
                    appendLog("[✗] فشل الإقران، حاول مرة أخرى.");
                }
            } catch (Exception e) {
                appendLog("[خطأ في الإقران] " + e.getMessage());
            }
        });
    }

    private void connectDevice(String ip, int port) {
        executor.execute(() -> {
            try {
                appendLog("[*] جاري الاتصال الفعلي بـ " + ip + ":" + port + " ...");
                boolean connected = adbManager.connect(ip, port);
                if (connected) {
                    appendLog("[✓] تم الاتصال بنجاح! يمكنك الآن استخدام Read Info أو كتابة الأوامر.");
                } else {
                    appendLog("[✗] فشل الاتصال، تأكد من إقران الجهاز أولاً.");
                }
            } catch (Exception e) {
                appendLog("[خطأ في الاتصال] " + e.getMessage());
            }
        });
    }

    private void executeAdbCommand(String command, boolean showCommand) {
        if (showCommand) {
            appendLog("\n$ adb shell " + command);
        }
        executor.execute(() -> {
            try {
                if (adbManager == null || !adbManager.isConnected()) {
                    appendLog("[!] غير متصل بأي جهاز. اضغط 'اتصال سريع' أولاً.");
                    return;
                }
                try (AdbStream stream = adbManager.openStream("shell:" + command);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(stream.openInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(line);
                    }
                }
            } catch (Exception e) {
                appendLog("[خطأ في التنفيذ] " + e.getMessage());
            }
        });
    }

    private void appendLog(String text) {
        runOnUiThread(() -> {
            tvLog.append(text + "\n");
            scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private static class HHuMMeRRAdbManager extends AbsAdbConnectionManager {
        private final PrivateKey privateKey;
        private final Certificate certificate;

        public HHuMMeRRAdbManager(Context context) throws Exception {
            setApi(Build.VERSION.SDK_INT);
            File keyFile = new File(context.getFilesDir(), "adb_private.key");
            File certFile = new File(context.getFilesDir(), "adb_cert.der");

            if (keyFile.exists() && certFile.exists()) {
                byte[] keyBytes = new byte[(int) keyFile.length()];
                try (FileInputStream fis = new FileInputStream(keyFile)) {
                    fis.read(keyBytes);
                }
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

                try (FileInputStream fis = new FileInputStream(certFile)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    this.certificate = cf.generateCertificate(fis);
                }
            } else {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair keyPair = kpg.generateKeyPair();
                this.privateKey = keyPair.getPrivate();

                String algorithmName = "SHA512withRSA";
                Date notBefore = new Date();
                Date notAfter = new Date(System.currentTimeMillis() + 86400000L * 365 * 10);
                X500Name x500Name = new X500Name("CN=HHuMMeRR");

                CertificateExtensions extensions = new CertificateExtensions();
                extensions.set("SubjectKeyIdentifier", new SubjectKeyIdentifierExtension(
                        new KeyIdentifier(keyPair.getPublic()).getIdentifier()));
                extensions.set("PrivateKeyUsage", new PrivateKeyUsageExtension(notBefore, notAfter));

                X509CertInfo info = new X509CertInfo();
                info.set("version", new CertificateVersion(2));
                info.set("serialNumber", new CertificateSerialNumber(new Random().nextInt() & Integer.MAX_VALUE));
                info.set("algorithmID", new CertificateAlgorithmId(AlgorithmId.get(algorithmName)));
                info.set("subject", new CertificateSubjectName(x500Name));
                info.set("key", new CertificateX509Key(keyPair.getPublic()));
                info.set("validity", new CertificateValidity(notBefore, notAfter));
                info.set("issuer", new CertificateIssuerName(x500Name));
                info.set("extensions", extensions);

                X509CertImpl cert = new X509CertImpl(info);
                cert.sign(this.privateKey, algorithmName);
                this.certificate = cert;

                try (FileOutputStream fos = new FileOutputStream(keyFile)) {
                    fos.write(this.privateKey.getEncoded());
                }
                try (FileOutputStream fos = new FileOutputStream(certFile)) {
                    fos.write(this.certificate.getEncoded());
                }
            }
        }

        @NonNull
        @Override
        protected PrivateKey getPrivateKey() {
            return privateKey;
        }

        @NonNull
        @Override
        protected Certificate getCertificate() {
            return certificate;
        }

        @NonNull
        @Override
        protected String getDeviceName() {
            return "HHuMMeRR adb";
        }
    }
}
