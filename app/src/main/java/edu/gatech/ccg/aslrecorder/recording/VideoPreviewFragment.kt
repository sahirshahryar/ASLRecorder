package edu.gatech.ccg.aslrecorder.recording

import android.os.Bundle
import android.view.View
import android.widget.VideoView
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import edu.gatech.ccg.aslrecorder.R
import java.io.File

class VideoPreviewFragment(@LayoutRes layout: Int): DialogFragment(layout) {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recordingFile = arguments?.get("file") as File
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val videoView = view.findViewById<VideoView>(R.id.videoPreview)

        
    }



}