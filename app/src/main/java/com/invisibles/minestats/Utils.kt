package com.invisibles.minestats

import android.animation.ValueAnimator
import android.content.res.Resources
import android.util.TypedValue
import android.view.animation.AlphaAnimation
import java.text.SimpleDateFormat
import java.util.*

class Utils {

    companion object{

        fun getLenNumUntilDot(num: Double): Int {
            val numStr = num.toString()
            val splitNum = numStr.split(".")[0]
            return splitNum.length

        }

        fun getTime(time: Long): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = Date(time)
            return dateFormat.format(date)
        }

        fun getValueAnimation(val0: Float, val1: Float, duration: Int = 500, function: (ValueAnimator) -> Unit): ValueAnimator {
            val animator = ValueAnimator.ofInt(dps(val0), dps(val1)).apply {
                setDuration(duration.toLong())
            }
            animator.addUpdateListener(function)
            return animator
        }

        fun getValueAnimationFloat(val0: Float, val1: Float, duration: Int = 500, function: (ValueAnimator) -> Unit): ValueAnimator {
            val animator = ValueAnimator.ofFloat(val0, val0).apply {
                setDuration(duration.toLong())
            }
            animator.addUpdateListener(function)
            return animator
        }

        fun getAlphaAnimation(val0: Float, val1: Float, duration: Int = 500): AlphaAnimation {
            val animator = AlphaAnimation(val0, val1).apply {
                setDuration(duration.toLong())
            }

            return animator
        }

        fun dps(dp: Float): Int {

            val r = Resources.getSystem()
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.displayMetrics)
            return px.toInt()

        }

    }

}