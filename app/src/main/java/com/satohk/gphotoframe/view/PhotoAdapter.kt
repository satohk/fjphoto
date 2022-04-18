package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel.PhotoGridItem
import com.satohk.gphotoframe.databinding.GridItemBinding
import android.util.Log
import android.view.KeyEvent
import com.satohk.gphotoframe.viewmodel.MenuBarItem
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.*

class PhotoAdapter internal constructor(private val _list: List<PhotoGridViewModel.PhotoGridItem>):
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
    var onClick: ((view:View?, position:Int) -> Unit)? = null
    var onFocus: ((view:View?, position:Int) -> Unit)? = null
    var loadThumbnail: ((photoGridItem: PhotoGridItem, width:Int?, height:Int?, callback:(bmp:Bitmap?)->Unit)->Unit)? = null

    var viewHolders: MutableList<PhotoViewHolder> = mutableListOf()
        private set

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = PhotoViewHolder(view)
        viewHolders.add(viewHolder)
        return viewHolder
    }

    // binds the data
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder._position = position
        holder.onClick = this.onClick
        holder.onKeyDown = this.onKeyDown
        holder.onFocus = this.onFocus
        holder.binding.imageView.setImageResource(R.drawable.default_background)
        loadThumbnail?.invoke(_list[position], 256, 256){
            holder.setImage(it)
        }
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    // stores and recycles views as they are scrolled off screen
    class PhotoViewHolder internal constructor(binding: GridItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
        var onClick: ((view:View?, position:Int) -> Unit)? = null
        var onFocus: ((view:View?, position:Int) -> Unit)? = null

        internal var _position: Int = 0
        private val _binding: GridItemBinding
        internal val binding: GridItemBinding get() = _binding

        init {
            itemView.setOnClickListener { view ->
                onClick?.invoke(view, _position)
            }
            itemView.setOnFocusChangeListener { view, b ->
                if(b) {
                    view.setBackgroundColor(Color.WHITE)
                    Log.d("setonfocuschange", _position.toString())
                    onFocus?.invoke(view, _position)
                }
                else{
                    view.setBackgroundResource(R.color.default_background)
                }
            }
            itemView.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
                if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener onKeyDown?.invoke(
                        view,
                        _position,
                        keyEvent
                    ) == true
                }
                return@setOnKeyListener false
            }
            _binding = binding
        }

        fun setImage(bitmap: Bitmap?){
            if(bitmap != null) {
                binding.imageView.setImageBitmap(bitmap)
            }
        }
    }
}
