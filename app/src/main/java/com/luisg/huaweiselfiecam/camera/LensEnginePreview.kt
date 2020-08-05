package com.luisg.huaweiselfiecam.camera

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.huawei.hms.common.size.Size
import com.huawei.hms.mlsdk.common.LensEngine
import com.luisg.huaweiselfiecam.overlay.GraphicOverlay
import java.io.IOException

class LensEnginePreview(context: Context, atts: AttributeSet?)
    : ViewGroup(context, atts) {

    private val mContext: Context = context
    private val mSurfaceView: SurfaceView
    private var mStartRequest: Boolean
    private var mSurfaceAvailable: Boolean
    private var mLensEngine: LensEngine? = null
    private var mOverlay: GraphicOverlay? = null

    init {
        mStartRequest = false
        mSurfaceAvailable = false
        mSurfaceView = SurfaceView(context)
        mSurfaceView.holder.addCallback(SurfaceCallback())
        this.addView(mSurfaceView)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        var previewWidth = 320
        var previewHeight = 240
        if (mLensEngine != null) {
            val size: Size? = mLensEngine!!.displayDimension
            if (size != null) {
                previewHeight = size.height
                previewWidth = size.width
            }
        }
        if (mContext.resources
                .configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            val tmp = previewWidth
            previewWidth = previewHeight
            previewHeight = tmp
        }

        val viewWidth = right - left
        val viewHeight = bottom - top
        val childWidth: Int
        val childHeight: Int
        var childXOffset = 0
        var childYOffset = 0
        val widthRatio = viewWidth.toFloat() / previewWidth.toFloat()
        val heightRation = viewHeight.toFloat() / previewHeight.toFloat()

        if (widthRatio > heightRation) {
            childWidth = viewWidth
            childHeight = (previewHeight.toFloat() * heightRation).toInt()
            childYOffset = (childHeight - viewHeight) / 2
        } else {
            childWidth = (previewWidth.toFloat() * heightRation).toInt()
            childHeight = viewHeight
            childXOffset = (childWidth - viewWidth) / 2
        }
        for (i in 0 until this.childCount) {
            getChildAt(i).layout(
                -1 * childXOffset, -1 * childYOffset, childWidth - childXOffset,
                childHeight - childYOffset
            )
        }
        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e("Error", "No se pudo iniciar la camara")
        }
    }

    @Throws(IOException::class)
    fun start(lensEngine: LensEngine?, overlay: GraphicOverlay?){
        mOverlay = overlay
        start(lensEngine)
    }

    fun start (lensEngine: LensEngine?){
        lensEngine?.let {
            mLensEngine = it
            mStartRequest = true
            startIfReady()
        }?: kotlin.run {
            stop()
        }
    }

    fun stop() = mLensEngine?.close()

    fun release(){
        mLensEngine?.release()
        mLensEngine = null
    }

    @Throws(IOException::class)
    fun startIfReady() {
        if (mStartRequest && mSurfaceAvailable) {
            mLensEngine!!.run(mSurfaceView.holder)
            if (mOverlay != null) {
                val size: Size = mLensEngine!!.displayDimension
                val min: Int = size.width.coerceAtMost(size.height)
                val max: Int = size.width.coerceAtLeast((size.height))
                if (Configuration.ORIENTATION_PORTRAIT == mContext.resources.configuration.orientation) {
                    mOverlay!!.setCameraInfo(min, max, mLensEngine!!.lensType)
                } else {
                    mOverlay!!.setCameraInfo(max, min, mLensEngine!!.lensType)
                }
                mOverlay!!.clear()
            }
            mStartRequest = false
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {

        override fun surfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            mSurfaceAvailable = false
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            mSurfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e("Error: ", "No pudimos iniciar la camra $e")
            }
        }
    }

}