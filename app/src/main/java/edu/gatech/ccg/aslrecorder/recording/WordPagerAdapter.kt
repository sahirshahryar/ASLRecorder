/**
 * WordPagerAdapter.kt
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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import edu.gatech.ccg.aslrecorder.R
import edu.gatech.ccg.aslrecorder.summary.RecordingListFragment
import java.util.ArrayList

class WordPagerAdapter(activity: AppCompatActivity, words: ArrayList<String>,
                       recordingTimestamps: HashMap<String, ArrayList<Pair<Long, Long>>>):
    FragmentStateAdapter(activity) {

    var recordingActivity = activity as RecordingActivity

    var wordList = words
    var recordingTimestamps = recordingTimestamps

    var recordingPrompts = HashMap<String, WordPromptFragment>()
    var recordingListFragment: RecordingListFragment? = null

    override fun getItemCount() = wordList.size + 1

    override fun createFragment(position: Int): Fragment {
        return if (position < wordList.size) {
            val word = this.wordList[position]
            if (recordingPrompts.containsKey(word)) {
                recordingPrompts[word]!!
            } else {
                val result = WordPromptFragment(word, R.layout.word_prompt)
                recordingPrompts[word] = result
                result
            }
        } else {
            recordingListFragment= RecordingListFragment(wordList, recordingTimestamps,
                recordingActivity, R.layout.recording_list)
            recordingListFragment!!
        }
    }

    fun updateRecordingCount(word: String, count: Int) {
        recordingPrompts[word]?.updateWordCount(count)
    }


}