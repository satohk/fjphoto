package com.satohk.gphotoframe.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel.PhotoGridItem
import com.satohk.gphotoframe.databinding.GridItemBinding
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import com.squareup.picasso.Picasso

class PhotoAdapter :
    ListAdapter<PhotoGridItem, PhotoAdapter.PhotoViewHolder>(PhotoGridItem.DIFF_UTIL) {

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
        val url: Uri = getItem(position).uri
        holder.adapterPosition = position
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.default_background)
            .into(holder.binding.imageView)

        holder.onClick = this.onClick
        holder.onKeyDown = this.onKeyDown
        holder.onFocus = this.onFocus
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
                if(b){
                    view.setBackgroundColor(Color.WHITE)
                    Log.d("setonfocuschange", adapterPosition.toString())
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
