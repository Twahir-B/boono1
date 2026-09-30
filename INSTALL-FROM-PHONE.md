# Install Aether without a PC (phone only)

## Option A — GitHub Actions (recommended, free)

1. On your phone browser, open https://github.com/new  
   - Create a **public** repository named `Aether` (or any name).  
   - Do **not** add README if you will upload the zip.

2. Upload the project:
   - Open the new repo → **Add file** → **Upload files**  
   - Upload everything inside the `Aether` folder (or the zip contents).  
   - Commit.

   Or use the GitHub mobile app to push if you prefer.

3. Build the APK:
   - Open the repo → tab **Actions**  
   - Select workflow **Build APK** → **Run workflow** → **Run**  
   - Wait 3–6 minutes until the green check appears.

4. Download:
   - Open the finished run → **Artifacts** → **aether-debug-apk**  
   - Download the zip, unzip, get `app-debug.apk`.

5. Install on Android 10:
   - Open the APK file  
   - Allow “Install unknown apps” for your browser/Files app  
   - Install.

## Option B — Ask a friend with a PC

Send them `Aether-android-source.zip`. They open it in Android Studio → Build APK → send you `app-debug.apk`.

## After install

1. Open Aether  
2. Enable **Accessibility** for Aether  
3. Allow **Display over other apps**  
4. Add your API key  
5. Start Island / Cursor  

Your keys stay on the phone.
