package edu.gatech.ccg.aslrecorder.recording

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import edu.gatech.ccg.aslrecorder.R

class WordPromptFragment(label: String, @LayoutRes layout: Int): Fragment(layout) {

    var label: String = label

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textField = view.findViewById<TextView>(R.id.promptText)
        textField.text = label
    }

}