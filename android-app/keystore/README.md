# Shared debug signing (`career-ops-debug.jks`)

This PKCS12 file signs **debug** APKs for `com.careerops.mobile` so installs **upgrade in place** instead of forcing an uninstall when:

- you switch between **GitHub Actions** builds and **local Android Studio** builds, or  
- you install successive CI APKs on the same device.

**Passwords (debug only, not secret):** `storePassword` and `keyPassword` are both `android`. **Alias:** `careeropsdebug`.

Do **not** use this keystore for Play Store release builds.

## Regenerating (only if the file is lost)

From repo root (requires `openssl`):

```bash
cd android-app/keystore
openssl genrsa -out _k.pem 2048
openssl req -new -x509 -key _k.pem -out _c.pem -days 10000 -subj "/CN=Career Ops Debug/O=CareerOpsMobile/C=IN"
openssl pkcs12 -export -out career-ops-debug.jks -inkey _k.pem -in _c.pem -name careeropsdebug -password pass:android
rm -f _k.pem _c.pem
```

Then update `app/build.gradle.kts` signing if alias/password change, and reinstall once on devices (signature change).
