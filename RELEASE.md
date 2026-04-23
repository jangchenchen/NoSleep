# Release Notes

## Lightweight Protection

Release builds enable R8 minification and resource shrinking. Debug builds keep signature checks disabled so local testing is not blocked.

To enable official signature verification for release builds, provide the signing certificate SHA-256 as a Gradle property:

```bash
./gradlew :app:assembleRelease -PofficialSigningCertSha256=<cert-sha256>
```

After configuring a release keystore, compute the certificate fingerprint with:

```bash
keytool -list -v -keystore <release-keystore.jks> -alias <alias>
```

Store each distributed APK, source snapshot, version number, build time, and APK SHA-256 hash for ownership records.
