package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel.PhotoGridItem
import com.satohk.gphotoframe.databinding.GridItemBinding
import android.util.Log
import android.view.KeyEvent
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel

class PhotoAdapter internal constructor(private val _list: List<PhotoGridItem>):
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
        holder.position = position
        holder.binding.imageView.setImageResource(R.drawable.default_background)
        holder.photoGridItem = _list[position]
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    // stores and recycles views as they are scrolled off screen
    class PhotoViewHolder internal constructor(binding: GridItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        internal var position: Int = 0
        private val _binding: GridItemBinding
        internal val binding: GridItemBinding get() = _binding
        private val _adapter: PhotoAdapter
            get(){ return this.bindingAdapter as PhotoAdapter }

        var photoGridItem: PhotoGridItem? = null
            set(value){
                value?.let {
                    if (field == null || (field!!.photoMetaData.metadataRemote.id != value.photoMetaData.metadataRemote.id)) {
                        _adapter.loadThumbnail?.invoke(value, 256, 256) {
                            setImage(it)
                        }
                    }
                    binding.aiIcon.visibility =
                        if (value.photoMetaData.metadataLocal.favorite)
                            View.VISIBLE
                        else
                            View.INVISIBLE
                }
                field = value
            }

        init {
            itemView.setOnClickListener { view ->
                _adapter.onClick?.invoke(view, position)
            }
            itemView.setOnFocusChangeListener { view, b ->
                if(b) {
                    view.setBackgroundColor(Color.WHITE)
                    Log.d("setonfocuschange", position.toString())
                    _adapter.onFocus?.invoke(view, position)
                }
                else{
                    view.setBackgroundResource(R.color.default_background)
                }
            }
            itemView.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
                if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener _adapter.onKeyDown?.invoke(
                        view,
                        position,
                        keyEvent
                    ) == true
                }
                return@setOnKeyListener false
            }
            _binding = binding
        }

        private fun setImage(bitmap: Bitmap?){
            if(bitmap != null) {
                binding.imageView.setImageBitmap(bitmap)
            }
        }
    }
}
