package edu.gatech.ccg.aslrecorder.summary

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.gatech.ccg.aslrecorder.R
import edu.gatech.ccg.aslrecorder.databinding.ActivityRecordingSummaryBinding
import edu.gatech.ccg.aslrecorder.padZeroes
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

class RecordingSummaryActivity: AppCompatActivity() {

    private lateinit var binding: ActivityRecordingSummaryBinding

    private lateinit var recordingListAdapter: RecordingListAdapter

    private lateinit var saveButton: Button

    private lateinit var timestamps: HashMap<String, ArrayList<Pair<Long, Long>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingSummaryBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        // Shout it from the rooftops!!
        val bundle = intent!!.extras!!
        val id = bundle.getInt("ID")
        val uid = bundle.getString("UID")!!
        val words = bundle.getStringArrayList("WORDS")!!
        val file = File(bundle.getString("RECORDING_FILENAME")!!)
        val uncheckedTimestamps = bundle.getSerializable("TIMESTAMPS")!! as HashMap<*, *>
        val safeTimestamps = HashMap<String, ArrayList<Pair<Long, Long>>>()

        // Ensure type safety
        for (uncheckedKey in uncheckedTimestamps.keys) {
            val key = uncheckedKey as String
            val timestampList = uncheckedTimestamps[key] as ArrayList<*>
            val list = ArrayList<Pair<Long, Long>>()

            for (uncheckedTimestamp in timestampList) {
                val timestamp = uncheckedTimestamp as Pair<*, *>
                val first = timestamp.first as Long
                val second = timestamp.second as Long

                list.add(Pair(first, second))
            }

            safeTimestamps[key] = list
        }

        this.timestamps = safeTimestamps

        val scrollView = findViewById<RecyclerView>(R.id.recordingList)
        scrollView.layoutManager = LinearLayoutManager(applicationContext)
        recordingListAdapter = RecordingListAdapter(words, file, this.timestamps, this)
        scrollView.adapter = recordingListAdapter

        this.saveButton = findViewById(R.id.closeSession)
        this.saveButton.setOnClickListener {
            copyFileToDownloads(applicationContext, file, words, uid, id)
        }
    }

    fun deleteRecording(word: String, recordingIndex: Int) {
        timestamps[word]?.removeAt(recordingIndex)
    }


    fun copyFileToDownloads(context: Context, videoFile: File, words: ArrayList<String>,
                            UID: String, id: Int) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                "${UID}_${padZeroes(id)}_${words.joinToString("&")}_${videoFile.name}")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.SIZE, videoFile.length())
        }

        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { downloadUri ->
            resolver.openOutputStream(downloadUri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream =
                    BufferedInputStream(FileInputStream(videoFile.absoluteFile))
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }
    }

}