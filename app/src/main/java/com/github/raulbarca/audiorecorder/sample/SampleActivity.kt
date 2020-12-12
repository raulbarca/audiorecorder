package com.github.raulbarca.audiorecorder.sample

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.github.raulbarca.audiorecorder.AudioRecorderActivity

class SampleActivity : AudioRecorderActivity() {
    override fun onPrepareRecorder(): Boolean {
        return if (hasPermission()) super.onPrepareRecorder()
        else {
            requestPermission()
            false
        }
    }

    private fun hasPermission(): Boolean = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() = ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        REQUEST_CODE_PERMISSION_RECORD_AUDIO
    )

    companion object {
        private const val REQUEST_CODE_PERMISSION_RECORD_AUDIO = 1001
    }
}