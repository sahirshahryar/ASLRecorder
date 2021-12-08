package edu.gatech.ccg.aslrecorder.recording

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import edu.gatech.ccg.aslrecorder.R
import java.io.File
import java.util.ArrayList

class RecordingListFragment(wordList: ArrayList<String>,
                            sessionFiles: HashMap<String, ArrayList<File>>,
                            activity: RecordingActivity,
                            @LayoutRes layout: Int): Fragment(layout) {

    val words = wordList
    val files = sessionFiles
    val recording = activity

    var recordingListAdapter: RecordingListAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // super.onViewCreated(view, savedInstanceState)
        Log.d("HELLO", "onViewCreated!")
        val scrollView = view.findViewById<RecyclerView>(R.id.recordingList)
        scrollView.layoutManager = LinearLayoutManager(this.context)
        recordingListAdapter = RecordingListAdapter(words, files, recording)
        scrollView.adapter = recordingListAdapter
    }

    fun updateList() {
        recording.runOnUiThread {
            recordingListAdapter?.notifyDataSetChanged()
        }
    }


}