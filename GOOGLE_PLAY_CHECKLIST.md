# Google Play checklist

## Before uploading

- Use a unique permanent package name: `com.stopreels`.
- Build a signed Android App Bundle (`.aab`), not only a debug APK.
- Increment `versionCode` for every update.
- Target the API level currently required by Google Play.
- Host the privacy policy at a public HTTPS URL.
- Complete the Data safety form accurately.
- Complete the AccessibilityService permission declaration and prominent in-app disclosure.
- Prepare screenshots, app icon, short description, full description and support email.
- Test the signed release on several Android versions and manufacturers.

## Build a release bundle

Create a private upload keystore and keep it outside Git:

```bash
keytool -genkeypair -v \
  -keystore stopreels-upload.jks \
  -alias stopreels \
  -keyalg RSA -keysize 4096 -validity 10000
```

Configure signing in a local `keystore.properties` file that is ignored by Git,
then build:

```bash
cp keystore.properties.example keystore.properties
```

Fill in that local file with the keystore path and passwords. Never commit it.

```bash
./gradlew clean test lint bundleRelease
```

The bundle will be in:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Play Console steps

1. Create or sign in to a Google Play developer account.
2. Create an app and keep the package name `com.stopreels`.
3. Complete the store listing and contact details.
4. Add the public privacy-policy URL.
5. Complete App content, Data safety and AccessibilityService declarations.
6. Upload the signed `.aab` to Internal testing first.
7. Test the release with a small group of devices.
8. Move to Closed testing or Production when the Play Console requirements are satisfied.
9. Submit the release for review.

Do not upload a debug APK, a private keystore, or a bundle containing secrets.
