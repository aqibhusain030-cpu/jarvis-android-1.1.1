package com.jarvis.models

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ModelVerificationResult(
    val verified: Boolean,
    val reason: String
)

@Singleton
class ModelAssetVerifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun verifyFile(asset: ModelAsset, file: File): ModelVerificationResult {
        if (!asset.downloadableToAndroid) {
            return ModelVerificationResult(false, "Asset is not approved for Android.")
        }
        if (!file.exists() || !file.isFile) {
            return ModelVerificationResult(false, "Model file is missing.")
        }
        if (file.length() != asset.sizeBytes) {
            return ModelVerificationResult(
                false,
                "Model size mismatch: expected " + asset.sizeBytes + ", got " + file.length() + "."
            )
        }
        if (!asset.sha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            return ModelVerificationResult(false, "Manifest SHA-256 is invalid.")
        }
        if (!ModelChecksum.verify(file, asset.sha256)) {
            return ModelVerificationResult(false, "Model SHA-256 verification failed.")
        }
        if (!hardwareEligible(asset)) {
            return ModelVerificationResult(false, "Device does not satisfy model minimum hardware.")
        }
        return ModelVerificationResult(true, "Model artifact is verified.")
    }

    private fun hardwareEligible(asset: ModelAsset): Boolean {
        val requiredAbi = asset.minHardware.abis.any { expected ->
            Build.SUPPORTED_ABIS.any { actual -> actual.equals(expected, ignoreCase = true) }
        }
        if (!requiredAbi) return false

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        if (memoryInfo.totalMem < asset.minHardware.ramMb * 1024L * 1024L) return false

        val stat = StatFs(context.filesDir.absolutePath)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        return freeBytes >= asset.minHardware.freeStorageMb * 1024L * 1024L
    }
}
