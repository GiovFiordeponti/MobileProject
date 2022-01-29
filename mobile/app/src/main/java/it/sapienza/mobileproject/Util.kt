package it.sapienza.mobileproject

import android.animation.Animator

import android.animation.AnimatorListenerAdapter
import android.view.View


fun formatFriendNumber(friendCode: String): String {
    val stringBuilder = StringBuilder()
    for (i in friendCode.indices) {
        stringBuilder.append(friendCode[i])
        if ((i + 1) % 4 == 0 && i + 1 < friendCode.length) {
            stringBuilder.append(" ")
        }
    }
    return stringBuilder.toString()
}

fun getRealFriendNumber(viewFriendNumber: String): String {
    return viewFriendNumber.replace(" ", "")
}

fun animateView(
    view: View,
    toVisibility: Int,
    toAlpha: Float,
    duration: Int
) {
    val show = toVisibility == View.VISIBLE
    if (show) {
        view.alpha = 0F
    }
    view.visibility = View.VISIBLE
    view.animate()
        .setDuration(duration.toLong())
        .alpha(if (show) toAlpha else 0F)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = toVisibility
            }
        })
}