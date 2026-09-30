# Install Aether using only your phone

## Common mistakes
1. Uploading the ZIP file itself instead of the files inside the Aether folder
2. Not opening Actions and clicking "Run workflow"
3. Looking for the APK in code files - it is under Artifacts after a green build

## Steps

### 1. GitHub account
https://github.com/signup (or log in)

### 2. New repository
https://github.com/new
- Name: Aether
- Public
- Do NOT add README
- Create repository

### 3. Upload files
1. Unzip Aether-android-source.zip on your phone
2. Open the folder named Aether
3. On GitHub: Add file -> Upload files
4. Upload EVERYTHING inside Aether (app folder, gradle folder, .github, build.gradle.kts, settings.gradle.kts, gradlew, etc.)
5. Commit changes

The repo root must show folders: app, gradle, .github
and files: build.gradle.kts, settings.gradle.kts, gradlew

### 4. Build
1. Actions tab
2. Build APK (left)
3. Run workflow -> Run workflow
4. Wait for green check (3-8 min)

If red X: open the failed job, expand "Build debug APK", copy the error and send it here.

### 5. Download APK
1. Open the green run
2. Artifacts -> Aether-APK
3. Download, unzip, open app-debug.apk
4. Allow install unknown apps, install

### 6. Setup on Android 10
1. Enable Accessibility for Aether
2. Allow Display over other apps
3. Add API key
4. Start Island

## Tell me which step fails
A) GitHub account/repo
B) Upload
C) Actions red X (paste error)
D) No APK artifact
E) App crashes after install
F) Permissions / Island not working
