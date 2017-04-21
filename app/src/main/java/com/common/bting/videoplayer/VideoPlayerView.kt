package com.common.bting.videoplayer

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.VideoView
import com.common.bting.videoplayer.d

/**
 * Created by bting on 1/13/17.
 */

class VideoPlayerView : VideoView {

    private var onPreparedListener: MediaPlayer.OnPreparedListener? = null
    private var videoViewListener: VideoViewListener? = null
    private var onVideoClickListener: OnVideoClickListener? = null

    private var pausePosition: Int
    private var videoAspectRatio: Float
    private var aspectRatioWidth: Float
    private var aspectRatioHeight: Float

    private val DEFAULT_ASPECT_RATIO_WIDTH = 16f
    private val DEFAULT_ASPECT_RATIO_HEIGHT = 9f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        pausePosition = 0
        videoAspectRatio = 0f

        aspectRatioWidth = DEFAULT_ASPECT_RATIO_WIDTH
        aspectRatioHeight = DEFAULT_ASPECT_RATIO_HEIGHT

        // OnClickListener indeed
        setOnTouchListener(object : View.OnTouchListener {

            val MAX_CLICK_DURATION = 200
            var startClickTime: Long = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startClickTime = System.currentTimeMillis()
                    }
                    MotionEvent.ACTION_UP -> {
                        val clickDuration = System.currentTimeMillis() - startClickTime
                        if (clickDuration < MAX_CLICK_DURATION) {
                            // On Click Event
                            onVideoClickListener?.onClick(v)
                        }
                    }
                }
                return true
            }
        })
    }

    override fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener) {
        onPreparedListener = listener


        super.setOnPreparedListener(MediaPlayer.OnPreparedListener { mp ->
            d("onPrepared")
            mp.setOnVideoSizeChangedListener { mp, width, height ->
                videoAspectRatio = height.toFloat() / width.toFloat()
                requestLayout()  // Invoke onMeasure
            }

            onPreparedListener?.onPrepared(mp)
        })
    }

    fun setVideoViewListener(listener: VideoViewListener) {
        videoViewListener = listener
    }

    fun setOnVideoClickListener(listener: OnVideoClickListener) {
        onVideoClickListener = listener
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == View.INVISIBLE) {
            pausePosition = currentPosition
        }
        super.onVisibilityChanged(changedView, visibility)
    }

    override fun pause() {
        videoViewListener?.onPause()
        super.pause()
    }

    override fun start() {
        videoViewListener?.onPlay()
        super.start()
    }

    override fun seekTo(msec: Int) {
        videoViewListener?.onSeekTo(msec)
        super.seekTo(msec)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        val newHeight: Int

        if (videoAspectRatio == 0f) {
            newHeight = (width * aspectRatioHeight / aspectRatioWidth).toInt()
        } else {
            newHeight = (width * videoAspectRatio).toInt()
        }
        setMeasuredDimension(width, newHeight)
    }

    fun resumeAtPreviousPosition() {
        seekTo(pausePosition)
    }

    fun setDefaultAspectRatio(mAspectRatioWidth: Int, mAspectRatioHeight: Int) {
        this.aspectRatioWidth = mAspectRatioWidth.toFloat()
        this.aspectRatioHeight = mAspectRatioHeight.toFloat()
    }

    interface VideoViewListener {
        fun onPlay()

        fun onPause()

        fun onSeekTo(msec: Int)
    }

    interface OnVideoClickListener {
        fun onClick(v: View)
    }

}