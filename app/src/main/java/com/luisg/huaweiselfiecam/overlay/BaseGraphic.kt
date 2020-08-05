package com.luisg.huaweiselfiecam.overlay

import android.graphics.Canvas
import com.huawei.hms.mlsdk.common.LensEngine

abstract class BaseGraphic(
    private val graphic: GraphicOverlay
) {
    abstract fun draw(canvas: Canvas?)

    fun scaleX(x: Float): Float{
        return x * graphic.widthScaleValue
    }

    fun scaleY(y: Float): Float{
        return y * graphic.heightScaleValue
    }

    fun translateX(x: Float): Float {
        return if (graphic.cameraFacing == LensEngine.FRONT_LENS){
            graphic.width - scaleX(x)
        } else {
            scaleX(x)
        }
    }

    fun translateY(y: Float): Float {
       return scaleY(y)
    }

}