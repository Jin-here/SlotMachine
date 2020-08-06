package com.vgaw.slotmachine

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_start.setOnClickListener { main_shaizi.start() }
        main_stop.setOnClickListener { main_shaizi.stop("789") }
    }
}