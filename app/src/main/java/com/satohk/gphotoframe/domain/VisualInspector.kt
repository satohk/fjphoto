/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.satohk.gphotoframe.domain

import android.graphics.Bitmap
import android.util.Size


class VisualInspector(
        private val _model: InferenceModel
    ) {

    private lateinit var _featureMean: FloatArray
    private lateinit var _featureStd: FloatArray
    private var _anchorDataFeature = mutableListOf<FloatArray>()

    val inputImageSize: Size get() = _model.tfInputSize

    fun calcWholeFeatures(imageList: List<Bitmap>){
        _featureMean = FloatArray(_model.tfOutputSize) { 0.0f }
        _featureStd = FloatArray(_model.tfOutputSize) { 0.0f }

        val resList = mutableListOf<FloatArray>()
        for(bmp in imageList){
            val res = _model.predict(bmp)

            for(i in _featureMean.indices){
                _featureMean[i] += res[i]
            }
            resList.add(res)
        }

        for(i in _featureMean.indices){
            _featureMean[i] = _featureMean[i] / imageList.size
            for(j in 0 until resList.size) {
                val a = resList[j][i] - _featureMean[i]
                _featureStd[i] += (a * a)
            }
            _featureStd[i] = _featureStd[i] / imageList.size
        }
    }

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
            val dist = calcDistance(feature, anchorFeature, _featureStd)
            minDist = Math.min(minDist, dist)
        }

        return minDist / 4
    }

    private fun calcDistance(feature1: FloatArray, feature2: FloatArray, featureWeight: FloatArray):Float{
        var d:Float = 0.0f

        for(i in feature1.indices){
            val dd = (feature1[i] - feature2[i]) / featureWeight[i]
            d += (dd * dd)
        }
        return Math.sqrt(d.toDouble()).toFloat()
    }
}