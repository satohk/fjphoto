package com.satohk.gphotoframe.domain

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil

class InferenceModelLoader(context: Context) {
    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(context, MODEL_PATH),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }

    fun close(){
        tflite.close()
        nnApiDelegate.close()
    }

    companion object {
        private const val MODEL_PATH = "lite-model_imagenet_mobilenet_v3_small_100_224_feature_vector_5_default_1.tflite"
    }
}