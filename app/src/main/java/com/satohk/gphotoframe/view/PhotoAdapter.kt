package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.databinding.GridItemBinding
import android.util.Log
import android.view.KeyEvent
import com.satohk.gphotoframe.viewmodel.PhotoGridItem
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel

class PhotoAdapter internal constructor(private val _list: PhotoGridViewModel.PhotoGridItemList):
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
    var onClick: ((view:View?, position:Int) -> Unit)? = null
    var onFocus: ((view:View?, position:Int) -> Unit)? = null
    var loadThumbnail: ((photoGridItem: PhotoGridItem, width:Int?, height:Int?, callback:(bmp:Bitmap?)->Unit)->Unit)? = null
    private var _lastSize = 0

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(view)
    }

    // binds the data
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        Log.d("onBindViewHolder", "position=$position")
        holder.position = position
        holder.binding.imageView.setImageResource(R.drawable.default_background)
        holder.photoGridItem = _list[position]
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    fun notifyDataChange(){
        if (_list.size == 0) {
            Log.d("notifyItemRangeRemoved",
                "count:${_lastSize}")
            this.notifyItemRangeRemoved(0, _lastSize)
        } else {
            Log.d("notifyItemRangeInserted",
                "positionStart:${_lastSize}, itemCount:${_list.size - _lastSize}")
            notifyItemRangeInserted(_lastSize, _list.size - _lastSize)
        }
        _lastSize = _list.size
    }

    // stores and recycles views as they are scrolled off screen
    class PhotoViewHolder internal constructor(binding: GridItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        internal var position: Int = 0
        private val _binding: GridItemBinding
        internal val binding: GridItemBinding get() = _binding
        private val _adapter: PhotoAdapter?
            get(){ return if( this.bindingAdapter!=null ) this.bindingAdapter as PhotoAdapter else null}

        var photoGridItem: PhotoGridItem? = null
            set(value){
                Log.d("photoGridItem", "position=${position}, photoGridItem=${value?.metadataRemote?.url}")
                value?.let {
                    _adapter?.loadThumbnail?.invoke(value, 256, 256) {
                        setImage(it)
                    }
                    binding.aiIcon.visibility =
                        if (value.metadataLocal.favorite)
                            View.VISIBLE
                        else
                            View.INVISIBLE

                    if(!value.metadataLocal.favorite && value.metadataTemp != null){
                        binding.textItemInfo.text = "%.2f".format(value.metadataTemp!!.aiScore)
                        binding.textItemInfo.visibility = View.VISIBLE
                    }
                    else{
                        binding.textItemInfo.visibility = View.INVISIBLE
                    }
                }
                field = value
            }

        init {
            itemView.setOnClickListener { view ->
                _adapter?.onClick?.invoke(view, position)
            }
            itemView.setOnFocusChangeListener { view, b ->
                if(b) {
                    view.setBackgroundColor(Color.WHITE)
                    Log.d("setonfocuschange", position.toString())
                    _adapter?.onFocus?.invoke(view, position)
                }
                else{
                    view.setBackgroundResource(R.color.default_background)
                }
            }
            itemView.setOnKeyListener { view: View, _: Int, keyEvent: KeyEvent ->
                if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener _adapter?.onKeyDown?.invoke(
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
