package com.slaviboy.chromawheelexamples

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.slaviboy.chromawheel.ChromaWheelView

class MainActivity : AppCompatActivity() {

    lateinit var chromaWheelView: ChromaWheelView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemUI()

        chromaWheelView = findViewById(R.id.chroma_wheel_view)
        chromaWheelView.setOnColorSelectedListener { view, clickedBlockIndex ->
            Log.i("listener", "${clickedBlockIndex}")
        }
    }

    fun changeColor(view: View) {

        val colorRange = (0..255)

        val r = colorRange.random()
        val g = colorRange.random()
        val b = colorRange.random()

        chromaWheelView.selectedColor = Color.rgb(r, g, b)
    }

    fun changeNumberOfColors(view: View) {
        chromaWheelView.numberOfChromaColors = (3..10).random()
    }

    fun changeCirclesRadius(view: View) {

        val bigCircleRadius = 0.5f + 0.5f * Math.random().toFloat()   // [0.5, 1]
        val smallCircleRadius = 0.1f + 0.3f * Math.random().toFloat() // [0.1, 0.4]

        chromaWheelView.bigCircleRadius = bigCircleRadius
        chromaWheelView.smallCircleRadius = smallCircleRadius
    }

    fun changeSpaceBetweenColorBlocks(view: View) {

        val emptySpaceBetweenColorBlocks = Math.random().toFloat()
        chromaWheelView.spaceBetweenColorBlocks = emptySpaceBetweenColorBlocks
    }

    fun showOverlayShadow(view: View) {
        if (chromaWheelView.overlayShadowColor == Color.TRANSPARENT) {
            chromaWheelView.overlayShadowColor = Color.argb(100, 0, 0, 0)
        } else {
            chromaWheelView.overlayShadowColor = Color.TRANSPARENT
        }
    }

    fun changeAnimationDuration(view: View) {
        val durationRange = (100..2000)
        chromaWheelView.animationDuration = durationRange.random().toLong()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

}