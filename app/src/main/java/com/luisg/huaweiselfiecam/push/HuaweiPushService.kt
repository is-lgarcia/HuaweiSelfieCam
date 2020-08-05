package com.luisg.huaweiselfiecam.push

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import java.lang.Exception
import kotlin.concurrent.thread

class HuaweiPushService: HmsMessageService() {

    companion object {
        private const val TAG = "HuaweiPushService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje Recibido")
        remoteMessage?.let {
            Log.d(TAG, " - Data ${it.data}")

            Log.i(TAG, "getCollapseKey: " + remoteMessage.collapseKey
                    + "\n getData: " + remoteMessage.data
                    + "\n getFrom: " + remoteMessage.from
                    + "\n getTo: " + remoteMessage.to
                    + "\n getMessageId: " + remoteMessage.messageId
                    + "\n getOriginalUrgency: " + remoteMessage.originalUrgency
                    + "\n getUrgency: " + remoteMessage.urgency
                    + "\n getSendTime: " + remoteMessage.sentTime
                    + "\n getMessageType: " + remoteMessage.messageType
                    + "\n getTtl: " + remoteMessage.ttl
            );

        }
    }

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Log.d(TAG, "Huawei push token: $token")

        if (!TextUtils.isEmpty(token)) {
            // This method callback must be completed in 10 seconds. Otherwise, you need to start a new Job for callback processing.
            refreshedTokenToServer(token);
        }

    }

    private fun refreshedTokenToServer(token: String?) {
        Log.i(TAG, "sending token to server. token:" + token);
    }

}

class GetTokenAction() {
    private val handle: Handler = Handler(Looper.getMainLooper())
    fun getToken(context: Context, callback: (String) -> Unit) {
        thread {
            try {
                val appID = AGConnectServicesConfig.fromContext(context).getString("client/app_id")
                val token = HmsInstanceId.getInstance(context).getToken(appID, "HCM")
                handle.post { callback(token) }
            } catch (err: Exception) {
                Log.e("Error: ", err.toString())
            }
        }
    }
}