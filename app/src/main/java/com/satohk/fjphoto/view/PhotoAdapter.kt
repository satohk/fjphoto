package com.satohk.fjphoto.view

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.satohk.fjphoto.R
import com.satohk.fjphoto.databinding.GridItemBinding
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.satohk.fjphoto.viewmodel.PhotoGridItem
import com.satohk.fjphoto.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.locks.Condition

class PhotoAdapter internal constructor(private val _list: PhotoGridViewModel.PhotoGridItemList):
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    var onKeyDown: ((view:View?, position:Int, keyEvent: KeyEvent) -> Boolean)? = null
    var onClick: ((view:View?, position:Int) -> Unit)? = null
    var onFocus: ((view:View?, position:Int) -> Unit)? = null
    var loadThumbnail: ((photoGridItem: PhotoGridItem, width:Int?, height:Int?, position:Int, callback:(position:Int, bmp:Bitmap?)->Unit)->Unit)? = null
    private var _lastSize = 0
    private var _nextSize = 0
    private var _holderCount = 0

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        Log.d("PhotoAdapter", "onCreateViewHolder  ct=$_holderCount")
        _holderCount++
        val view = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(view)
    }

    // binds the data
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        Log.d("onBindViewHolder", "new pos=$position  holder.position=${holder.position} bindingPos=${holder.bindingAdapterPosition} absPos=${holder.absoluteAdapterPosition}")
        holder.binding.imageView.setImageResource(R.drawable.default_background)
        holder.photoGridItem = _list[position]
    }

    override fun getItemCount(): Int {
        return _nextSize
    }

    fun notifyDataChange(){
        // _list.size may change in other thread, use the saved value while updating the view
        _nextSize = _list.size
        if (_nextSize == 0) {
            Log.d("notifyItemRangeRemoved",
                "count:${_lastSize}")
            this.notifyItemRangeRemoved(0, _lastSize)
        }
        else if(_nextSize < _lastSize){
            Log.d("notifyItemRangeRemoved",
                "count:${_lastSize}")
            this.notifyItemRangeRemoved(0, _lastSize)
            Log.d("notifyItemRangeInserted",
                "positionStart:0, itemCount:${_nextSize}")
            this.notifyItemRangeInserted(0, _nextSize)
        }
        else {
            Log.d("notifyItemRangeInserted",
                "positionStart:${_lastSize}, itemCount:${_nextSize - _lastSize}")
            notifyItemRangeInserted(_lastSize, _nextSize - _lastSize)
        }
        _lastSize = _nextSize
    }

    // stores and recycles views as they are scrolled off screen
    class PhotoViewHolder internal constructor(binding: GridItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        private val _binding: GridItemBinding
        internal val binding: GridItemBinding get() = _binding
        private val _adapter: PhotoAdapter?
            get(){ return if( this.bindingAdapter!=null ) this.bindingAdapter as PhotoAdapter else null}
        internal var loadState: LoadState = LoadState.BEFORE_LOAD
        enum class LoadState {
            BEFORE_LOAD,
            LOADING,
            LOADED,
        }

        var photoGridItem: PhotoGridItem? = null
            set(value){
                Log.d("photoGridItem", "position=${bindingAdapterPosition}, photoGridItem=${value?.metadataRemote?.url}")
                value?.let {
                    loadState = LoadState.LOADING
                    _adapter?.loadThumbnail?.invoke(value, 256, 256, bindingAdapterPosition) { position:Int, bmp:Bitmap? ->
                        // loadThumbnail実行時とPositionが変わっていない場合のみ画像を適用
                        if(position == bindingAdapterPosition) {
                            setImage(bmp)
                            loadState = LoadState.LOADED
                        }
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

                    //test
                    binding.textItemInfo.text = "pos=$position"
                    binding.textItemInfo.visibility = View.VISIBLE
                }
                field = value
            }

        init {
            itemView.setOnClickListener { view ->
                _adapter?.onClick?.invoke(view, position)
            }
            itemView.setOnFocusChangeListener { view, b ->
                if(b) {
                    itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                        view.setBackgroundColor(Color.WHITE)
                        _adapter?.onFocus?.invoke(view, position)
                    }
                    Log.d("PhotoViewHolder", "setonfocuschange ${position.toString()}")
                }
                else{
                    Log.d("PhotoViewHolder", "setonfocuschange reset ${position.toString()}")
                    //itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                    view.setBackgroundResource(R.color.default_background)
                    //}
                }
            }
            itemView.setOnKeyListener { view: View, _: Int, keyEvent: KeyEvent ->
                Log.d("PhotoViewHolder", "keylistener ${position.toString()}")
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
