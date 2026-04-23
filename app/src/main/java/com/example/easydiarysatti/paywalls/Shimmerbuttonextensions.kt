package com.example.easydiarysatti.paywalls

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Shimmer sweep animation for CTA buttons.
 *
 * Usage (flat / small-radius button):
 *   btnContinue?.startShimmer()
 *
 * Usage (rounded AppCompatButton / button inside a CardView):
 *   btnContinue?.startShimmerDp(24f)   // pass the button's visual corner radius in dp
 *
 * Always call stopShimmer() in onDestroyView / onDismiss to avoid animator leaks.
 *
 * ── Why NOT View.generateViewId() ───────────────────────────────────────────
 * View.setTag(key, value) requires the key to be an *application-scoped* resource
 * id (i.e. declared in R.id.*). generateViewId() produces a synthetic view-id that
 * is NOT in the application's resource table, so setTag() throws:
 *   "The key must be an application-specific resource id."
 * We therefore use a plain Int constant as the tag key, which is the correct
 * approach when you don't have (or don't want) an extra res/values/ids.xml entry.
 * ────────────────────────────────────────────────────────────────────────────
 */

private const val SHIMMER_DURATION_MS = 2_400L
private const val SHIMMER_DELAY_MS    =   800L

/**
 * Plain Int used as the tag key.
 * View.setTag(Int, Any?) accepts any Int when called as the two-arg overload
 * only if the Int is a valid resource id from the *app's* R namespace.
 *
 * The safe, zero-resource alternative: use the single-arg View.setTag(Any?) /
 * View.getTag() — that API has no key restriction and stores one object per view,
 * which is all we need here.
 */

/**
 * Start a shimmer sweep on this view.
 *
 * @param cornerRadiusPx  corner radius of the button background in **pixels**.
 *                        Pass 0f for a flat button.
 *                        Pass the button's actual corner radius so the shimmer
 *                        RoundRect clips correctly at the corners.
 */
fun View.startShimmer(cornerRadiusPx: Float = 0f) {
    // Cancel any existing animator stored in the single-arg tag slot.
    stopShimmer()

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val animator = ValueAnimator.ofFloat(-1f, 2f).apply {
        duration     = SHIMMER_DURATION_MS
        startDelay   = SHIMMER_DELAY_MS
        repeatCount  = ValueAnimator.INFINITE
        repeatMode   = ValueAnimator.RESTART
        interpolator = LinearInterpolator()

        addUpdateListener { anim ->
            val progress = anim.animatedValue as Float
            val w = this@startShimmer.width.toFloat()
            val h = this@startShimmer.height.toFloat()
            if (w == 0f || h == 0f) return@addUpdateListener

            val cx = progress * w
            paint.shader = LinearGradient(
                cx - w * 0.40f, 0f,
                cx + w * 0.40f, 0f,
                intArrayOf(0x00FFFFFF, 0x44FFFFFF, 0x99FFFFFF.toInt(), 0x44FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )

            val rect = RectF(0f, 0f, w, h)
            overlay.clear()
            overlay.add(object : android.graphics.drawable.Drawable() {
                override fun draw(canvas: Canvas) {
                    if (cornerRadiusPx > 0f)
                        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
                    else
                        canvas.drawRect(rect, paint)
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
                @Suppress("OVERRIDE_DEPRECATION")
                override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
            })
            this@startShimmer.invalidate()
        }
    }

    // ── Use the zero-arg tag slot — no resource id required ──────────────────
    tag = animator
    animator.start()
}

/**
 * Convenience overload: pass the corner radius in dp.
 */
fun View.startShimmerDp(cornerRadiusDp: Float) {
    startShimmer(cornerRadiusDp * resources.displayMetrics.density)
}

/**
 * Cancel and remove the shimmer.
 * Safe to call even if no shimmer is running.
 */
fun View.stopShimmer() {
    (tag as? ValueAnimator)?.cancel()
    tag = null
    overlay.clear()
}