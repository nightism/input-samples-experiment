package com.example.lowlatencysample.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.platform.LocalContext

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            Button(onClick = {
                context.startActivity(Intent(context, SampleLowLatencyViewActivity::class.java))
            }) {
                Text(text = "Go to Low Latency Activity")
            }

            Button(onClick = {
                context.startActivity(Intent(context, SampleLowLatencyViewActivity::class.java))
            }) {
                Text(text = "Go to Ink Activity")
            }
        }
    }

}