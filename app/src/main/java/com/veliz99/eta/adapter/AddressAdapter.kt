package com.veliz99.eta.adapter

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.veliz99.eta.R
import java.util.*

class AddressAdapter(private val context: Context): BaseAdapter(), Filterable {

    private var results = listOf<Address>()
    val geocoder = Geocoder(context, Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if(convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.view_address, parent, false)
        }

        val disp = convertView!!.findViewById<TextView>(R.id.textView_address)
        disp.text = results[position].getAddressLine(0)

        return convertView
    }

    override fun getItem(position: Int) = results[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = results.size

    override fun getFilter(): Filter {
        return object: Filter() {
            override fun performFiltering(text: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                val results = geocoder.getFromLocationName(text.toString(), 5)

                filterResults.values = results
                filterResults.count = results.size

                return filterResults
            }

            override fun publishResults(p0: CharSequence?, results: FilterResults?) {
                if(results != null && results.count > 0) {
                    this@AddressAdapter.results = results.values as List<Address>
                    notifyDataSetChanged()
                }
                else{
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}