package edu.gatech.ccg.aslrecorder.splash

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gatech.ccg.aslrecorder.R
import edu.gatech.ccg.aslrecorder.recording.RecordingActivity

class TopicListAdapter: RecyclerView.Adapter<TopicListAdapter.TopicListItem>() {

    class TopicListItem(itemView: View):
        RecyclerView.ViewHolder(itemView) {

        lateinit var wordList: ArrayList<String>
        lateinit var title: String
        lateinit var description: String

        fun setData(wordList: ArrayList<String>,
                    title: String, description: String) {
            this.wordList = wordList
            this.title = title
            this.description = description

            // Set labels
            val titleText = itemView.findViewById<TextView>(R.id.cardTitle)
            titleText.text = title

            val descText = itemView.findViewById<TextView>(R.id.cardDescription)
            descText.text = description

            // When the user clicks on this card, start a recording session
            // using the words in this list.
            itemView.setOnClickListener {
                val recordingActivityIntent = Intent(itemView.context,
                    RecordingActivity::class.java).apply {
                    putStringArrayListExtra("WORDS", wordList)
                }

                itemView.context.startActivity(recordingActivityIntent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicListItem {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.splash_card, parent, false)

        return TopicListItem(view)
    }

    override fun onBindViewHolder(holder: TopicListItem, position: Int) {
        val topic = WordDefinitions.values()[position]
        val wordArray = holder.itemView.context.resources.getStringArray(topic.resourceId)
        val words = ArrayList(listOf(*wordArray))

        holder.setData(words, topic.title, topic.desc)
    }

    override fun getItemCount(): Int {
        Log.d("TopicListAdapter", "Item count = ${WordDefinitions.values().size}")
        return WordDefinitions.values().size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.splash_card
    }


}


