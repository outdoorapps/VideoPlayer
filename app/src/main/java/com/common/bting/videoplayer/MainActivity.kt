package com.common.bting.videoplayer

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.net.URL

class MainActivity : AppCompatActivity() {

    val link1 = "https://ak6.picdn.net/shutterstock/videos/13945376/preview/stock-footage-hawksbill-sea-turtle-is-swimming-and-chases-the-camera-probably-seeing-it-s-mirror-image.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val videoPlayer = find<VideoPlayer>(R.id.video_player)
        val url = URL(link1)
        videoPlayer.setVideoURI(Uri.parse(link1))

    }
}
