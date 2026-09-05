# 📜 Privacy Policy – Call Limiter & Reminder

_Last updated: August 30, 2026_

Thank you for using **Call Limiter & Reminder**. Your privacy is our highest priority. This document outlines our privacy commitments and how the application handles permissions and local data.

---

## 📱 What Does the App Do?

**Call Limiter & Reminder** helps users manage and limit call durations, set periodic reminder beeps/vibrations, and control call times effectively:

- Setting custom time limits (in hours, minutes, and seconds) for specific contacts or all outgoing calls  
- Periodic audio tone and vibration reminder alerts during calls  
- Optional automatic call disconnection once the duration limit is reached  
- Emergency and call start buffer configurations  
- Daily and per-call limit resetting options  

---

## 🔐 Zero Data Collection & 100% Offline

We do **not** collect, store, transmit, or share any personal data, phone numbers, call records, or audio content.

- The application contains **zero internet permissions** (`android.permission.INTERNET` is not requested).
- The app operates **100% locally and offline** on your device.
- No analytics, tracking SDKs, advertisements, or external servers are used.

### Android Permissions & Usage:
All requested permissions are strictly required for local call management on your device:
- **Phone State & Call Handling** (`READ_PHONE_STATE`, `CALL_PHONE`, `ANSWER_PHONE_CALLS`): To detect call start/end and automatically disconnect when limits expire.
- **Call Logs & Contacts** (`READ_CALL_LOG`, `READ_CONTACTS`): To allow selecting contacts from your address book to apply limits.
- **Notifications & Background Services** (`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`): To display active call timer notifications and maintain accurate timers in the background.
- **Vibration** (`VIBRATE`): To deliver gentle haptic alerts at selected reminder intervals.
- **Boot** (`RECEIVE_BOOT_COMPLETED`): To restore user limits after restarting your phone.

---

## ⚠️ Liability Disclaimer

While **Call Limiter & Reminder** is designed to assist with personal call duration management, **you use this utility tool at your own discretion**:
- It should not replace emergency protocols or critical communications.
- The developers are not liable for unintended disconnects during critical situations.

---

## 📩 Contact & Open-Source

**Call Limiter & Reminder** is free and open-source software under the **GNU General Public License v3.0 (GPLv3)**.

- **Developer & Maintainer:** Mohamed Amine Louati
- **Email:** [mohamedaminlouati@gmail.com](mailto:mohamedaminlouati@gmail.com)
- **GitHub Repository:** [https://github.com/mohamedaminelouati/Call-Limiter-Reminder](https://github.com/mohamedaminelouati/Call-Limiter-Reminder)
- **LinkedIn:** [https://www.linkedin.com/in/mohamed-amine-louati-a383a367](https://www.linkedin.com/in/mohamed-amine-louati-a383a367)
