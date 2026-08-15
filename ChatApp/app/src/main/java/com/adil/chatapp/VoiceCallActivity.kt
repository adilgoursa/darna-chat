package com.adil.chatapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.adil.chatapp.databinding.ActivityVoiceCallBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import java.util.Locale

class VoiceCallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OTHER_UID = "extra_other_uid"
        const val EXTRA_OTHER_NAME = "extra_other_name"
        const val EXTRA_APP_ID = "extra_app_id"
        const val EXTRA_TOKEN = "extra_token"
        const val EXTRA_CHANNEL = "extra_channel"

        // Call states
        private const val CALL_STATE_CONNECTING = 0
        private const val CALL_STATE_ACTIVE = 1
        private const val CALL_STATE_ENDED = 2
    }

    private lateinit var binding: ActivityVoiceCallBinding
    private var mRtcEngine: RtcEngine? = null
    private var mCallState = CALL_STATE_CONNECTING
    private var mStartTime: Long = 0
    private val mHandler = Handler(Looper.getMainLooper())
    private var isMuted = false
    private var isSpeakerOn = false
    private var otherUserUid = ""
    private var otherUserName = ""
    private var currentUserUid = ""
    private var appId = ""
    private var token = ""
    private var channelName = ""

    private val updateCallDuration = object : Runnable {
        override fun run() {
            if (mCallState == CALL_STATE_ACTIVE && mStartTime > 0) {
                val duration = (System.currentTimeMillis() - mStartTime) / 1000
                val minutes = duration / 60
                val seconds = duration % 60
                binding.tvCallDuration.text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                mHandler.postDelayed(this, 1000)
            }
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            mCallState = CALL_STATE_ACTIVE
            mStartTime = System.currentTimeMillis()
            mHandler.post(updateCallDuration)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            // Other user joined
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            // Other user left, end call
            endCall()
        }

        override fun onError(err: Int) {
            super.onError(err)
            Toast.makeText(this@VoiceCallActivity, "خطأ في المكالمة: $err", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get extras
        otherUserUid = intent.getStringExtra(EXTRA_OTHER_UID) ?: ""
        otherUserName = intent.getStringExtra(EXTRA_OTHER_NAME) ?: "مكالمة"
        appId = intent.getStringExtra(EXTRA_APP_ID) ?: ""
        token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
        channelName = intent.getStringExtra(EXTRA_CHANNEL) ?: ""

        binding.tvCallerName.text = otherUserName

        // Set up buttons
        binding.btnEndCall.setOnClickListener { endCall() }
        binding.btnMute.setOnClickListener { toggleMute() }
        binding.btnSpeaker.setOnClickListener { toggleSpeaker() }

        // Initialize Agora
        initializeAgoraEngine()

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            }
        } else {
            joinChannel()
        }
    }

    private fun initializeAgoraEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = this
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            mRtcEngine = RtcEngine.create(config)
            mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في تهيئة المكالمة", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun joinChannel() {
        if (mRtcEngine == null) return

        try {
            val options = ChannelMediaOptions()
            options.autoSubscribeAudio = true
            options.autoSubscribeVideo = false
            options.publishMicrophoneTrack = true
            mRtcEngine?.joinChannel(token, channelName, 0, options)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في الانضمام للقناة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        mRtcEngine?.muteLocalAudioStream(isMuted)
        binding.btnMute.setImageResource(
            if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
        )
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = isSpeakerOn
        binding.btnSpeaker.setImageResource(
            if (isSpeakerOn) R.drawable.ic_speaker_on else R.drawable.ic_speaker_off
        )
    }

    private fun endCall() {
        mCallState = CALL_STATE_ENDED
        mHandler.removeCallbacks(updateCallDuration)
        mRtcEngine?.leaveChannel()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                joinChannel()
            } else {
                Toast.makeText(this, "المايك مطلوب للمكالمة", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(updateCallDuration)
        RtcEngine.destroy()
        mRtcEngine = null
    }
}
