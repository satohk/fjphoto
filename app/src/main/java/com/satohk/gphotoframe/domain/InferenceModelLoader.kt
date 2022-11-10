package com.satohk.gphotoframe.domain

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil

class InferenceModelLoader(context: Context) {
    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    private val labels by lazy {
        FileUtil.loadLabels(context, LABELS_PATH)
    }

    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(context, MODEL_PATH),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }

    fun close(){
        tflite.close()
        nnApiDelegate.close()
    }

    companion object {
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}