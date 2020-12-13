package com.github.raulbarca.audiorecorder

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

open class AudioRecorderActivity : AppCompatActivity() {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var filename: String? = null
    private var mScheduledExecutorService: ScheduledExecutorService? = null
    private val updateCurrentPosition = Runnable {
        setCurrentPosition(player?.currentPosition ?: 0)
    }

    private val btnMic by lazy { findViewById<AppCompatImageView>(R.id.btnMic) }
    private val btnStop by lazy { findViewById<AppCompatImageView>(R.id.btnStop) }
    private val btnPlay by lazy { findViewById<AppCompatImageView>(R.id.btnPlay) }
    private val btnPausePlaying by lazy { findViewById<AppCompatImageView>(R.id.btnPausePlaying) }
    private val btnDone by lazy { findViewById<AppCompatImageView>(R.id.btnDone) }
    private val tvCurrentPosition by lazy { findViewById<AppCompatTextView>(R.id.tvCurrentPosition) }
    private val tvDuration by lazy { findViewById<AppCompatTextView>(R.id.tvDuration) }
    private val playerSeekBar by lazy { findViewById<androidx.appcompat.widget.AppCompatSeekBar>(R.id.playerSeekBar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)
        setTitle(R.string.audio_recorder_title)
        setupMicButton()
        setupStopButton()
        setupPlayButton()
        setupPausePlayingButton()
        setupDoneButton()
        setupPlayerSeekBar()
    }

    override fun onStop() {
        super.onStop()
        onStopRecording()
    }

    override fun onPause() {
        super.onPause()
        onStopRecording()
    }

    private fun setupMicButton() {
        btnMic.setOnClickListener { onStartRecording() }
    }

    private fun setupStopButton() {
        btnStop.setOnClickListener { onStopRecording() }
    }

    private fun setupPlayButton() {
        btnPlay.setOnClickListener { onPlayRecording(0) }
        disablePlaying()
    }

    private fun setupPausePlayingButton() {
        btnPausePlaying.setOnClickListener { onStopPlaying() }
        disablePlaying()
    }

    private fun setupDoneButton() {
        btnDone.setOnClickListener { onFinishRecording() }
    }

    private fun setupPlayerSeekBar() {
        playerSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    onStopPlaying()
                    onPlayRecording(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setDuration() {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(filename)
        val duration = mediaMetadataRetriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toInt() ?: 0
        tvDuration.text = getDuration(duration)
        playerSeekBar.max = duration
    }

    private fun setCurrentPosition(currentPositionMillis: Int) {
        tvCurrentPosition.text = getDuration(currentPositionMillis)
        playerSeekBar.progress = currentPositionMillis
    }

    open fun onPrepareRecorder(): Boolean {
        val uuid = UUID.randomUUID().toString()
        filename = externalCacheDir?.absolutePath + uuid + ".3gp"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(filename)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e("onPrepareRecorder", e.message, e)
                return false
            }
        }
        return true
    }

    open fun onStartRecording() {
        if (onPrepareRecorder()) {
            recorder?.start()
            btnMic.visibility = View.INVISIBLE
            btnStop.visibility = View.VISIBLE
            disablePlaying()
        }
    }

    open fun onStopRecording() {
        recorder?.apply {
            release()
            btnMic.visibility = View.VISIBLE
            btnStop.visibility = View.INVISIBLE
            enablePlaying()
            setDuration()
        }
        recorder = null
    }

    open fun onPlayRecording(seekToMillis: Int) {
        player = MediaPlayer().apply {
            try {
                setDataSource(filename)
                setOnCompletionListener { onStopPlaying() }
                prepare()
                seekTo(seekToMillis)
                start()
                btnPausePlaying.visibility = View.VISIBLE
                btnPlay.visibility = View.INVISIBLE
                updatePlayerSeekBar()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    open fun onStopPlaying() {
        player?.apply {
            release()
            btnPausePlaying.visibility = View.INVISIBLE
            btnPlay.visibility = View.VISIBLE
        }
        player = null
    }

    open fun onFinishRecording() {
        val data = Intent().putExtra(BUNDLE_RECORDED_AUDIO_FILENAME, filename)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun disablePlaying() {
        btnPlay.isEnabled = false
        btnDone.isEnabled = false
        playerSeekBar.isEnabled = false
        tvDuration.visibility = View.INVISIBLE
        tvCurrentPosition.visibility = View.INVISIBLE
    }

    private fun enablePlaying() {
        btnPlay.isEnabled = true
        btnDone.isEnabled = true
        playerSeekBar.isEnabled = true
        tvDuration.visibility = View.VISIBLE
        tvCurrentPosition.visibility = View.VISIBLE
        setDuration()
        setCurrentPosition(0)
    }

    private fun updatePlayerSeekBar() {
        if (mScheduledExecutorService?.isShutdown == false) mScheduledExecutorService?.shutdown()
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        mScheduledExecutorService?.scheduleWithFixedDelay(
            { playerSeekBar?.post(updateCurrentPosition) },
            100,
            500,
            TimeUnit.MILLISECONDS
        )
    }

    private fun getDuration(durationMillis: Int): String {
        val duration = durationMillis / 1000
        val hours = duration / 3600
        var temp = duration - hours * 3600
        val mins = temp / 60
        temp -= mins * 60
        val secs = temp
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    companion object {
        const val BUNDLE_RECORDED_AUDIO_FILENAME = "BUNDLE_RECORDED_AUDIO_FILENAME"
    }
}