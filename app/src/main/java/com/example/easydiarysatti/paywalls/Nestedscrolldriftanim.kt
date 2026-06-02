package com.example.easydiarysatti.paywalls
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * Infinite ticker scroll — items scroll up continuously.
 * When the last item scrolls out of view, the list silently jumps back
 * to the top and continues — creating a seamless infinite loop effect.
 *
 * To make the loop truly seamless, the LinearLayout inside featuresScrollView
 * should have its 5 rows duplicated (rows 1-5 then rows 1-5 again) in XML.
 * This way the "jump" happens while the duplicate is visible — user never sees it.
 *
 * Call from onViewCreated:
 *   view.findViewById<NestedScrollView>(R.id.featuresScrollView)?.startDriftScroll()
 */
private const val SCROLL_INTERVAL_MS = 16L   // ~60fps
private const val PIXELS_PER_TICK    = 1     // 1px per frame = smooth slow drift
private const val RESUME_AFTER_TOUCH = 1200L

fun NestedScrollView.startDriftScroll() {
    val handler = Handler(Looper.getMainLooper())
    var isRunning = true

    val ticker = object : Runnable {
        override fun run() {
            if (isRunning) {
                val child = getChildAt(0)
                val maxScroll = (child?.height ?: 0) - height

                if (maxScroll > 0) {
                    if (scrollY >= maxScroll) {
                        // Reached bottom — jump silently to halfway point
                        // (halfway = end of first copy, start of second copy)
                        scrollTo(0, maxScroll / 2)
                    } else {
                        scrollBy(0, PIXELS_PER_TICK)
                    }
                }
            }
            handler.postDelayed(this, SCROLL_INTERVAL_MS)
        }
    }

    viewTreeObserver.addOnGlobalLayoutListener {
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 600L)
    }

    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> isRunning = false
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.postDelayed({ isRunning = true }, RESUME_AFTER_TOUCH)
                v.performClick()
            }
        }
        false
    }
}