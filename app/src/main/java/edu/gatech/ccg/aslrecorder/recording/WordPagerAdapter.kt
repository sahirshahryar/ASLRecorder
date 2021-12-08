package edu.gatech.ccg.aslrecorder.recording

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import edu.gatech.ccg.aslrecorder.R
import java.io.File
import java.util.ArrayList

class WordPagerAdapter(activity: AppCompatActivity, words: ArrayList<String>,
                       sessionFiles: HashMap<String, ArrayList<File>>):
    FragmentStateAdapter(activity) {

    var recordingActivity = activity as RecordingActivity

    var wordList = words
    var sessionFiles = sessionFiles

    var recordingListFragment: RecordingListFragment? = null

    override fun getItemCount() = wordList.size + 1

    override fun createFragment(position: Int): Fragment {
        return if (position < wordList.size) {
            val word = this.wordList[position]
            val result = WordPromptFragment(word, R.layout.word_prompt)
            result
        } else {
            recordingListFragment= RecordingListFragment(wordList,
                sessionFiles, recordingActivity, R.layout.recording_list)
            recordingListFragment!!
        }
    }

    fun updateRecordingList() {
        recordingListFragment?.updateList()
    }

}