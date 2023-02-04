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

package com.satohk.fjphoto.domain

import android.graphics.Bitmap
import android.util.Size
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class InferenceModel(
        modelLoader: InferenceModelLoader
    ) {
    private val tflite = modelLoader.tflite

    private val tfImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(CastOp(DataType.FLOAT32))
            .add(ResizeOp(
                tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(0f, 255f))
            .build()
    }

    val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    val tfOutputSize by lazy {
        val outputShape = tflite.getOutputTensor(0).shape()
        outputShape[0] * outputShape[1]
    }



    fun predict(bitmap: Bitmap): FloatArray{
        val tfImageBuffer = TensorImage(DataType.UINT8)
        tfImageBuffer.load(bitmap)
        val tfImage =  tfImageProcessor.process(tfImageBuffer)
        val output = TensorBuffer.createFixedSize(intArrayOf(1, 1024), DataType.FLOAT32)
        tflite.run(tfImage.buffer, output.buffer)
        return output.floatArray
    }

    companion object {
        const val OBJECT_COUNT = 10
        private const val ACCURACY_THRESHOLD = 0.5f
    }
}