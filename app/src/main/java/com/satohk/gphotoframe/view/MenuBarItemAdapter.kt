package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.databinding.MenuBarItemBinding
import android.widget.TextView
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel.MenuBarItem


class MenuBarItemAdapter:
    RecyclerView.Adapter<MenuBarItemAdapter.MenuBarItemViewHolder>() {

    private var _list: List<MenuBarItem> = listOf()

    var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
    var onClick: ((view:View?, position:Int) -> Unit)? = null
    var onFocus: ((view:View?, position:Int) -> Unit)? = null
    var loadIcon: ((menuBarItem: MenuBarItem, width:Int?, height:Int?, callback:(bmp:Bitmap?)->Unit)->Unit)? = null

    internal fun setList(list: List<MenuBarItem>){
        _list = list
    }

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuBarItemViewHolder {
        Log.d("MenuBarItemAdapter", "onCreateViewHolder")
        val view = MenuBarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuBarItemViewHolder(view)
    }

    // binds the data
    override fun onBindViewHolder(holder: MenuBarItemViewHolder, position: Int) {
        Log.d("MenuBarItemAdapter", "onBindViewHolder")
        val item: MenuBarItem = _list[position]
        holder.adapterPosition = position

        val iconId = when(item.itemType){
            MenuBarItem.MenuBarItemType.SHOW_ALL -> R.drawable.all_media_icon
            MenuBarItem.MenuBarItemType.SHOW_PHOTO -> R.drawable.photo_icon
            MenuBarItem.MenuBarItemType.SHOW_MOVIE -> R.drawable.movie_icon
            MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST -> R.drawable.all_media_icon
            MenuBarItem.MenuBarItemType.SEARCH -> R.drawable.search_icon
            MenuBarItem.MenuBarItemType.SETTING -> R.drawable.setting_icon
            MenuBarItem.MenuBarItemType.ALBUM_ITEM -> R.drawable.all_media_icon
        }
        val captionId = when(item.itemType){
            MenuBarItem.MenuBarItemType.SHOW_ALL -> R.string.menu_item_title_all
            MenuBarItem.MenuBarItemType.SHOW_PHOTO -> R.string.menu_item_title_photo
            MenuBarItem.MenuBarItemType.SHOW_MOVIE -> R.string.menu_item_title_movie
            MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST -> R.string.row_title_album
            MenuBarItem.MenuBarItemType.SEARCH -> R.string.menu_item_title_search
            MenuBarItem.MenuBarItemType.SETTING-> R.string.menu_item_title_setting
            MenuBarItem.MenuBarItemType.ALBUM_ITEM -> 0
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
        else if(_list[position].album != null){
            holder.binding.button.text = _list[position].album?.name
        }
        else{
            holder.binding.button.text = "null"
        }

        loadIcon?.invoke(item, 96, 96){
            it?.let {
                Log.d("debug", "loaded bmp :width=%d, height=%d".format(it.width, it.height))
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
    class MenuBarItemViewHolder internal constructor(bindingArg: MenuBarItemBinding)
        : RecyclerView.ViewHolder(bindingArg.root) {

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
                   if (focused) {
                        onFocus?.invoke(view, adapterPosition)
                    }
                }
            }

            buttonView.setOnKeyListener { view: View, _: Int, keyEvent: KeyEvent ->
                if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener onKeyDown?.invoke(
                        view,
                        adapterPosition,
                        keyEvent
                    ) == true
                }
                return@setOnKeyListener false
            }

            _binding = bindingArg
        }
    }
}
