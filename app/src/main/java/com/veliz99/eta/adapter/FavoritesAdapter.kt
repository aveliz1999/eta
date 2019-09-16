package com.veliz99.eta.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setMargins
import com.veliz99.eta.MainActivity
import com.veliz99.eta.R

class FavoritesAdapter(private val context: Context, private var items: MutableList<Pair<String, String>>): BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if(convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.view_favorite, parent, false)
        }

        val item = getItem(position)

        val name = convertView!!.findViewById<TextView>(R.id.textView_name)
        val address = convertView.findViewById<TextView>(R.id.textView_address)

        name.text = item.second
        address.text = item.first

        val optionsButton = convertView.findViewById<ImageButton>(R.id.imageButton_options)


        optionsButton.setOnClickListener {
            val menu = PopupMenu(context, optionsButton, Gravity.RIGHT)
            val menuInflater = menu.menuInflater
            menuInflater.inflate(R.menu.menu_favorites, menu.menu)

            menu.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.item_delete -> {
                        items.removeAt(position)
                        notifyDataSetChanged()
                        context.getSharedPreferences(MainActivity::class.java.simpleName, Context.MODE_PRIVATE).edit().apply {
                            putStringSet("favorites", items.map{favorite -> favorite.first}.toSet())
                            remove("address_${item.first}")
                            apply()
                        }

                    }
                    R.id.item_editName -> {
                        val nameInputEditText = EditText(context)
                        nameInputEditText.layoutParams = (LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)).apply {
                            setMargins(R.attr.dialogPreferredPadding)
                        }

                        AlertDialog.Builder(context, R.style.ThemeOverlay_AppCompat_Dialog)
                            .setTitle("Change Name")
                            .setMessage("New name for ${item.first}:")
                            .setView(nameInputEditText)
                            .setPositiveButton("Add") { _, _ ->
                                items[position] = item.first to nameInputEditText.text.toString()
                                notifyDataSetChanged()
                                context.getSharedPreferences(MainActivity::class.java.simpleName, Context.MODE_PRIVATE).edit().apply {
                                    putString("address_${item.first}", nameInputEditText.text.toString())
                                    apply()
                                }
                            }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .create()
                            .show()
                    }
                }

                true
            }

            menu.show()
        }

        return convertView
    }

    override fun getItem(position: Int): Pair<String, String> = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = items.size

    fun add(value: Pair<String, String>): Unit {
        items.add(value)
    }

    fun getAllItems(): List<Pair<String, String>> = items
}