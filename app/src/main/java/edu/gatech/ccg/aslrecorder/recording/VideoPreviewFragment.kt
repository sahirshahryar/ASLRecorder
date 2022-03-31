/**
 * VideoPreviewFragment.kt
 * This file is part of ASLRecorder, licensed under the MIT license.
 *
 * Copyright (c) 2021 Sahir Shahryar <contact@sahirshahryar.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package edu.gatech.ccg.aslrecorder.recording

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import edu.gatech.ccg.aslrecorder.R
import java.io.File
import java.util.*

class VideoPreviewFragment(@LayoutRes layout: Int): DialogFragment(layout),
     SurfaceHolder.Callback, MediaPlayer.OnPreparedListener {

    lateinit var mediaPlayer: MediaPlayer

    lateinit var recordingUri: Uri

    lateinit var word: String

    lateinit var attemptNumber: String

    private var recordingStartPoint: Long = -1

    private var recordingEndpoint: Long = -1

    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recordingPath = arguments?.getString("filename")!!
        this.recordingUri = Uri.fromFile(File(recordingPath))

        val word = arguments?.getString("word")!!
        this.word = word

        val start = arguments?.getLong("start")!!
        this.recordingStartPoint = start

        val end = arguments?.getLong("end")!!
        this.recordingEndpoint = end

        val attemptNumber = arguments?.getInt("recordingIndex")!! + 1
        this.attemptNumber = "Attempt #$attemptNumber"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.setStyle(STYLE_NO_FRAME, 0)

        val videoView = view.findViewById<VideoView>(R.id.videoPreview)
        videoView.holder.addCallback(this)

        val title = view.findViewById<TextView>(R.id.wordBeingSigned)
        title.text = this.word

        val attemptCounter = view.findViewById<TextView>(R.id.attemptCounter)
        attemptCounter.text = attemptNumber
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        this.mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), recordingUri)
            setSurface(holder.surface)
            setOnPreparedListener(this@VideoPreviewFragment)
            prepareAsync()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // TODO("Not yet implemented")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // TODO("Not yet implemented")
    }

    // MediaPlayer
    override fun onPrepared(mp: MediaPlayer?) {
        mp?.let {
            it.isLooping = true
            it.seekTo(this.recordingStartPoint.toInt())
            it.start()

            val delay = this.recordingEndpoint - this.recordingStartPoint
            timer.scheduleAtFixedRate(
                object: TimerTask() {
                    override fun run() {
                        this@VideoPreviewFragment.activity?.runOnUiThread {
                            it.seekTo(this@VideoPreviewFragment.recordingStartPoint.toInt())
                        }
                    }
                }, 0, delay)
        }
    }


}