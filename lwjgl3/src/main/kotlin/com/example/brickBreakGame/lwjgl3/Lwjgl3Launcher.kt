@file:JvmName("Lwjgl3Launcher")

package com.example.brickBreakGame.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.example.brickBreakGame.BrickBreakerGame

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return
    Lwjgl3Application(BrickBreakerGame(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("brick_break")
        setWindowedMode(720, 650)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
