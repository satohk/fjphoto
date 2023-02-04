package com.satohk.fjphoto.domain

import android.graphics.Bitmap
import android.util.Size
import kotlin.math.min
import kotlin.math.sqrt


class VisualInspector(
        private val _model: InferenceModel
    ) {
    private var _anchorDataFeature = mutableListOf<FloatArray>()

    val inputImageSize: Size get() = _model.tfInputSize

    fun setAnchorImage(images: List<Bitmap>){
        _anchorDataFeature = mutableListOf()

        for(image in images){
            val feature = _model.predict(image)
            _anchorDataFeature.add(feature)
        }
    }

    fun calcImageScore(image: Bitmap): Float{
        val feature = _model.predict(image)
        var minDist = Float.MAX_VALUE

        for(anchorFeature in _anchorDataFeature){
            val dist = calcDistance(feature, anchorFeature)
            minDist = min(minDist, dist)
        }

        val scoreMax = 0.1f    // experimentally determined
        return (1.0f / (minDist + 0.000001f)) / scoreMax
    }

    private fun calcDistance(feature1: FloatArray, feature2: FloatArray): Float {
        var d: Float = 0.0f

        for (i in feature1.indices) {
            val weight = 1 //Math.max(feature1[i], 0.5f)
            val dd = (feature1[i] - feature2[i]) * weight
            d += (dd * dd)
        }
        return sqrt(d.toDouble()).toFloat()
    }
}