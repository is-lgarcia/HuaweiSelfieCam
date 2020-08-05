package com.luisg.huaweiselfiecam.face

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ImageButton
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLResultTrailer
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.huawei.hms.mlsdk.face.MLMaxSizeFaceTransactor
import com.luisg.huaweiselfiecam.R
import com.luisg.huaweiselfiecam.camera.LensEnginePreview
import com.luisg.huaweiselfiecam.overlay.GraphicOverlay
import com.luisg.huaweiselfiecam.overlay.LocalFaceGraphic
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException

class LiveFaceCameraActivity : AppCompatActivity(), View.OnClickListener {

    private var analyzer: MLFaceAnalyzer? = null
    private var mLensEngine: LensEngine? = null
    private var mPreview: LensEnginePreview? = null
    private var mOverlay : GraphicOverlay? = null
    private var lensType = LensEngine.FRONT_LENS
    private var detectMode = 0
    private var isFront = false
    private val smilingRate = 0.8f
    private var smilingPossibility = 0.95f
    private var safeToTakePicture = false
    private var restart: ImageButton? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_face_camera)

        if (savedInstanceState != null){
            lensType = savedInstanceState.getInt("lensType")
        }
        mPreview = findViewById(R.id.preview)

        val intent = this.intent
        try {
            detectMode = intent.getIntExtra("detec_mode",1)
        }catch (e: RuntimeException){
            Log.e("Error","No se obtuvo el codigo de detección")
        }

        mOverlay = findViewById(R.id.graphic_overlay)
        findViewById<View>(R.id.facingSwitch).setOnClickListener(this)
        restart = findViewById(R.id.restart)

        createFaceAnalyzer()
        createLensEngine()

    }

    override fun onResume() {
        super.onResume()
        startLensEngine()
    }

    override fun onPause() {
        super.onPause()
        mPreview!!.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mLensEngine != null) {
            mLensEngine!!.release()
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        outState.putInt("lensType", lensType)
        super.onSaveInstanceState(outState)
    }

    private fun createFaceAnalyzer() {
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1f)
            .setTracingAllowed(true)
            .create()
        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)
        if (detectMode == 1003) {
            val transactor =
                MLMaxSizeFaceTransactor.Creator(analyzer, object : MLResultTrailer<MLFace?>() {
                    override fun objectCreateCallback(
                        itemId: Int,
                        obj: MLFace?
                    ) {
                        mOverlay!!.clear()
                        if (obj == null) {
                            return
                        }
                        val faceGraphic = LocalFaceGraphic(
                            mOverlay!!,
                            obj,
                            this@LiveFaceCameraActivity
                        )
                        mOverlay!!.addGraphic(faceGraphic)
                        val emotion = obj.emotions
                        if (emotion.smilingProbability > smilingPossibility) {
                            safeToTakePicture = false
                            mHandler.sendEmptyMessage(TAKE_PHOTO)
                        }
                    }

                    override fun objectUpdateCallback(
                        var1: MLAnalyzer.Result<MLFace?>?,
                        obj: MLFace?
                    ) {
                        mOverlay!!.clear()
                        if (obj == null) {
                            return
                        }
                        val faceGraphic = LocalFaceGraphic(
                            mOverlay!!,
                            obj,
                            this@LiveFaceCameraActivity
                        )
                        mOverlay!!.addGraphic(faceGraphic)
                        val emotion = obj.emotions
                        if (emotion.smilingProbability > smilingPossibility && safeToTakePicture) {
                            safeToTakePicture = false
                            mHandler.sendEmptyMessage(TAKE_PHOTO)
                        }
                    }

                    override fun lostCallback(result: MLAnalyzer.Result<MLFace?>?) {
                        mOverlay!!.clear()
                    }

                    override fun completeCallback() {
                        mOverlay!!.clear()
                    }
                }).create()
            analyzer!!.setTransactor(transactor)
        } else{
        analyzer!!.setTransactor(object : MLAnalyzer.MLTransactor<MLFace> {
            override fun transactResult(result: MLAnalyzer.Result<MLFace>?) {
                val faceSparseArray = result!!.analyseList
                var flag = 0
                for (i in 0 until faceSparseArray.size()) {
                    val emotion = faceSparseArray.valueAt(i).emotions
                    if (emotion.smilingProbability > smilingPossibility) {
                        flag++
                    }
                }
                if (flag > faceSparseArray.size() * smilingRate && safeToTakePicture) {
                    safeToTakePicture = false
                    mHandler.sendEmptyMessage(TAKE_PHOTO)
                }
            }

            override fun destroy() {}

        })
    }
    }

    private fun createLensEngine() {
        val context: Context = this.applicationContext
        mLensEngine = LensEngine.Creator(context, analyzer).setLensType(lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(25.0f)
            .enableAutomaticFocus(true)
            .create()
    }

    private fun startLensEngine() {
        restart!!.visibility = View.GONE
        if (mLensEngine != null) {
            try {
                if (detectMode == 1003) {
                    mPreview!!.start(mLensEngine, mOverlay)
                } else {
                    mPreview!!.start(mLensEngine)
                }
                safeToTakePicture = true
            } catch (e: IOException) {
                mLensEngine!!.release()
                mLensEngine = null
            }
        }
    }

    fun startPreview(view: View?) {
        mPreview!!.release()
        createFaceAnalyzer()
        createLensEngine()
        startLensEngine()
    }

    override fun onClick(v: View?) {
        isFront = !isFront
        if (isFront) {
            lensType = LensEngine.FRONT_LENS
        } else {
            lensType = LensEngine.BACK_LENS
        }
        if (mLensEngine != null) {
            mLensEngine!!.close()
        }
        startPreview(v)
    }

    companion object {
        private const val STOP_PREVIEW = 1
        private const val TAKE_PHOTO = 2
    }

    fun stopPreview (){
        restart!!.visibility = View.VISIBLE
        if (mLensEngine != null){
            mLensEngine!!.release()
            safeToTakePicture = false
        }
        if (analyzer!=null){
            try {
                analyzer!!.stop()
            } catch (e: IOException){
                Log.e("ERROR","No se pudo parar la cámara")
            }
        }
    }

    private fun takePhoto(){
        mLensEngine!!.photograph(null,
        LensEngine.PhotographListener { bytes ->
            mHandler.sendEmptyMessage(STOP_PREVIEW)
            val bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.size)
            savedBitmapToGallery(bitmap)
        }
        )
    }

    private fun savedBitmapToGallery(bitmap: Bitmap): String{
        val appDir = File("/storage/emulated/0/DCIM/Camera")
        if (!appDir.exists()){
            val res: Boolean = appDir.mkdir()
            if (!res){
                Log.e("ERROR","No se pudo crear el directorio")
                return ""
            }
        }
        val fileName = "HuaweiSelfieCamera" + System.currentTimeMillis() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100,fos)
            fos.flush()
            fos.close()
            val uri: Uri = Uri.fromFile(file)
            this.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri))
        }catch (e: IOException){
            e.printStackTrace()
        }
        return file.absolutePath
    }

    private val mHandler: Handler = object : Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                STOP_PREVIEW -> stopPreview()
                TAKE_PHOTO -> takePhoto()
                else -> {

                }
            }
        }
    }
}