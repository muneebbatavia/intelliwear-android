# IntelliWear

**IntelliWear** is an Android-based Final Year Project (FYP) focused on health monitoring and early breast cancer risk awareness. The application integrates user questionnaires, AI-based analysis, real-time sensor readings, Bluetooth communication, and an admin approval system.

---

## Key Features

### Authentication (User & Admin)
- User **Sign Up / Login** system.
- Separate **Admin login**.
- Admin can **approve or decline users** before granting full access to the app.

---

### Health Questionnaires
- The application contains **three structured questionnaires**.
- Users must complete all questionnaires to proceed.
- Questionnaire data is stored securely in **Firebase Firestore**.

---

### AI-Based Health Analysis
- User questionnaire responses are processed to generate an **AI-based health assessment**.
- The system provides:
  - Breast cancer risk indication
  - Recommendation on whether to consult a doctor
  - Lifestyle and precautionary guidance

---

### Sensor Readings & Bluetooth Connection
- Bluetooth connectivity using **HC-05 module**.
- Real-time sensor data received from Arduino:
  - **BMP180** – Temperature & Pressure
  - **HX711 Load Cell** – Weight / pressure measurement
- Live sensor readings displayed in a custom Android UI.
- Stores the **last 10 readings** in Firebase Firestore (oldest data overwritten).
- Calculates and displays **average temperature and pressure** values.

---

## Tech Stack
- **Android Studio**
- **Java** & **XML**
- **Firebase Firestore**
- **Arduino Uno**
- **Bluetooth HC-05**
- **Sensors:** BMP180, HX711

---

## Application Modules
- User Authentication Module
- Admin Approval System
- Questionnaire Module (3 questionnaires)
- AI Health Analysis Module
- Bluetooth Device Discovery & Connection
- Sensor Data Reading & Storage Module

---

## Installation & Setup

1. Clone the repository:
      ```bash
      git clone https://github.com/yourusername/IntelliWear.git
2. Open the project in Android Studio.
3. Sync Gradle files.
4. Add your own Firebase configuration:
      google-services.json (do NOT commit this file)
5. Connect an Android device or emulator.
6. Run the application.

## Author
Muneeb Altaf
Final Year Computer Science Student
