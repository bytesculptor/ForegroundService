/*
 * Copyright (c) 2024 Byte Sculptor Software - All Rights Reserved
 *
 * All information contained herein is and remains the property of Byte Sculptor Software.
 * Unauthorized copying of this file, via any medium, is strictly prohibited unless prior
 * written permission is obtained from Byte Sculptor Software.
 *
 * bytesculptor@gmail.com
 *
 */

package dev.bytesculptor.foregroundservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import dev.bytesculptor.foregroundservice.service.MyForegroundService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                this.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    123
                )
            }
        }

        val serviceIntent = Intent(this, MyForegroundService::class.java)
        try {
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.d(TAG, "onCreate: start service failed")
        }
    }

    companion object {
        val TAG = MainActivity::class.simpleName
    }
}