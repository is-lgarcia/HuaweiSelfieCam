package com.luisg.huaweiselfiecam.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.luisg.huaweiselfiecam.R
import com.luisg.huaweiselfiecam.auth.AuthActivity
import com.luisg.huaweiselfiecam.face.LiveFaceCameraActivity
import kotlinx.android.synthetic.main.activity_home.*
import java.lang.Exception
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_RESQUEST = 1
        private fun isPermissionGranted(
            context: Context,
            permission: String?
        ): Boolean{
            if (ContextCompat.checkSelfPermission(context, permission!!) == PackageManager.PERMISSION_GRANTED){
                return true
            }
            return false
        }
    }

    private val requiredPermission: Array<String?>
    get() = try {
        val info = this.packageManager
            .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
        val ps = info.requestedPermissions
        if (ps != null && ps.isNotEmpty()){
            ps
        } else {
            arrayOfNulls(0)
        }
    }catch (e: RuntimeException){
        throw e
    } catch (e: Exception){
        arrayOfNulls(0)
    }

    private fun allPermisionGranted(): Boolean {
        for (permission in requiredPermission){
            if (!isPermissionGranted(this, permission)){
                return false
            }
        }
        return true
    }

    private val runtimePermission: Unit
    get(){
        val allPermission: MutableList<String?> = ArrayList()
        for (permission in requiredPermission){
            if (!isPermissionGranted(this, permission)){
                allPermission.add(permission)
            }
            if (allPermission.isNotEmpty()){
                ActivityCompat.requestPermissions(
                    this,
                    allPermission.toTypedArray(),
                    PERMISSION_RESQUEST
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_RESQUEST){
            return
        }
        var isNeedShowDialog = false
        for (i in permissions.indices){
            if (permissions[i] == Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[i]
            != PackageManager.PERMISSION_GRANTED){
                isNeedShowDialog = true
            }
        }
        if (isNeedShowDialog && !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            val dialog: AlertDialog = AlertDialog.Builder(this)
                .setMessage("Esta aplicación requiere acceso a tu carpeta de medios y a tu camara para poder funcionar")
                .setPositiveButton("Configuración") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, 200)
                    startActivity(intent)
                }.setNegativeButton("Cancel")
                { _, _ -> finish() }.create()
            dialog.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (!allPermisionGranted()){
            runtimePermission
        }

        btnLogout.setOnClickListener {
            logOutWithHuaweiId()
        }

        btnMostPeople.setOnClickListener {
            val intent = Intent(this, LiveFaceCameraActivity::class.java)
            intent.putExtra("detec_mode",1002)
            startActivity(intent)
        }

        btnNearestPeople.setOnClickListener {
            val intent = Intent(this, LiveFaceCameraActivity::class.java)
            intent.putExtra("detec_mode",1003)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        //No haga nada
    }

    private fun logOutWithHuaweiId(){
        val mAthParams = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .createParams()
        val mAuthManager = HuaweiIdAuthManager.getService(this, mAthParams)
        val logoutTask = mAuthManager.signOut()
        logoutTask.addOnSuccessListener {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
        logoutTask.addOnFailureListener {
            Toast.makeText(this, "El Logout falló",Toast.LENGTH_SHORT).show()
        }
    }
}