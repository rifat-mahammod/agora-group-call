package com.example.voice_calling_kotlin2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.voice_calling_kotlin2.R
import com.google.gson.Gson
import io.agora.rtc2.*
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    // Fill the App ID of your project generated on Agora Console.
    private val appId = "3de8978e50af4e37ae03c62399598d99"

    // Fill the channel name.
    private var channelName = "test2"

    // Fill the temp token generated on Agora Console.
    private var token =
        "" //"007eJxTYDA00VPR3iaSnDJNZnG7GvP+pQn1xg3Hg1edYzXZZ3dQUlyBwTgl1cLS3CLV1CAxzSTV2Dwx1cA42czI2NLS1NIixdLSZ4Z7SkMgI8PyzNWsjAwQCOKzMpSkFpcYMTAAADBTG9o=";

    // An integer that identifies the local user.
    private val uid = 0

    // Track the status of your connection
    private var isJoined = false

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null
    private var tokenRole // The token role
            = 0
    private val serverUrl =
        "https://agora-token-service-production-bf0a.up.railway.app" // The base URL to your token server, for example, "https://agora-token-service-production-92ff.up.railway.app".
    private val tokenExpireTime = 300 // Expire time in Seconds.
    private var editChannelName // To read the channel name from the UI.
            : EditText? = null

    // UI elements
    private var infoText: TextView? = null
    private var joinLeaveButton: Button? = null

    // Volume Control
    private var volumeSeekBar: SeekBar? = null
    private var muteCheckBox: CheckBox? = null
    private var volume = 50
    private var remoteUid = 0 // Stores the uid of the remote user

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)

    private fun checkSelfPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED
    }

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the event that the token is about to expire
        override fun onTokenPrivilegeWillExpire(token: String) {
            Log.i("i", "Token Will expire")
            fetchToken(uid, channelName, tokenRole)
            super.onTokenPrivilegeWillExpire(token)
        }

        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            remoteUid = uid
            runOnUiThread { infoText!!.text = "Remote user joined: $uid" }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true
            showMessage("Joined Channel $channel")
            runOnUiThread { infoText!!.text = "Waiting for a remote user to join" }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            showMessage("Remote user offline $uid $reason")
            if (isJoined) runOnUiThread { infoText!!.text = "Waiting for a remote user to join" }
        }

        override fun onLeaveChannel(stats: RtcStats) {
            // Listen for the local user leaving the channel
            runOnUiThread { infoText!!.text = "Press the button to join a channel" }
            isJoined = false
        }
    }

    //    private void joinChannel() {
    //        ChannelMediaOptions options = new ChannelMediaOptions();
    //        options.autoSubscribeAudio = true;
    //        // Set both clients as the BROADCASTER.
    //        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
    //        // Set the channel profile as BROADCASTING.
    //        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
    //
    //        // Join the channel with a temp token.
    //        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
    //        agoraEngine.joinChannel(token, channelName, uid, options);
    //    }
    fun joinChannel() {
        channelName = editChannelName!!.text.toString()
        if (channelName.length == 0) {
            showMessage("Type a channel name")
            return
        } else if (!serverUrl.contains("http")) {
            showMessage("Invalid token server URL")
            return
        }
        //        tokenRole = Constants.CLIENT_ROLE_BROADCASTER;
//        fetchToken(uid, channelName, tokenRole);
        if (checkSelfPermission()) {
            tokenRole = Constants.CLIENT_ROLE_BROADCASTER
            // Display LocalSurfaceView.
//            setupLocalVideo();
//            localSurfaceView.setVisibility(View.VISIBLE);
            fetchToken(uid, channelName, tokenRole)
        } else {
            showMessage("Permissions was not granted")
        }
    }

    fun joinLeaveChannel(view: View?) {
        if (isJoined) {
            agoraEngine!!.leaveChannel()
            joinLeaveButton!!.text = "Join"
        } else {
            joinChannel()
            joinLeaveButton!!.text = "Leave"
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
//        if (!checkSelfPermission()) {
//            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
//        }
//        setupVoiceSDKEngine()
//        volumeSeekBar = findViewById<View>(R.id.volumeSeekBar) as SeekBar
//        volumeSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                volume = progress
//                agoraEngine!!.adjustRecordingSignalVolume(volume)
//                //                agoraEngine.adjustPlaybackSignalVolume(volume);
////                agoraEngine.adjustUserPlaybackSignalVolume(remoteUid,volume);
////                agoraEngine.adjustAudioMixingVolume(volume);
////                agoraEngine.adjustAudioMixingPlayoutVolume(volume);
////                agoraEngine.adjustAudioMixingPublishVolume(volume);
////                agoraEngine.setInEarMonitoringVolume(volume);
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar) {
//                //Required to implement OnSeekBarChan§geListener
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                //Required to implement OnSeekBarChangeListener
//            }
//        })
//        muteCheckBox = findViewById<View>(R.id.muteCheckBox) as CheckBox
//        muteCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked -> //                agoraEngine.muteRemoteAudioStream(remoteUid, isChecked);
//            //                agoraEngine.muteAllRemoteAudioStreams(isChecked);
//            agoraEngine!!.muteLocalAudioStream(isChecked)
//        }
//
//        // Set up access to the UI elements
//        joinLeaveButton = findViewById<Button>(R.id.joinLeaveButton)
//        infoText = findViewById<TextView>(R.id.infoText)
//        editChannelName = findViewById<View>(R.id.editChannelName) as EditText
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
        setupVoiceSDKEngine()

        val muteButton: ImageButton = findViewById(R.id.muteButton)
        var isMuted = false

        muteButton.setOnClickListener {
            isMuted = !isMuted
            if (isMuted) {
                muteButton.setImageResource(R.drawable.mute_off)
                agoraEngine!!.muteLocalAudioStream(true)
            } else {
                muteButton.setImageResource(R.drawable.mute)
                agoraEngine!!.muteLocalAudioStream(false)
            }
        }

        volumeSeekBar = findViewById<View>(R.id.volumeSeekBar) as SeekBar
        volumeSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                volume = progress
                agoraEngine!!.adjustRecordingSignalVolume(volume)
                //                agoraEngine.adjustPlaybackSignalVolume(volume);
//                agoraEngine.adjustUserPlaybackSignalVolume(remoteUid,volume);
//                agoraEngine.adjustAudioMixingVolume(volume);
//                agoraEngine.adjustAudioMixingPlayoutVolume(volume);
//                agoraEngine.adjustAudioMixingPublishVolume(volume);
//                agoraEngine.setInEarMonitoringVolume(volume);
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Required to implement OnSeekBarChan§geListener
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Required to implement OnSeekBarChangeListener
            }
        })
//        muteCheckBox = findViewById<View>(R.id.muteCheckBox) as CheckBox
//        muteCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked -> //                agoraEngine.muteRemoteAudioStream(remoteUid, isChecked);
//            //                agoraEngine.muteAllRemoteAudioStreams(isChecked);
//            agoraEngine!!.muteLocalAudioStream(isChecked)
//        }

        // Set up access to the UI elements
        joinLeaveButton = findViewById<Button>(R.id.joinLeaveButton)
        infoText = findViewById<TextView>(R.id.infoText)
        editChannelName = findViewById<View>(R.id.editChannelName) as EditText
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    // Fetch the <Vg k="VSDK" /> token
    private fun fetchToken(uid: Int, channelName: String, tokenRole: Int) {
        // Prepare the Url
        val URLString = (serverUrl + "/rtc/" + channelName + "/" + tokenRole + "/"
                + "uid" + "/" + uid + "/?expiry=" + tokenExpireTime)
        val client = OkHttpClient()

        // Instantiate the RequestQueue.

//        val request: Request = Builder()
        val request: Request = Request.Builder()
            .url(URLString)
            .header("Content-Type", "application/json; charset=UTF-8")
            .get()
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("IOException", e.toString())
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val gson = Gson()
                    val result = response.body!!.string()
                    val map = gson.fromJson<Map<*, *>>(
                        result,
                        MutableMap::class.java
                    )
                    val _token = map["rtcToken"].toString()
                    if (!isJoined) setToken(_token)
                    Log.i("Token Received", _token)
                }
            }
        })
    }

    fun setToken(newValue: String) {
        Log.d("setToken", "called")
        token = newValue
        if (!isJoined) { // Join a channel
            Log.d("if", "called")
            val options = ChannelMediaOptions()
            //            options.autoSubscribeAudio = true;
//            // Set both clients as the BROADCASTER.
//            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
//            // Set the channel profile as BROADCASTING.
//            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

            // For a Video call, set the channel profile as COMMUNICATION.
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            // Start local preview.
            agoraEngine!!.startPreview()

            // Join the channel with a token.
            agoraEngine!!.joinChannel(token, channelName, uid, options)
        } else { // Already joined, renew the token by calling renewToken
            agoraEngine!!.renewToken(token)
            showMessage("Token renewed")
        }
    }

//    companion object {
//        private const val PERMISSION_REQ_ID = 22
//        private val REQUESTED_PERMISSIONS = arrayOf(
//            Manifest.permission.RECORD_AUDIO
//        )
//    }
}