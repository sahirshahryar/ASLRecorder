/**
 * RecordingActivity.kt
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.hardware.camera2.*
import android.media.MediaCodec
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.Range
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.gatech.ccg.aslrecorder.R
import edu.gatech.ccg.aslrecorder.randomChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedInputStream

import java.io.File
import java.io.FileInputStream
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gatech.ccg.aslrecorder.databinding.ActivityRecordBinding
import edu.gatech.ccg.aslrecorder.splash.TopicListAdapter
import edu.gatech.ccg.aslrecorder.splash.WordDefinitions
import edu.gatech.ccg.aslrecorder.weightedRandomChoice
import java.util.Collections.min
//import kotlinx.android.synthetic.main.activity_record.*
import java.util.concurrent.Executors
import kotlin.math.max


const val WORDS_PER_SESSION = 5

/**
 * This class handles the recording of ASL into videos.
 *
 * @author  Sahir Shahryar <contact@sahirshahryar.com>, Matthew So <matthew.so@gatech.edu>
 * @since   October 4, 2021
 * @version 1.1.0
 */
class RecordingActivity : AppCompatActivity() {

    private lateinit var context: Context

    /**
     * The button that must be held to record video.
     */
    lateinit var recordButton: FloatingActionButton


    /**
     * A timer that will be shown in the corner
     */
    lateinit var countdownTimer: CountDownTimer

    lateinit var countdownText: TextView

    /**
     * Whether or not the recording button is disabled. When this is true,
     * all interactions with the record button will be passed to the layer
     * underneath.
     */
    var recordButtonDisabled = false


    /**
     * List of words that we can swipe through
     */
    lateinit var wordList: ArrayList<String>


    /**
     * The pager used to swipe back and forth between words.
     */
    lateinit var wordPager: ViewPager2


    /**
     * The current word that the user has been asked to sign.
     */
    var currentWord: String = "test"


    var sessionTimestamps = HashMap<String, ArrayList<Pair<Long, Long>>>()


    /**
     * The thread responsible for handling the camera.
     */
    private lateinit var cameraThread: HandlerThread


    /**
     * Handler object for the camera.
     */
    private lateinit var cameraHandler: Handler

    private var globalRecordingStartTime: Long = 0L

    private var currentRecordingStartTime: Long = 0L


    /**
     * Camera executor
     */
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    /*
     * CameraX video capture object
     */
    private lateinit var videoCapture: VideoCapture<Recorder>

    /*
     * Current recording being recorded
     */
    private lateinit var currRecording: Recording

    private lateinit var binding: ActivityRecordBinding

    /*
     * Filename of file currently being recorded.
     */
    private lateinit var filename: String

    /*
     * User's UID. Assigned by CCG to each user during recording app deployment.
     */
    private var uid: String = ""

    /**
     * Additional data for recordings.
     */
    companion object {
        private const val TAG = "RecordingActivity"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            // video recording

            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))

            val recorder = Recorder.Builder()
                .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            try {
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, videoCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.US)
            filename = "${sdf.format(Date())}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            }
            val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                super.getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

            // 2. Configure Recorder and Start recording to the mediaStoreOutput.

            currRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .start(ContextCompat.getMainExecutor(super.getBaseContext())) {
                        videoRecordEvent ->
                    if (videoRecordEvent is VideoRecordEvent.Start) {
                        Log.d("currRecording", "Recording Started")
                    } else if (videoRecordEvent is VideoRecordEvent.Pause) {
                        Log.d("currRecording", "Recording Paused")
                    } else if (videoRecordEvent is VideoRecordEvent.Resume) {
                        Log.d("currRecording", "Recording Resumed")
                    } else if (videoRecordEvent is VideoRecordEvent.Finalize) {
                        val finalizeEvent =
                            videoRecordEvent as VideoRecordEvent.Finalize
                        // Handles a finalize event for the active recording, checking Finalize.getError()
                        val error = finalizeEvent.error
                        if (error != VideoRecordEvent.Finalize.ERROR_NONE) {

                        }
                        Log.d("currRecording", "Recording Finalized")
                    }

                    // All events, including VideoRecordEvent.Status, contain RecordingStats.
                    // This can be used to update the UI or track the recording duration.
                    // val recordingStats = videoRecordEvent.recordingStats
                }

            countdownText = findViewById(R.id.timerLabel)

            countdownTimer = object : CountDownTimer(900_000, 1000) {
                override fun onTick(p0: Long) {
                    val rawSeconds = (p0 / 1000).toInt() + 1
                    val minutes = padZeroes(rawSeconds / 60, 2)
                    val seconds = padZeroes(rawSeconds % 60, 2)
                    countdownText.text = "$minutes:$seconds"
                }

                override fun onFinish() {
                    TODO("Not yet implemented")
                }
            }

            countdownTimer.start()
            globalRecordingStartTime = System.currentTimeMillis()

        }, ContextCompat.getMainExecutor(this))

    }

    /**
     * This code initializes the camera-related portion of the code, adding listeners to enable
     * video recording as long as we hold down the Record button.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        /**
         * First, check camera permissions. If the user has not granted permission to use the
         * camera, give a prompt asking them to grant that permission in the Settings app, then
         * relaunch the app.
         *
         * TODO: Streamline this flow so that users don't need to restart the app at all.
         */
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
               PackageManager.PERMISSION_GRANTED) {

            val errorRoot = findViewById<ConstraintLayout>(R.id.main_root)
            val errorMessage = layoutInflater.inflate(R.layout.permission_error, errorRoot,
                false)
            errorRoot.addView(errorMessage)

            // Dim Record button
            recordButton.backgroundTintList = ColorStateList.valueOf(0xFFFA9389.toInt())
        }

        /**
         * User has given permission to use the camera
         */
        else {
            startCamera()

            val buttonLock = ReentrantLock()

            /**
             * Set a listener for when the user presses the record button.
             */
            recordButton.setOnTouchListener { _, event ->
                /**
                 * Do nothing if the record button is disabled.
                 */
                if (recordButtonDisabled) {
                    return@setOnTouchListener false
                }

                when (event.action) {
                    /**
                     * User holds down the record button:
                     */
                    MotionEvent.ACTION_DOWN -> lifecycleScope.launch(Dispatchers.IO) {

                        Log.d(TAG, "Record button down")

                        buttonLock.withLock {
                            currentRecordingStartTime =
                                System.currentTimeMillis() - globalRecordingStartTime
                        }
                    }

                    /**
                     * User releases the record button:
                     */
                    MotionEvent.ACTION_UP -> lifecycleScope.launch(Dispatchers.IO) {

                        Log.d(TAG, "Record button up")

                        buttonLock.withLock {
                            val recordingEnd = System.currentTimeMillis() - globalRecordingStartTime
                            if (!sessionTimestamps.containsKey(currentWord)) {
                                sessionTimestamps[currentWord] = ArrayList()
                            }

                            sessionTimestamps[currentWord]!!.add(
                                Pair(currentRecordingStartTime, recordingEnd)
                            )

                            val recordingCount = sessionTimestamps[currentWord]!!.size

                            val wordPagerAdapter = wordPager.adapter as WordPagerAdapter

                            runOnUiThread {
                                wordPagerAdapter.updateRecordingList()
                                wordPagerAdapter.updateRecordingCount(currentWord, recordingCount)
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Log.d(TAG, "Requesting haptic feedback (R+)")
                                recordButton.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            } else {
                                Log.d(TAG, "Requesting haptic feedback (R-)")
                                recordButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }
                    }
                }
                true
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
//            session.close()
//            camera.close()
//            cameraThread.quitSafely()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
    }

    /**
     * END BORROWED CODE FROM AOSP.
     */

    fun generateCameraThread() = HandlerThread("CameraThread").apply { start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        context = this

        // Set up view pager
        wordPager = findViewById(R.id.wordPager)

        val bundle = intent.extras!!

        this.uid = bundle.getString("UID", "")!!

        wordList = ArrayList(bundle.getStringArrayList("WORDS")!!)
        currentWord = wordList[0]

        // Set title bar text
        title = "1 of ${wordList.size}"

        wordPager.adapter = WordPagerAdapter(
            this, wordList, sessionTimestamps
        )

        wordPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                Log.d("D", "${wordList.size}")
                if (position < wordList.size) {
                    // Animate the record button back in, if necessary.
                    if (recordButtonDisabled) {
                        recordButton.animate().apply {
                            alpha(1.0f)
                            duration = 250
                        }.start()

                        recordButtonDisabled = false
                        recordButton.isClickable = true
                        recordButton.isFocusable = true
                    }

                    this@RecordingActivity.currentWord = wordList[position]
//                    this@RecordingActivity.outputFile = createFile(this@RecordingActivity)
                    title = "${position + 1} of ${wordList.size}"
                } else {
                    // Hide record button and move the slider to the front (so users can't
                    // accidentally press record)
                    recordButton.animate().apply {
                        alpha(0.0f)
                        duration = 250
                    }.start()

                    recordButton.isClickable = false
                    recordButton.isFocusable = false
                    recordButtonDisabled = true

                    title = "Session summary"
                }
            }
        })

        recordButton = findViewById(R.id.recordButton)
        recordButton.isHapticFeedbackEnabled = true

        initializeCamera()
    }

    private var initializedAlready = false

    override fun onResume() {
        super.onResume()

        cameraThread = generateCameraThread()
        cameraHandler = Handler(cameraThread.looper)
//
        if (!initializedAlready) {
            initializeCamera()
//            setupCameraCallback()
            initializedAlready = true
        }
    }

    public fun goToWord(index: Int) {
        wordPager.currentItem = index
    }

    fun concludeRecordingSession() {
        val prefs = getPreferences(MODE_PRIVATE)
        var recordingIndex = prefs.getInt("RECORDING_INDEX", 0)
        for (entry in sessionVideoFiles) {
            for (file in entry.value) {
                copyFileToDownloads(this.applicationContext, file, entry.key, ++recordingIndex)

                // Delete the copy stored within the app's sandbox
                file.delete()
            }
        }

        with (prefs.edit()) {
            putInt("RECORDING_INDEX", recordingIndex)
            for (entry in sessionVideoFiles) {
                val key = "RECORDING_COUNT_${entry.key}"
                val recordingCount = prefs.getInt(key, 0);
                putInt(key, recordingCount + entry.value.size)
            }
            apply()
        }


    }


    /**
     * THE CODE BELOW IS COPIED FROM Rubén Viguera at StackOverflow (CC-BY-SA 4.0),
     * with some modifications.
     * See https://stackoverflow.com/a/64357198/13206041.
     */
    private val DOWNLOAD_DIR = Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

    // val finalUri : Uri? = copyFileToDownloads(context, downloadedFile)

    fun copyFileToDownloads(context: Context, videoFile: File, word: String, id: Int): Uri? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.VideoColumns.DISPLAY_NAME,
                    "${UID}_${padZeroes(id)}_${word}_${videoFile.name}")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.SIZE, videoFile.length())
            }
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val authority = "${context.packageName}.provider"
            // Modify the line below if we add support for a subfolder within Downloads
            val destinyFile = File(DOWNLOAD_DIR, videoFile.name)
            FileProvider.getUriForFile(context, authority, destinyFile)
        }?.also { downloadedUri ->
            resolver.openOutputStream(downloadedUri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream = BufferedInputStream(FileInputStream(videoFile.absoluteFile))
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }
    }
    /**
     * End borrowed code from Rubén Viguera.
     */

}