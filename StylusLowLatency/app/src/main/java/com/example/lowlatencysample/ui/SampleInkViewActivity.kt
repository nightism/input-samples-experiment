package com.example.lowlatencysample.ui

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.UiThread
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

class SampleInkViewActivity : ComponentActivity(), InProgressStrokesFinishedListener {
    private lateinit var inProgressStrokesView: InProgressStrokesView
    private val finishedStrokesState = mutableStateOf(emptyMap<InProgressStrokeId, Stroke>())

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inProgressStrokesView = InProgressStrokesView(this)
        inProgressStrokesView.addFinishedStrokesListener(this)
        inProgressStrokesView.handoffDebounceTimeMs = 0

        setContent {
            Column {
                Row {
                    Button(onClick = {
                        finish()
                    }) {
                        Text("Back to Home Activity")
                    }
                    Button(onClick = {
                        val windowInsetsController =
                            WindowCompat.getInsetsController(window, window.decorView)
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    }) {
                        Text("Show Nav Bar")
                    }
                    Button(onClick = {
                        val windowInsetsController =
                            WindowCompat.getInsetsController(window, window.decorView)
                        windowInsetsController.show(
                            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.captionBar()
                        )
                        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                    }) {
                        Text("Hide Nav Bar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                DrawingSurface(
                    inProgressStrokesView = inProgressStrokesView,
                    finishedStrokesState = finishedStrokesState
                )
            }
        }
    }

    @UiThread
    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finishedStrokesState.value += strokes
//        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun DrawingSurface(
    inProgressStrokesView: InProgressStrokesView,
    finishedStrokesState: MutableState<Map<InProgressStrokeId, Stroke>>
) {
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }
    val defaultBrush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePenLatest,
        colorIntArgb = Color.Black.toArgb(),
        size = 0.5F,
        epsilon = 0.1F
    )

    val finishBrush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePenLatest,
        colorIntArgb = Color.Red.toArgb(),
        size = 0.5F,
        epsilon = 0.1F
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = FrameLayout(context)
                inProgressStrokesView.apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                }
                val predictor = MotionEventPredictor.newInstance(rootView)
                val touchListener =
                    View.OnTouchListener { view, event ->
                        predictor.record(event)
                        val predictedEvent = predictor.predict()

                        try {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    // First pointer - treat it as inking.
                                    view.requestUnbufferedDispatch(event)
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    currentPointerId.value = pointerId
                                    currentStrokeId.value =
                                        inProgressStrokesView.startStroke(
                                            event = event,
                                            pointerId = pointerId,
                                            brush = defaultBrush
                                        )
                                    true
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    val pointerId = checkNotNull(currentPointerId.value)
                                    val strokeId = checkNotNull(currentStrokeId.value)

                                    for (pointerIndex in 0 until event.pointerCount) {
                                        if (event.getPointerId(pointerIndex) != pointerId) continue
                                        inProgressStrokesView.addToStroke(
                                            event,
                                            pointerId,
                                            strokeId,
                                            predictedEvent
                                        )
                                    }
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    check(pointerId == currentPointerId.value)
                                    val currentStrokeId = checkNotNull(currentStrokeId.value)
                                    inProgressStrokesView.finishStroke(
                                        event,
                                        pointerId,
                                        currentStrokeId
                                    )
                                    view.performClick()
                                    true
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    check(pointerId == currentPointerId.value)

                                    val currentStrokeId = checkNotNull(currentStrokeId.value)
                                    inProgressStrokesView.cancelStroke(currentStrokeId, event)
                                    true
                                }

                                else -> false
                            }
                        } finally {
                            predictedEvent?.recycle()
                        }

                    }
                rootView.setOnTouchListener(touchListener)
                rootView.addView(inProgressStrokesView)
                rootView
            },
        ) {

        }



        Canvas(modifier = Modifier) {
            val canvasTransform = Matrix()
//            canvasTransform.setTranslate(10f, 10f)
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            finishedStrokesState.value.forEach { stroke ->
                canvasStrokeRenderer.draw(
                    stroke = stroke.value, // .copy(finishBrush),
                    canvas = canvas,
                    strokeToScreenTransform = canvasTransform
                )
            }

            inProgressStrokesView.removeFinishedStrokes(finishedStrokesState.value.keys)
        }
    }
}