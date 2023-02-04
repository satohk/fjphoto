// Ref: https://codeutility.org/android-recyclerview-items-lose-focus-stack-overflow-2/

package com.satohk.fjphoto.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

class FocusFixConstraintLayout : ConstraintLayout {
    constructor(context: Context)
            : super(context)
    constructor(context: Context, attr: AttributeSet)
            : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int)
            : super(context, attr, defStyleAttr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attr, defStyleAttr, defStyleRes)

    override fun clearFocus(){
        if(this.parent != null){
            super.clearFocus()
        }
    }
}
