package com.example.lowlatencysample.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            Column (modifier = Modifier.padding(16.dp)) {
                Button(onClick = {
                    context.startActivity(Intent(context, SampleLowLatencyViewActivity::class.java))
                }) {
                    Text(text = "Go to Low Latency Activity")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    context.startActivity(Intent(context, SampleInkViewActivity::class.java))
                }) {
                    Text(text = "Go to Ink Activity")
                }
            }
        }
    }

}