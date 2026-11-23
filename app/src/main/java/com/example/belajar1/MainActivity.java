package com.example.belajar1;

import android.app.AlertDialog;
import android.content.Context;
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
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;

public class MainActivity extends AppCompatActivity {

    private TextView tvLog;
    private ScrollView scrollLog;
    private EditText etCommand;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private HHuMMeRRAdbManager adbManager;
    private String lastIp = "192.168.1.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);
        etCommand = findViewById(R.id.etCommand);

        executor.execute(() -> {
            try {
                adbManager = new HHuMMeRRAdbManager(getApplicationContext());
                appendLog("[✓] محرك HHuMMeRR ADB جاهز للعمل.");
            } catch (Exception e) {
                appendLog("[خطأ في تهيئة المحرك] " + e.getMessage());
            }
        });

        Button btnPair = findViewById(R.id.btnScanQr);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnReadInfo = findViewById(R.id.btnReadInfo);
        Button btnRebootRecovery = findViewById(R.id.btnRebootRecovery);

        btnPair.setText("إقران يدوي (Pair)");
        btnConnect.setText("اتصال يدوي (Connect)");

        btnPair.setOnClickListener(v -> showManualPairDialog());
        btnConnect.setOnClickListener(v -> showManualConnectDialog());

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

    private void showManualPairDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        EditText etIp = new EditText(this);
        etIp.setHint("عنوان IP (مثال: 192.168.1.106)");
        etIp.setText(lastIp);
        layout.addView(etIp);

        EditText etPort = new EditText(this);
        etPort.setHint("منفذ الإقران Port (مثال: 36181)");
        etPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPort);

        EditText etCode = new EditText(this);
        etCode.setHint("كود الإقران (6 أرقام)");
        etCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etCode);

        new AlertDialog.Builder(this)
                .setTitle("إقران يدوي (Pair with code)")
                .setView(layout)
                .setPositiveButton("إقران", (dialog, which) -> {
                    String ip = etIp.getText().toString().trim();
                    String portStr = etPort.getText().toString().trim();
                    String code = etCode.getText().toString().trim();
                    if (!ip.isEmpty() && !portStr.isEmpty() && !code.isEmpty()) {
                        lastIp = ip;
                        pairDevice(ip, Integer.parseInt(portStr), code);
                    } else {
                        appendLog("[!] يرجى تعبئة جميع الحقول.");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showManualConnectDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        EditText etIp = new EditText(this);
        etIp.setHint("عنوان IP (مثال: 192.168.1.106)");
        etIp.setText(lastIp);
        layout.addView(etIp);

        EditText etPort = new EditText(this);
        etPort.setHint("منفذ الاتصال Port (مثال: 43831)");
        etPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPort);

        new AlertDialog.Builder(this)
                .setTitle("اتصال يدوي (Connect)")
                .setView(layout)
                .setPositiveButton("اتصال", (dialog, which) -> {
                    String ip = etIp.getText().toString().trim();
                    String portStr = etPort.getText().toString().trim();
                    if (!ip.isEmpty() && !portStr.isEmpty()) {
                        lastIp = ip;
                        connectDevice(ip, Integer.parseInt(portStr));
                    } else {
                        appendLog("[!] يرجى إدخال الـ IP والمنفذ.");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void pairDevice(String ip, int port, String password) {
        executor.execute(() -> {
            try {
                appendLog("[*] جاري الإقران مع " + ip + ":" + port + " ...");
                boolean paired = adbManager.pair(ip, port, password);
                if (paired) {
                    appendLog("[✓] تم الإقران بنجاح! اضغط الآن على 'اتصال يدوي (Connect)'.");
                } else {
                    appendLog("[✗] فشل الإقران، تأكد من الكود والمنفذ.");
                }
            } catch (Exception e) {
                appendLog("[خطأ في الإقران] " + e.getMessage());
            }
        });
    }

    private void connectDevice(String ip, int port) {
        executor.execute(() -> {
            try {
                appendLog("[*] جاري الاتصال بـ " + ip + ":" + port + " ...");
                boolean connected = adbManager.connect(ip, port);
                if (connected) {
                    appendLog("[✓] تم الاتصال بنجاح! يمكنك الآن استخدام Read Info أو كتابة الأوامر.");
                } else {
                    appendLog("[✗] فشل الاتصال، تأكد من إقران الجهاز أولاً ومنفذ الاتصال الصحيح.");
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
                    appendLog("[!] غير متصل بأي جهاز. اضغط 'اتصال يدوي' أولاً.");
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
