package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel.MenuBarItem
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel.MenuBarItem.MenuBarItemType
import com.satohk.gphotoframe.databinding.MenuBarItemBinding
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuBarItemAdapter internal constructor(private val _list:List<MenuBarViewModel.MenuBarItem>,
                                              private val _viewModel: MenuBarViewModel) :
    RecyclerView.Adapter<MenuBarItemAdapter.MenuBarItemViewHolder>() {

    var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
    var onClick: ((view:View?, position:Int) -> Unit)? = null
    var onFocus: ((view:View?, position:Int) -> Unit)? = null

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuBarItemViewHolder {
        val view = MenuBarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuBarItemViewHolder(view)
    }

    // binds the data
    override fun onBindViewHolder(holder: MenuBarItemViewHolder, position: Int) {
        val item:MenuBarItem = _list[position]
        holder.adapterPosition = position

        val iconId = when(item.itemType){
            MenuBarItemType.SHOW_ALL -> R.drawable.all_media_icon
            MenuBarItemType.SHOW_PHOTO -> R.drawable.photo_icon
            MenuBarItemType.SHOW_MOVIE -> R.drawable.movie_icon
            MenuBarItemType.SHOW_ALBUM_LIST -> R.drawable.all_media_icon
            MenuBarItemType.SEARCH -> R.drawable.search_icon
            MenuBarItemType.SETTING -> R.drawable.search_icon
            MenuBarItemType.ALBUM_ITEM -> R.drawable.all_media_icon
            else -> 0
        }
        val captionId = when(item.itemType){
            MenuBarItemType.SHOW_ALL -> R.string.menu_item_title_all
            MenuBarItemType.SHOW_PHOTO -> R.string.menu_item_title_photo
            MenuBarItemType.SHOW_MOVIE -> R.string.menu_item_title_movie
            MenuBarItemType.SHOW_ALBUM_LIST -> R.string.row_title_album
            MenuBarItemType.SEARCH -> R.string.menu_item_title_search
            MenuBarItemType.SETTING-> R.string.menu_item_title_search
            MenuBarItemType.ALBUM_ITEM -> 0
        }
        val context = holder.binding.root.context
        val resource = context.resources!!
        if(iconId != 0){
            val img: Drawable = resource.getDrawable(iconId, context.theme!!)
            holder.binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(img, null, null, null)
            holder.binding.button.setCompoundDrawablePadding(20)
        }
        else{
            holder.binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }
        if(captionId != 0) {
            holder.binding.button.text = resource.getString(captionId)
        }
        else{
            holder.binding.button.text = _list[position].caption
        }

        _viewModel.loadIcon(item, 96, 96){
            if(it != null){
                Log.d("debug", "loaded bmp :width=%d, height=%d".format(it?.width, it?.height))
                val drawable = BitmapDrawable(it)
                holder.binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                holder.binding.button.setCompoundDrawablePadding(20)
            }
        }

        holder.onClick = this.onClick
        holder.onKeyDown = this.onKeyDown
        holder.onFocus = this.onFocus
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    // stores and recycles views as they are scrolled off screen
    class MenuBarItemViewHolder internal constructor(binding: MenuBarItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
        var onClick: ((view:View?, position:Int) -> Unit)? = null
        var onFocus: ((view:View?, position:Int) -> Unit)? = null
        private val _binding: MenuBarItemBinding
        val binding: MenuBarItemBinding get() = _binding
        internal var adapterPosition: Int = 0

        init {
            val buttonView = itemView.findViewById<TextView>(R.id.button)

            buttonView.setOnClickListener { view ->
                onClick?.invoke(view, adapterPosition)
            }

            buttonView.setOnFocusChangeListener { view, focused ->
                if(view is TextView) {
                    val context = binding.root.context
                    val resource = binding.root.context.resources!!
                    if (focused) {
                        view.setTextColor(resource.getColor(
                            R.color.menu_bar_item_foreground_highlight,
                            context.theme
                        ))
                        //view.setBackgroundColor(Color.WHITE)
                        onFocus?.invoke(view, adapterPosition)
                    } else {
                        view.setTextColor(
                            resource.getColor(
                                R.color.menu_bar_item_foreground,
                                context.theme
                            )
                        )
                        //view.setBackgroundResource(R.color.default_background)
                    }
                }
            }

            buttonView.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
                return@setOnKeyListener onKeyDown?.invoke(view, adapterPosition, keyEvent) == true
            }

            _binding = binding
        }
    }
}
