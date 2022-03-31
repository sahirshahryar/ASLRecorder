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
package edu.gatech.ccg.aslrecorder.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gatech.ccg.aslrecorder.R
import edu.gatech.ccg.aslrecorder.recording.RecordingActivity
import edu.gatech.ccg.aslrecorder.recording.VideoPreviewFragment
import java.io.File
import java.lang.ref.WeakReference

class RecordingListAdapter(wordList: ArrayList<String>,
                           file: File,
                           timestamps: HashMap<String, ArrayList<Pair<Long, Long>>>,
                           activity: RecordingSummaryActivity
):
    RecyclerView.Adapter<RecordingListAdapter.RecordingListItem>() {

    val words = wordList
    val timestamps = timestamps
    val file = file
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
            nextIndex += 1 + (timestamps[word]?.size ?: 0)
            breakpoints.add(nextIndex)
        }

        breakpoints.removeAt(breakpoints.size - 1)
        totalSize = nextIndex

        return totalSize
    }

    open class RecordingListItem(itemView: View): RecyclerView.ViewHolder(itemView)

    class SectionHeader(itemView: View): RecordingListItem(itemView) {
        fun setData(word: String) {
            val label = itemView.findViewById<TextView>(R.id.recordedWord)
            label.text = word
        }
    }

    class RecordingEntry(itemView: View): RecordingListItem(itemView) {
        fun setData(word: String, recordingIndex: Int, entryPosition: Int,
                    activity: WeakReference<RecordingSummaryActivity>,
                    listAdapter: WeakReference<RecordingListAdapter>) {
            val label = itemView.findViewById<TextView>(R.id.recordingTitle)
            label.text = "Recording #${recordingIndex + 1}"

            // TODO: Show recording preview when user taps the title
            val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteRecording)
            deleteButton.setOnClickListener {
                activity.get()?.deleteRecording(word, recordingIndex)

                // NOTE: This was previously notifyItemRemoved as that is more
                // efficient, but it would cause crashes when not deleting the very latest
                // recording for a given word (as the indices would be rearranged)
                listAdapter.get()?.notifyDataSetChanged()
            }

            label.setOnClickListener {
                val timestamps = listAdapter.get()?.timestamps?.get(word)!![recordingIndex]
                val file = listAdapter.get()?.file

                val bundle = Bundle()
                bundle.putString("word", word)
                bundle.putInt("recordingIndex", recordingIndex)
                bundle.putString("filename", file?.absolutePath)
                bundle.putLong("start", timestamps.first)
                bundle.putLong("end", timestamps.second)

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

        if (holder is SectionHeader) {
            // First: Index of word in words array
            // Second: words[index] (i.e. the actual word)
            // Third: index of a particular recording inside the array at recordings[word]
            holder.setData(decomposedIndex.second)
        } else if (holder is RecordingEntry) {
            holder.setData(decomposedIndex.second, decomposedIndex.third!!,
                           position, activity, WeakReference(this)
            )
        }
    }

    override fun getItemCount(): Int {
        return updateBreakpoints()
    }

    override fun getItemViewType(position: Int): Int {
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

        val wordStartingIndex = breakpoints[index]
        val word = words[index]
        var recordingIndex: Int? = null

        val relativeIndex = itemIndex - wordStartingIndex
        if (relativeIndex > 0) {
            // It should be logically impossible to access this code unless
            // there is at least one recording for the given word
            val recordingCount = timestamps[word]!!.size
            recordingIndex = recordingCount - relativeIndex
        }

        return Triple(index, word, recordingIndex)
    }

}