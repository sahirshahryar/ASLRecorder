package edu.gatech.ccg.aslrecorder.recording

import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gatech.ccg.aslrecorder.R
import java.io.File
import java.lang.ref.WeakReference

class RecordingListAdapter(wordList: ArrayList<String>,
                           sessionFiles: HashMap<String, ArrayList<File>>,
                           activity: RecordingActivity):
    RecyclerView.Adapter<RecordingListAdapter.RecordingListItem>() {

    val words = wordList
    val recordings = sessionFiles
    val activity = WeakReference(activity)

    val breakpoints = ArrayList<Int>()
    var totalSize = 0

    init {
        updateBreakpoints()
    }

    fun updateBreakpoints(): Int {
        breakpoints.clear()
        breakpoints.add(0)
        var nextIndex = 0
        for (word in words) {
            nextIndex += 1 + (recordings[word]?.size ?: 0)
            breakpoints.add(nextIndex)
        }

        breakpoints.removeAt(breakpoints.size - 1)
        totalSize = nextIndex

        Log.d("HELLO", "Total size = $totalSize")
        return totalSize
    }

    open class RecordingListItem(itemView: View): RecyclerView.ViewHolder(itemView)

    class SectionHeader(itemView: View): RecordingListItem(itemView) {
        fun setData(word: String, paginationIndex: Int,
                    activity: WeakReference<RecordingActivity>) {
            val label = itemView.findViewById<TextView>(R.id.recordedWord)
            label.text = word

            val jumpButton = itemView.findViewById<ImageButton>(R.id.jumpToWord)
            jumpButton.setOnClickListener {
                activity.get()?.goToWord(paginationIndex)
            }
        }
    }

    class RecordingEntry(itemView: View): RecordingListItem(itemView) {
        fun setData(word: String, recordingIndex: Int, entryPosition: Int,
                    activity: WeakReference<RecordingActivity>,
                    listAdapter: WeakReference<RecordingListAdapter>) {
            val label = itemView.findViewById<TextView>(R.id.recordingTitle)
            label.text = "Recording #${recordingIndex + 1}"

            // TODO: Show recording preview when user taps the title
            val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteRecording)
            deleteButton.setOnClickListener {
                activity.get()?.deleteRecording(word, recordingIndex)
                listAdapter.get()?.notifyItemRemoved(entryPosition)
            }

            label.setOnClickListener {
                val mediaPlayer = MediaPlayer().apply {
                    activity.get()?.let {
                        setDataSource(it.sessionVideoFiles[word]!![recordingIndex].absolutePath)
                    }
                    prepare()
                    start()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingListItem {
        return when (viewType) {
            1 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recording_list_item, parent, false)
                RecordingEntry(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recording_list_header, parent, false)
                SectionHeader(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecordingListItem, position: Int) {
        val decomposedIndex = getWordAndIndex(position)
        Log.d("HELLO", "Binding $position ($decomposedIndex)")
        if (holder is SectionHeader) {
            Log.d("HELLO", "is SectionHeader!")
            holder.setData(decomposedIndex.second, decomposedIndex.first, activity)
        } else if (holder is RecordingEntry) {
            holder.setData(decomposedIndex.second, decomposedIndex.third!!,
                           position, activity, WeakReference(this)
            )
        }
    }

    override fun getItemCount(): Int {
        Log.d("HELLO", "totalSize = $totalSize")
        return updateBreakpoints()
    }

    override fun getItemViewType(position: Int): Int {
        Log.d("HELLO", "item view type of  $position")
        val decomposedIndex = getWordAndIndex(position)
        return if (decomposedIndex.third != null) 1 else 0
    }

    fun getWordAndIndex(itemIndex: Int): Triple<Int, String, Int?> {
        var index = 1
        while (index < words.size) {
            if (breakpoints[index] > itemIndex) {
                break
            } else {
                index += 1
            }
        }

        index -= 1
        Log.d("HELLO", "itemIndex: $itemIndex, i:$index, b:$breakpoints, wsize:${words.size}")
        val wordStartingIndex = breakpoints[index]
        val word = words[index]
        var recordingIndex: Int? = null

        val relativeIndex = itemIndex - wordStartingIndex
        if (relativeIndex > 0) {
            // It should be logically impossible to access this code unless
            // there is at least one recording for the given word
            val recordingCount = recordings[word]!!.size
            recordingIndex = recordingCount - relativeIndex
        }

        return Triple(itemIndex, word, recordingIndex)
    }

}