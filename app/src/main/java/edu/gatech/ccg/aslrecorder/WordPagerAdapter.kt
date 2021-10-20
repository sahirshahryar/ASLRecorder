package edu.gatech.ccg.aslrecorder

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.util.ArrayList

class WordPagerAdapter(activity: AppCompatActivity, words: ArrayList<String>):
    FragmentStateAdapter(activity) {

    var wordList = words

    override fun getItemCount() = wordList.size

    override fun createFragment(position: Int): Fragment {
        val word = this.wordList[position]
        val result = WordPromptFragment(word, R.layout.word_prompt)
        return result
    }

}