package com.veliz99.eta.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.veliz99.eta.R

class FavoritesAdapter(private val context: Context, private var items: List<Pair<String, String>>): BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if(convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.view_address, parent, false)
        }

        val item = getItem(position)

        val name = convertView!!.findViewById<TextView>(R.id.textView_name)
        val address = convertView.findViewById<TextView>(R.id.textView_address)

        name.text = item.second
        address.text = item.first

        return convertView
    }

    override fun getItem(position: Int): Pair<String, String> = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = items.size
}