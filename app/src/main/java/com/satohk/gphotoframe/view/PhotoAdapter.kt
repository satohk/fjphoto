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
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.*

class PhotoAdapter internal constructor(private val _list: List<PhotoGridViewModel.PhotoGridItem>,
                                        private val _viewModel: PhotoGridViewModel):
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
    var onClick: ((view:View?, position:Int) -> Unit)? = null
    var onFocus: ((view:View?, position:Int) -> Unit)? = null

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(view)
    }

    // binds the data
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val item: PhotoGridItem = _list[position]
        holder.adapterPosition = position
        holder.onClick = this.onClick
        holder.onKeyDown = this.onKeyDown
        holder.onFocus = this.onFocus
        holder.binding.imageView.setImageResource(R.drawable.default_background)

        // load image
        GlobalScope.launch(Dispatchers.Main){
            var bmp: Bitmap?
            Log.d("debug", "url=%s".format(item.photoMetaData.url))
            withContext(Dispatchers.IO) {
                bmp = _viewModel.loadThumbnail(item, holder.binding.imageView.width, holder.binding.imageView.height)
            }
            if(bmp != null){
                holder.binding.imageView.setImageBitmap(bmp)
            }
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

        private val _binding: GridItemBinding
        internal val binding: GridItemBinding get() = _binding
        internal var adapterPosition: Int = 0

        init {
            itemView.setOnClickListener { view ->
                onClick?.invoke(view, adapterPosition)
            }
            itemView.setOnFocusChangeListener { view, b ->
                if(b) {
                    view.setBackgroundColor(Color.WHITE)
                    Log.d("setonfocuschange", adapterPosition.toString())
                    onFocus?.invoke(view, adapterPosition)
                }
                else{
                    view.setBackgroundResource(R.color.default_background)
                }
            }
            itemView.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
                if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener onKeyDown?.invoke(
                        view,
                        adapterPosition,
                        keyEvent
                    ) == true
                }
                return@setOnKeyListener false
            }
            _binding = binding
        }
    }
}
