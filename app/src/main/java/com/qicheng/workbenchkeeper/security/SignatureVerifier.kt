package com.qicheng.workbenchkeeper.security

import android.content.Context
import android.content.pm.PackageManager
import com.qicheng.workbenchkeeper.BuildConfig
import java.security.MessageDigest
import java.util.Locale

object SignatureVerifier {
    fun isOfficialBuild(context: Context): Boolean {
        if (!BuildConfig.ENABLE_SIGNATURE_CHECK) return true

        val expected = BuildConfig.OFFICIAL_SIGNING_CERT_SHA256
            .trim()
            .lowercase(Locale.US)
        if (expected.isEmpty()) return true

        val signingInfo = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo
            ?: return false

        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }

        return signatures.any { signature ->
            sha256(signature.toByteArray()) == expected
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
