package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
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

class MenuBarItemAdapter internal constructor(private val _listener: ItemClickListener,
                                              private val _viewModel: MenuBarViewModel) :
    ListAdapter<MenuBarItem, MenuBarItemAdapter.MenuBarItemViewHolder>(MenuBarItem.DIFF_UTIL) {

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuBarItemViewHolder {
        val view = MenuBarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuBarItemViewHolder(view, _listener)
    }

    // binds the data
    override fun onBindViewHolder(holder: MenuBarItemViewHolder, position: Int) {
        val item:MenuBarItem = getItem(position)

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
            holder.binding.button.text = getItem(position).caption
        }

        // load album cover icon
        GlobalScope.launch(Dispatchers.Main){
            var bmp: Bitmap?
            withContext(Dispatchers.IO) {
                bmp = _viewModel.loadIcon(item, 128, 128)
            }
            Log.d("debug", bmp.toString())
            if(bmp != null){
                bmp = centerCropBitmap(bmp!!)
                val drawable = BitmapDrawable(bmp)
                holder.binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                holder.binding.button.setCompoundDrawablePadding(20)
            }
        }
    }

    private fun centerCropBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width == bitmap.height) {
            return bitmap
        }
        if (bitmap.width > bitmap.height) {
            val leftOffset = (bitmap.width - bitmap.height) / 2
            return Bitmap.createBitmap(
                bitmap,
                leftOffset,
                0,
                bitmap.height,
                bitmap.height,
                null,
                true
            )
        }
        val topOffset = (bitmap.height - bitmap.width) / 2
        return Bitmap.createBitmap(
            bitmap,
            0,
            topOffset,
            bitmap.width,
            bitmap.width,
            null,
            true
        )
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }


    // stores and recycles views as they are scrolled off screen
    class MenuBarItemViewHolder internal constructor(binding: MenuBarItemBinding, listener: MenuBarItemAdapter.ItemClickListener)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val _listener: MenuBarItemAdapter.ItemClickListener
        private val _binding: MenuBarItemBinding
        internal val binding: MenuBarItemBinding get() = _binding
        internal var adapterPosition: Int = 0

        override fun onClick(view: View?) {
            if (_listener != null) _listener!!.onItemClick(view, adapterPosition)
        }

        init {
            val buttonView = itemView.findViewById<TextView>(R.id.button)
            buttonView.setOnClickListener(this)
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
            _listener = listener
            _binding = binding
        }
    }
}
