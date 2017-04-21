package com.common.bting.videoplayer

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.*
import java.util.*

/**
 * Created by bting on 1/4/17.
 *
 * Usage:
 * 1. Put this in any layout file
 * <com.studiesweekly.common.video.VideoPlayer
 *      android:layout_width="match_parent"
 *      android:layout_height="wrap_content" />
 *
 * 2. set video url in code by calling: VideoPlayer.setVideoURI(uri);
 * 3. Put android:configChanges="screenSize|orientation" in the manifest for the activity using the
 *    the video view (otherwise the player would re-initialize and media controller may leak on
 *    screen rotation)
 *
 * Note: The view height is dynamically adjusted after the video is loaded
 */

class VideoPlayer : FrameLayout {

    private val videoView by bind<VideoPlayerView>(R.id.video_player_view)
    private val playIcon by bind<ImageView>(R.id.video_preview_icon)
    private val replayIcon by bind<ImageView>(R.id.video_replay_icon)
    private val progressBar by bind<ProgressBar>(R.id.video_view_progress_bar)
    private val controller: VideoPlayerController

    private var ready: Boolean = false

    private val PREVIEW_POSITION = 100
    private val MEDIA_CONTROLLER_BACKGROUND = 0x44000000

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        ready = false

        View.inflate(context, R.layout.video_player, this)

        controller = VideoPlayerController(context, false)

        videoView.setOnPreparedListener {
            progressBar.visibility = View.GONE
            playIcon.visibility = View.VISIBLE

            if (!ready) {
                videoView.seekTo(PREVIEW_POSITION)
                ready = true
            } else {
                videoView.resumeAtPreviousPosition()
            }

            videoView.setMediaController(controller)

            videoView.setOnCompletionListener { mediaPlayer ->
                val duration = mediaPlayer.duration
                if (duration > 0) {
                    mediaPlayer.seekTo(duration)
                    replayIcon.visibility = View.VISIBLE
                    controller.show(0)
                } else {
                    videoView.setOnCompletionListener(null)
                }
            }
        }

        videoView.setOnVideoClickListener(object : VideoPlayerView.OnVideoClickListener {
            override fun onClick(v: View) {
                if (videoView.isPlaying) {
                    videoView.pause()
                } else {
                    // Replay
                    if (videoView.currentPosition == videoView.duration) {
                        videoView.seekTo(0)
                    }
                    videoView.start()
                }
            }
        })

        videoView.setVideoViewListener(object : VideoPlayerView.VideoViewListener {
            override fun onPlay() {
                if (videoView.currentPosition == PREVIEW_POSITION) {
                    videoView.seekTo(0)
                }
                playIcon.visibility = View.GONE
                replayIcon.visibility = View.GONE
                controller.show() // Hide with default delay (3 secs)
            }

            override fun onPause() {
                controller.show(0)
                playIcon.visibility = View.VISIBLE
            }

            override fun onSeekTo(msec: Int) {
                if (videoView.duration == msec) {
                    replayIcon.visibility = View.VISIBLE
                    playIcon.visibility = View.GONE
                } else {
                    replayIcon.visibility = View.GONE
                    if (!videoView.isPlaying) {
                        playIcon.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.INVISIBLE) {
            progressBar.visibility = View.VISIBLE
            playIcon.visibility = View.GONE
        }
    }

    /**
     * Set the initial aspect ratio of the layout (before video load)
     */
    fun setDefaultAspectRatio(mAspectRatioWidth: Int, mAspectRatioHeight: Int) {
        videoView.setDefaultAspectRatio(mAspectRatioWidth, mAspectRatioHeight)
    }

    fun setVideoURI(uri: Uri) {
        videoView.setVideoURI(uri)
        progressBar.visibility = View.VISIBLE
    }

    private inner class VideoPlayerController : MediaController {

        private val mFormatBuilder: StringBuilder = StringBuilder()
        private val mFormatter: Formatter
        private val mFadeOut = Runnable { hide() }

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

        constructor(context: Context, useFastForward: Boolean) : super(context, useFastForward)

        constructor(context: Context) : super(context)

        init {
            mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
        }

        override fun show(timeout: Int) {
            removeCallbacks(mFadeOut)
            if (timeout != 0) {
                postDelayed(mFadeOut, timeout.toLong())
            }
            super.show(0)
        }

        override fun setAnchorView(view: View) {
            super.setAnchorView(view)

            // Change media controller background color
            val mediaControllerRoot = getChildAt(0) as LinearLayout
            mediaControllerRoot.setBackgroundColor(MEDIA_CONTROLLER_BACKGROUND)

            // Hide play/pause button
            val buttonLayout = mediaControllerRoot.getChildAt(0) as LinearLayout
            buttonLayout.getChildAt(2).visibility = View.GONE

            // Change Time text color
            val progressLayout = mediaControllerRoot.getChildAt(1) as LinearLayout

            val currentPositionText = progressLayout.getChildAt(0) as TextView
            currentPositionText.setTextColor(Color.WHITE)

            val durationText = progressLayout.getChildAt(2) as TextView
            durationText.setTextColor(Color.WHITE)

            val duration = videoView.duration

            if (duration > 0) {
                val position = videoView.currentPosition

                currentPositionText.visibility = View.VISIBLE
                currentPositionText.text = stringForTime(position)

                durationText.visibility = View.VISIBLE
                durationText.text = stringForTime(duration)

                // Progress bar (Copied from MediaController)
                val mProgress = progressLayout.getChildAt(1) as ProgressBar
                if (duration > 0) {
                    // use long to avoid overflow
                    val pos = 1000L * position / duration
                    mProgress.progress = pos.toInt()
                }
                val percent = videoView.bufferPercentage
                mProgress.secondaryProgress = percent * 10

            } else {
                currentPositionText.visibility = View.INVISIBLE
                durationText.visibility = View.INVISIBLE
                mediaControllerRoot.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                controller.hide()
                (context as Activity).onBackPressed()
            }
            return super.dispatchKeyEvent(event)
        }

        // Copied from MediaController
        private fun stringForTime(timeMs: Int): String {
            val totalSeconds = timeMs / 1000

            val seconds = totalSeconds % 60
            val minutes = totalSeconds / 60 % 60
            val hours = totalSeconds / 3600

            mFormatBuilder.setLength(0)
            if (hours > 0) {
                return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
            } else {
                return mFormatter.format("%02d:%02d", minutes, seconds).toString()
            }
        }
    }
}
