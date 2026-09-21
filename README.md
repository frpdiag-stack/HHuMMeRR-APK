<!-- Tech Stack Badges -->
<p align="center">
  <img src="https://img.shields.io/badge/ANDROID-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/JAVA-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/GRADLE-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle">
  <img src="https://img.shields.io/badge/ANDROID_STUDIO-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white" alt="Android Studio">
</p>

<br>

<!-- Logo / Judul -->
<h1 align="center">🤖 belajar1</h1>

<p align="center"><strong>Aplikasi Android Belajar — Project Pemula dengan Java & Gradle Kotlin DSL</strong></p>

<!-- Info Badges -->
<p align="center">
  <img src="https://img.shields.io/badge/Version-1.0.0-blue" alt="Version">
  <img src="https://img.shields.io/badge/Status-Development-orange" alt="Status">
  <img src="https://img.shields.io/badge/Android-13.0_(API_33)-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Java-1.8-ED8B00?logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Min_SDK-21-red" alt="Min SDK">
</p>

---

## 📋 Tentang Project

**belajar1** adalah aplikasi Android sederhana berbasis **Java** yang dibuat sebagai project pembelajaran. Aplikasi ini menggunakan template *Empty Views Activity* dari Android Studio dengan satu `MainActivity` sebagai layar utama. Project ini cocok sebagai titik awal untuk belajar struktur project Android, layout XML, dan build system Gradle.

---

## ✨ Fitur Utama

| Fitur | Deskripsi |
| --- | --- |
| 🚀 **Splash ke MainActivity** | Satu activity utama yang langsung tampil saat aplikasi dibuka |
| 🎨 **Layout XML** | Tampilan antarmuka didefinisikan lewat `activity_main.xml` (ConstraintLayout) |
| 🌙 **Tema Day/Night** | Dukungan tema terang & gelap via `values/themes.xml` dan `values-night/themes.xml` |
| 🔧 **Ikon Launcher Adaptif** | Ikon aplikasi adaptif untuk berbagai kepadatan layar (hdpi–xxxhdpi) |
| ✅ **Testing Dasar** | Template unit test (JUnit) & instrumented test (Espresso) |
| 🔐 **Aturan Backup** | Konfigurasi backup & data extraction via `xml/backup_rules.xml` |

---

## 🛠️ Tech Stack

| Layer | Teknologi |
| --- | --- |
| **Platform** | Android |
| **Language** | Java (source & target compatibility 1.8) |
| **Build System** | Gradle (Kotlin DSL / `.gradle.kts`) |
| **Android Gradle Plugin** | 8.1.1 |
| **UI** | XML Layout + AppCompat + Material Components |
| **Layout** | ConstraintLayout 2.1.4 |
| **Testing** | JUnit 4.13.2, Espresso 3.5.1 |
| **Min SDK** | 21 (Android 5.0 Lollipop) |
| **Target / Compile SDK** | 33 (Android 13) |

---

## 📂 Struktur File (Sesuai Repository)

```
belajar1/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/belajar1/
│   │   │   │   └── MainActivity.java       # Activity utama aplikasi
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml   # Layout layar utama
│   │   │   │   ├── values/                 # Warna, string, tema (mode terang)
│   │   │   │   ├── values-night/           # Tema mode gelap
│   │   │   │   ├── drawable/               # Ikon & background
│   │   │   │   ├── mipmap-*/               # Ikon launcher semua kepadatan layar
│   │   │   │   └── xml/                    # Aturan backup & data extraction
│   │   │   └── AndroidManifest.xml         # Manifest aplikasi
│   │   ├── test/                           # Unit test lokal (JUnit)
│   │   └── androidTest/                    # Instrumented test (Espresso)
│   ├── build.gradle.kts                    # Konfigurasi module app
│   └── proguard-rules.pro                  # Aturan ProGuard/R8
├── gradle/
│   └── wrapper/                            # Gradle Wrapper
├── build.gradle.kts                        # Konfigurasi level project (AGP 8.1.1)
├── settings.gradle.kts                     # Nama project & repository
├── gradle.properties                       # Properti build (JVM heap, AndroidX, dll)
├── gradlew / gradlew.bat                   # Script Gradle Wrapper
├── .gitignore
└── README.md
```

---

## 🗄️ Konfigurasi Build (Sesuai app/build.gradle.kts)

| Konfigurasi | Nilai |
| --- | --- |
| `namespace` | `com.example.belajar1` |
| `applicationId` | `com.example.belajar1` |
| `versionCode` / `versionName` | `1` / `1.0` |
| `minSdk` | `21` |
| `targetSdk` / `compileSdk` | `33` |
| `sourceCompatibility` | Java 1.8 |
| `isMinifyEnabled` | `false` (release) |

### Dependency Utama

| Library | Versi |
| --- | --- |
| `androidx.appcompat:appcompat` | 1.6.1 |
| `com.google.android.material:material` | 1.9.0 |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 |
| `junit:junit` (test) | 4.13.2 |
| `androidx.test.ext:junit` (androidTest) | 1.1.5 |
| `androidx.test.espresso:espresso-core` (androidTest) | 3.5.1 |

---

## ⚙️ Alur Kerja Aplikasi

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  📱 Buka App    │────▶│ AndroidManifest  │────▶│  🚀 MainActivity │
│  (Launcher)     │     │ (LAUNCHER intent)│     │   .onCreate()   │
└─────────────────┘     └──────────────────┘     └───────┬─────────┘
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │ setContentView()     │
                                              │ activity_main.xml    │
                                              └───────┬─────────────┘
                                                      │
                                                      ▼
                                              ┌─────────────────────┐
                                              │  📺 Tampil Layout    │
                                              │  "Hello World"       │
                                              └─────────────────────┘
```

---

## 📝 Cara Menjalankan

### 1. Clone Repository

```bash
git clone https://github.com/Firzyafriyan/belajar1.git
cd belajar1
```

### 2. Buka di Android Studio

- Pilih **File → Open**, lalu arahkan ke folder project
- Tunggu Gradle Sync selesai

### 3. Jalankan via Gradle Wrapper

```bash
# Build debug APK
./gradlew assembleDebug        # Linux/macOS
gradlew.bat assembleDebug      # Windows

# Install ke emulator/perangkat yang terhubung
./gradlew installDebug

# Jalankan semua unit test
./gradlew test
```

### 4. Jalankan via Android Studio

- Hubungkan perangkat / nyalakan emulator
- Klik tombol **Run ▶** (Shift+F10)

---

## 🎯 Ide Pengembangan Lanjutan

- [ ] Ubah bahasa ke **Kotlin**
- [ ] Tambah activity kedua & navigasi antar layar
- [ ] Ganti "Hello World" dengan UI interaktif (EditText, Button, RecyclerView)
- [ ] Terapkan **ViewBinding** untuk akses view yang lebih aman
- [ ] Aktifkan **minifyEnabled** + R8 untuk APK yang lebih kecil
- [ ] Tambah GitHub Actions untuk CI (build & test otomatis)

---

## 🤝 Kontribusi

Kontribusi sangat diterima! Silakan:

1. Fork repository ini
2. Buat branch fitur ( `git checkout -b fitur-anda`)
3. Commit perubahan ( `git commit -m 'Tambah fitur X'`)
4. Push ke branch ( `git push origin fitur-anda`)
5. Buat Pull Request

---

## 📄 Lisensi

Project ini dilisensikan di bawah MIT License.

---
