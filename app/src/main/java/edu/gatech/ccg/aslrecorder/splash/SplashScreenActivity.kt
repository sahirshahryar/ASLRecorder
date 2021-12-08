package edu.gatech.ccg.aslrecorder.splash

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.gatech.ccg.aslrecorder.R

class SplashScreenActivity: AppCompatActivity() {

    lateinit var recordingSessionsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        recordingSessionsList = findViewById(R.id.recording_sessions_list)
        recordingSessionsList.layoutManager = LinearLayoutManager(this)
        recordingSessionsList.adapter = TopicListAdapter()
    }

}