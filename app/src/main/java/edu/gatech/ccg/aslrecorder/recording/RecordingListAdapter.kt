/**
 * RecordingListAdapter.kt
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

import android.os.Bundle
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

/**
 * Contains the logic for managing the list of recordings, which is shown to
 * the user after they finish all their recordings.
 */
class RecordingListAdapter(wordList: ArrayList<String>,
                           sessionFiles: HashMap<String, ArrayList<File>>,
                           activity: RecordingActivity):
    RecyclerView.Adapter<RecordingListAdapter.RecordingListItem>() {

    /**
     * The list of words being recorded.
     */
    val words = wordList


    /**
     * The list of recordings that should be displayed on this page.
     * This is technically a strong reference to [RecordingActivity.sessionVideoFiles],
     * which is probably bad practice, but since this adapter only lives while
     * RecordingActivity itself lives, we should be fine.
     */
    val recordings = sessionFiles


    /**
     * Get a weak reference to the currently active [RecordingActivity]. In practice,
     * there's no real reason this needs to be a [WeakReference].
     */
    val activity = WeakReference(activity)


    /**
     * A list of positions where new words' entries begin within the list of objects in the
     * [RecyclerView]. For example, if there are two recordings for "hello", one for "world",
     * and three for "I can't think of another word", then this array would be [0, 3, 5].
     * (Note that we include a slot for the HEADING for each word, since the header for each
     * recording takes up an item in the list as well.)
     */
    val breakpoints = ArrayList<Int>()


    /**
     * The total number of items rendered by the [RecyclerView]
     */
    var totalSize = 0

    init {
        // Set initial values for the breakpoints array
        updateBreakpoints()
    }

    /**
     * Update the breakpoints array, which is used to quickly determine the word and index
     * associated with the recording at a given index within the list. See [breakpoints]
     * for an example of the values calculated by this function.
     */
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

    /**
     * Parent class for both section headings and individual recording entries
     */
    open class RecordingListItem(itemView: View): RecyclerView.ViewHolder(itemView)

    /**
     * The class that represents the boldfaced label shown above the recordings for a
     * particular word. The user can press the recording button next to the word's name
     * to quickly jump to that word.
     */
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


    /**
     * The class that represents an individual recording. The user can tap the title
     * of the word to review their recording, or they can press the trash can icon to delete
     * the recording.
     */
    class RecordingEntry(itemView: View): RecordingListItem(itemView) {
        fun setData(word: String, recordingIndex: Int, entryPosition: Int,
                    activity: WeakReference<RecordingActivity>,
                    listAdapter: WeakReference<RecordingListAdapter>) {
            val label = itemView.findViewById<TextView>(R.id.recordingTitle)
            label.text = "Recording #${recordingIndex + 1}"

            val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteRecording)
            deleteButton.setOnClickListener {
                activity.get()?.deleteRecording(word, recordingIndex)

                // NOTE: This was previously notifyItemRemoved as that is more
                // efficient, but it would cause crashes when not deleting the very latest
                // recording for a given word (as the indices would be rearranged)
                listAdapter.get()?.notifyDataSetChanged()
            }

            // When we click the title, launch a modal which shows the user that particular
            // recording.
            label.setOnClickListener {
                val file = listAdapter.get()?.recordings?.get(word)!![recordingIndex]

                val bundle = Bundle()
                bundle.putString("word", word)
                bundle.putInt("recordingIndex", recordingIndex)
                bundle.putString("filename", file.absolutePath)

                val previewFragment = VideoPreviewFragment(R.layout.recording_preview)
                previewFragment.arguments = bundle

                val transaction = activity.get()!!.supportFragmentManager.beginTransaction()
                transaction.add(previewFragment, "videoPreview")
                transaction.commit()
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
            // First: Index of word in words array
            // Second: words[index] (i.e. the actual word)
            // Third: index of a particular recording inside the array at recordings[word]
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

        return Triple(index, word, recordingIndex)
    }

}