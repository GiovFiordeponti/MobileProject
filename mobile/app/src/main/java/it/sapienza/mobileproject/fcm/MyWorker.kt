package it.sapienza.mobileproject.fcm

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.entities.room.AppDatabase
import it.sapienza.mobileproject.entities.room.entity.PongNotification


class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val database = AppDatabase.getInstance(applicationContext)

    override fun doWork(): ListenableWorker.Result {
        val json = inputData.getString("json")

        Log.d(TAG, "Performing long running task in scheduled job with string $json")
        val notification = Gson().fromJson<PongNotification>(json, PongNotification::class.java)
        when(notification.type){
            Constants.REQUEST_TYPE_GAME, Constants.REQUEST_TYPE_FRIEND, Constants.REQUEST_TYPE_ACCEPT_GAME -> {
                Log.d(TAG, "Performing notification insert $json")
                database.pongNotificationDao().insertNotifications(notification)
            }
            Constants.REQUEST_TYPE_ACCEPT_FRIEND -> {
                Log.d(TAG, "Performing friend insert $json")
                database.pongNotificationDao().insertFriendsUnsuspended(notification.user!!)
            }
        }

        sendMessageToActivity(json)

        return ListenableWorker.Result.success()
    }

    private fun sendMessageToActivity(msg: String?) {
        val intent = Intent("intentKey")
        // You can also include some extra data.
        intent.putExtra("key", msg)
        Log.d(TAG, "Sending msg $msg to activity")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }


    companion object {
        private const val TAG = "MyWorker"
    }
}