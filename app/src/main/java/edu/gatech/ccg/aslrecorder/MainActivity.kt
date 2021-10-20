package edu.gatech.ccg.aslrecorder

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.camera2.*
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.*
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

import java.io.File
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * @author  Sahir Shahryar <sahirshahryar@gatech.edu>
 * @since   October 4, 2021
 * @version 1.0.0
 */
class MainActivity : AppCompatActivity() {

    /**
     * Whether or not the application should use the rear camera. The functionality
     * to switch cameras on-the-fly has not yet been implemented, so this is a constant
     * that must be set at compile time.
     */
    private val useBackCamera = false

    /**
     * The camera preview.
     */
    lateinit var cameraView: SurfaceView

    /**
     * The button that must be held to record video.
     */
    lateinit var recordButton: FloatingActionButton

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


    /**
     * CODE BORROWED FROM THE Android Open Source Project (AOSP), LICENSED UNDER THE
     * Apache 2.0 LICENSE (https://www.apache.org/licenses/LICENSE-2.0). (c) 2020 AOSP.
     *
     * SEE https://github.com/android/camera-samples/blob/master/Camera2Video/app/src/main/java/com/
     *     example/android/camera2/video/fragments/CameraFragment.kt
     */

    private lateinit var camera: CameraDevice

    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    private val cameraHandler = Handler(cameraThread.looper)

    private lateinit var session: CameraCaptureSession

    private val cameraManager: CameraManager by lazy {
        val context = this.applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val outputFile: File by lazy {
        createFile(this)
    }

    private lateinit var previewSurface: Surface

    private val previewRequest: CaptureRequest by lazy {
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
        }.build()
    }

    private val recordRequest: CaptureRequest by lazy {
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(previewSurface)
            addTarget(recordingSurface)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        }.build()
    }

    private val recordingSurface: Surface by lazy {
        val surface = MediaCodec.createPersistentInputSurface()
        createRecorder(surface).apply {
            prepare()
            release()
        }

        surface
    }

    private val recorder: MediaRecorder by lazy { createRecorder(recordingSurface) }

    private var recordingStartMillis: Long = 0L

    private fun createRecorder(surface: Surface) = MediaRecorder().apply {
        // TODO: Is recording audio necessary?
        // setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        // if (args.fps > 0) setVideoFrameRate(args.fps)

        // TODO: Device-specific!
        setVideoSize(1920, 1080)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        // setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setInputSurface(surface)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L

        /** Creates a [File] named with the current date and time */
        private fun createFile(activity: MainActivity): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
            val currentWord = activity.currentWord
            return File(activity.applicationContext.filesDir,
                "${currentWord}-${sdf.format(Date())}.mp4")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        // TODO: Determine `cameraId`'s value
        val manager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId = ""

        for (id in manager.cameraIdList) {
            val face = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            if (face == CameraSelector.LENS_FACING_FRONT) {
                cameraId = id
                break
            }
        }

        if (cameraId == "") {
            throw IllegalStateException("No front camera available")
        }

        camera = openCamera(cameraManager, cameraId, cameraHandler)

        val targets = listOf(/* fragmentCameraBinding.viewFinder.holder.surface, */
            previewSurface, recordingSurface)
        session = createCaptureSession(camera, targets, cameraHandler)
        session.setRepeatingRequest(previewRequest, null, cameraHandler)

        // React to user touching the capture button
        recordButton.setOnTouchListener { view, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> lifecycleScope.launch(Dispatchers.IO) {

                    // Prevents screen rotation during the video recording
                    this@MainActivity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LOCKED

                    // Start recording repeating requests, which will stop the ongoing preview
                    //  repeating requests without having to explicitly call `session.stopRepeating`
                    session.setRepeatingRequest(recordRequest, null, cameraHandler)

                    // Finalizes recorder setup and starts recording
                    recorder.apply {
                        // Sets output orientation based on current sensor value at start time
                        // relativeOrientation.value?.let { setOrientationHint(it) }
                        prepare()
                        start()
                    }
                    recordingStartMillis = System.currentTimeMillis()
                    Log.d(TAG, "Recording started")

                    // Starts recording animation
                    // fragmentCameraBinding.overlay.post(animationTask)

                    // Send recording started haptic
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        delay(100)
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                    }
                }

                MotionEvent.ACTION_UP -> lifecycleScope.launch(Dispatchers.IO) {

                    // Unlocks screen rotation after recording finished
                    // requireActivity().requestedOrientation =
                    //    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                    // Requires recording of at least MIN_REQUIRED_RECORDING_TIME_MILLIS
                    val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
                    if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
                        delay(MIN_REQUIRED_RECORDING_TIME_MILLIS - elapsedTimeMillis)
                    }

                    Log.d(TAG, "Recording stopped. Output file: $outputFile")
                    recorder.stop()

                    // TODO: Don't delete the code below just yet. I'm not sure whether errors
                    //   were occurring due to GPU-based rendering or due to an actual
                    //   error in the code.

                    // Removes recording animation
                    // fragmentCameraBinding.overlay.removeCallbacks(animationTask)

                    // Broadcasts the media file to the rest of the system
//                    MediaScannerConnection.scanFile(
//                        view.context, arrayOf(outputFile.absolutePath), null, null)
//
//                    // Launch external activity via intent to play video recorded using our provider
//                    startActivity(Intent().apply {
//                        action = Intent.ACTION_VIEW
//                        type = MimeTypeMap.getSingleton()
//                            .getMimeTypeFromExtension(outputFile.extension)
//                        val authority = "${BuildConfig.APPLICATION_ID}.provider"
//                        data = FileProvider.getUriForFile(view.context, authority, outputFile)
//                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
//                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    // })

                    // Finishes our current camera screen
                    // navController.popBackStack()

                    // Send a haptic feedback on recording end
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        delay(100)
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                    }

                }
            }

            true
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
                this@MainActivity.finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when(error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Creates a capture session using the predefined targets, and defines a session state
        // callback which resumes the coroutine once the session is configured
        device.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * END BORROWED CODE FROM AOSP.
     */

    private fun acquirePreviewSurface() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set title bar text
        // actionBar?.title = "Please hold the button and record..."

        // Set up view pager
        wordPager = findViewById(R.id.wordPager)

//        val wordsInputStream = resources.openRawResource(resources.getIdentifier("words",
//                                                 "values", packageName))
//        val wordList = ArrayList<String>()
//        wordsInputStream.bufferedReader().useLines { lines -> lines.forEach { wordList.add(it) } }

        val wordArray = resources.getStringArray(R.array.words)
        wordList = ArrayList(listOf(*wordArray))
        currentWord = wordList[0]

        for (word in wordList) {
            Log.d("D", "Word $word")
        }

        wordPager.adapter = WordPagerAdapter(this, wordList)
        wordPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                this@MainActivity.currentWord = wordList[position]
            }
        })

        cameraView = findViewById(R.id.cameraPreview)
        recordButton = findViewById(R.id.recordButton)

//        cameraProviderLF = ProcessCameraProvider.getInstance(this)
//        cameraProviderLF.addListener({
//            val cameraProvider = cameraProviderLF.get()
//            bindCameraPreview(cameraProvider)
//        }, ContextCompat.getMainExecutor(this))

        cameraView.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                previewSurface = holder.surface
                initializeCamera()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                // PROBABLY NOT THE BEST IDEA!
//                previewSurface = holder.surface
//                initializeCamera()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // TODO("Not yet implemented")
            }

        })
        // initializeCamera()
    }

    fun getCamera(): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(
                if (useBackCamera) CameraSelector.LENS_FACING_BACK
                else CameraSelector.LENS_FACING_FRONT
            )
            .build()
    }

    fun beginRecording() {
        // Start video recording


        // Send haptic feedback (single tap) to confirm recording has ended
    }

    fun endRecording() {
        // End recording

        // Send haptic feedback (double tap) to confirm recording has finished

        // Save file
    }

}