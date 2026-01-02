# Encrypto — End-to-End Encrypted Chat Application

An end-to-end encrypted, real-time Android messaging app. Encrypto encrypts messages on the sender device using AES-256-GCM and PBKDF2-derived keys, sends only ciphertext to the server (Supabase), and decrypts messages only on the recipient device — implementing a zero-knowledge architecture.


---

##  Key Features

- End-to-end encrypted messaging
  - AES-256-GCM for authenticated encryption
  - PBKDF2-derived keys with unique salt per user
  - Unique IV + salt per message
  - Plaintext never leaves the device
- Real-time messaging
  - WebSocket-based realtime (Supabase Realtime)
  - Sub-200ms delivery latency (typical, per project tests)
- Status / Story feature (24-hour expiration)
  - Secure image upload to storage buckets
  - Automatic deletion via scheduled server tasks
- Modern Android client (MVVM)
  - LiveData, ViewModel, Repository layers
  - Material UI, optimized RecyclerViews & DiffUtil
- Supabase backend
  - PostgreSQL with Row Level Security (RLS)
  - Realtime WAL-based streaming
  - JWT authentication and secure media storage
- Contact sync & profile
  - Local contact matching
  - Profile photo, display name, about text

---

## Security Architecture

- True client-side encryption/decryption — server stores only ciphertext.
- AES-256-GCM with authentication tag to prevent tampering.
- Per-message IV and per-user salt to reduce reuse risks and assist forward-secrecy-like behavior.
- Encrypted SharedPreferences for storing tokens and local secrets.
- Minimal logging and no plaintext persisted on server.
  

---

## System Overview

### Client (Android)
- Language: Java (100% of repo)
- Architecture: MVVM (Activities/Fragments, ViewModel, Repository)
- Libraries: Retrofit, OkHttp (WebSocket), Glide, AndroidX, EncryptedSharedPreferences
- Responsibilities: key derivation, message encryption/decryption, UI, local caching

### Server (Supabase)
- PostgreSQL with RLS policies
- Realtime message broadcasting via WAL streaming
- Storage buckets for media with access rules
- Automated expiry (pg_cron or similar) for status/story deletion
- Stores only ciphertext and metadata required for routing/delivery

### Communication Flow (high level)
1. User composes message in the app.
2. Message is encrypted locally (AES-256-GCM) → ciphertext + metadata.
3. Ciphertext is sent to Supabase via Realtime API.
4. Supabase stores ciphertext and broadcasts event.
5. Recipient receives event and decrypts locally.


---

## Project Management

- Hybrid Waterfall + Agile (sprint-based) approach
- 14-week timeline with sprint breakdowns:
  - Authentication & profiles
  - Encryption & key management
  - Realtime messaging
  - Status/story & media handling
  - Testing

---

## Future Enhancements

Planned improvements:
- Group chats and group key management
- Read receipts & typing indicators
- Multi-device key sync / secure backups
- Push notifications with encrypted payloads
- Voice & video messages
- Further metadata minimization and privacy hardening
- Optional decentralized delivery architectures

---

## 🛠️ Technologies Used

- Android (Java, XML) — MVVM, LiveData, ViewModel
- Networking: Retrofit, OkHttp (WebSocket)
- Image loading: Glide
- Backend: Supabase (PostgreSQL, Realtime, Auth, Storage)
- Security: AES-256-GCM, PBKDF2, EncryptedSharedPreferences
- Tools: Android Studio, Git, Draw.io (diagrams)

---

## 🚀 Quick Start (Development)

1. Clone the repository:
   git clone https://github.com/mohamedsadah/PBL-Encrypto.git

2. Open in Android Studio and let Gradle sync.

3. Configuration:
   - If using Supabase: place your `supabase` config / keys in the Constants class (Constants.java)
   - If using Firebase or alternate services, add `google-services.json` or equivalent as required.
   - Ensure any secrets are set via secure build configs or local properties — do not commit secrets.

4. Build & Run:
   - Run from Android Studio or:
     ./gradlew assembleDebug
     ./gradlew installDebug

5. For testing:
   - ./gradlew test
   - ./gradlew connectedAndroidTest

---

## 📄 License

Use this project responsibly.

---

## 👥 Credits & Mentorship

- Author: mohamedsadah
- Mentor: Mr. Utsav Kumar
  > Assistant Professor
  > Department of Computer Applications, Graphic Era Deemed University

---

## 📬 Contact

For questions, issues, or contributions:
- Open an issue on this repository: https://github.com/mohamedsadah/PBL-Encrypto/issues
- GitHub: @mohamedsadah
- whatsapp: +23278901710
- email: ibnaadam806@gmail.com
- LinkedIn: https://www.linkedin.com/in/mohamed-sadah-bah-a178951b4/

---
