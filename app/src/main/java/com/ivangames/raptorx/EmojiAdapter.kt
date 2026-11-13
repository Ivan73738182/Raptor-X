package com.ivangames.raptorx

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class EmojiAdapter(context: Context, emojis: Array<String>) : ArrayAdapter<String>(context, 0, emojis) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView = convertView as? TextView ?: TextView(context).apply {
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
        textView.text = getItem(position)
        return textView
    }
}
