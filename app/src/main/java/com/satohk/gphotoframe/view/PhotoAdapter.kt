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
import com.squareup.picasso.Picasso

class PhotoAdapter internal constructor(private val _listener: ItemClickListener) :
    ListAdapter<PhotoGridItem, PhotoAdapter.PhotoViewHolder>(PhotoGridItem.DIFF_UTIL) {

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(view, _listener)
    }

    // binds the data
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val url: Uri = getItem(position).uri
        holder.adapterPosition = position
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.photo_icon)
            .into(holder.binding.imageView)
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    // stores and recycles views as they are scrolled off screen
    class PhotoViewHolder internal constructor(binding: GridItemBinding, listener: PhotoAdapter.ItemClickListener)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val _listener: PhotoAdapter.ItemClickListener
        private val _binding: GridItemBinding
        internal val binding: GridItemBinding get() = _binding
        internal var adapterPosition: Int = 0

        override fun onClick(view: View?) {
            if (_listener != null) _listener!!.onItemClick(view, adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
            itemView.setOnFocusChangeListener { view, b ->
                if(b){
                    view.setBackgroundColor(Color.WHITE)
                }
                else{
                    view.setBackgroundResource(R.color.default_background)
                }
            }
            _listener = listener
            _binding = binding
        }
    }
}
