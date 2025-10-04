package com.example.easydiarysatti.utills

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MaterialButtonToggleGroupWithRadius @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButtonToggleGroup(context, attrs, defStyleAttr) {

    override fun onFinishInflate() {
        super.onFinishInflate()
        val radius = 40f
        for (i in 0 until childCount) {
            (getChildAt(i) as? MaterialButton)?.let { button ->
                val builder = button.shapeAppearanceModel.toBuilder()
                button.shapeAppearanceModel = builder
                    .setAllCornerSizes(radius)
                    .build()
            }
        }
    }
}
