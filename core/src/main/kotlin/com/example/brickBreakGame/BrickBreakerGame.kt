package com.example.brickBreakGame

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.Input

class BrickBreakerGame : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var paddleTexture: Texture
    private lateinit var ballTexture: Texture
    private lateinit var brickTexture: Texture
    private lateinit var font: BitmapFont

    private lateinit var paddle: Rectangle
    private lateinit var ball: Rectangle
    private val bricks = Array<Rectangle>()

    private var ballVelocity = Vector2(300f, 300f)
    private var score = 0
    private var lives = 3
    private var level = 1
    private var pointsPerBrick = 10
    private var speedMultiplier = 1.0f
    private var gameOver = false
    private var showLevelMessage = false
    private var levelMessageTimer = 0f

    private var nameInput = ""
    private var showNameInput = true
    private var gameStarted = false

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont().apply { color = Color.WHITE }

        paddleTexture = Texture("ui/paddle.png")
        ballTexture = Texture("ui/ball.png")
        brickTexture = Texture("ui/brick.png")
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.begin()

        if (showNameInput) {
            renderNameInputScreen()
        } else if (!gameStarted) {
            renderStartGamePrompt()
        } else {
            if (showLevelMessage) {
                updateLevelMessage()
            } else if (!gameOver) {
                update()
            } else {
                handleRestart()
            }

            if (!gameOver && !showLevelMessage) {
                // Draw paddle, ball, and bricks
                batch.draw(paddleTexture, paddle.x, paddle.y, paddle.width, paddle.height)
                batch.draw(ballTexture, ball.x, ball.y, ball.width, ball.height)
                for (brick in bricks) {
                    batch.draw(brickTexture, brick.x, brick.y, brick.width, brick.height)
                }

                // Display score, lives, level, and player name
                font.draw(batch, "Score: $score", 10f, Gdx.graphics.height - 10f)
                font.draw(batch, "Lives: $lives", Gdx.graphics.width - 100f, Gdx.graphics.height - 10f)
                font.draw(batch, "Level: $level", Gdx.graphics.width / 2f - 40f, Gdx.graphics.height - 10f)
                font.draw(batch, "Player: $nameInput", 10f, Gdx.graphics.height - 30f)
            } else if (showLevelMessage) {
                // Display "Level X" message
                font.draw(batch, "Level $level", Gdx.graphics.width / 2f - 40f, Gdx.graphics.height / 2f)
            } else {
                // Display game over and restart instructions
                font.draw(
                    batch,
                    "Game Over! \n $nameInput's Final Score: $score",
                    Gdx.graphics.width / 2f - 80f,
                    Gdx.graphics.height / 2f + 20f
                )
                font.draw(
                    batch,
                    "\n \n Press Enter to Play Again",
                    Gdx.graphics.width / 2f - 80f,
                    Gdx.graphics.height / 2f - 10f
                )
            }
        }

        batch.end()
    }

    private fun renderNameInputScreen() {
        font.draw(
            batch,
            "Enter your name",
            Gdx.graphics.width / 2f - 100f,
            Gdx.graphics.height / 2f + 20f
        )
        font.draw(batch, nameInput, Gdx.graphics.width / 2f - 100f, Gdx.graphics.height / 2f)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && nameInput.isNotEmpty()) {
            nameInput = nameInput.dropLast(1)
        }

        val pressedKey = getTypedCharacter()
        if (pressedKey != null && nameInput.length < 20) {
            nameInput += pressedKey
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && nameInput.isNotBlank()) {
            showNameInput = false
        }
    }

    private fun renderStartGamePrompt() {
        font.draw(batch, "Press Enter to start the game", Gdx.graphics.width / 2f - 100f, Gdx.graphics.height / 2f)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            initializeGame()
            gameStarted = true
        }
    }

    private fun getTypedCharacter(): Char? {
        for (key in Input.Keys.A..Input.Keys.Z) {
            if (Gdx.input.isKeyJustPressed(key)) {
                return (key - Input.Keys.A + 'a'.code).toChar()
            }
        }
        return null
    }

    private fun update() {
        // Paddle movement with mouse or touch input
        if (Gdx.input.isTouched) {
            val touchX = Gdx.input.x.toFloat()
            paddle.x = touchX - paddle.width / 2
            paddle.x = paddle.x.coerceIn(0f, Gdx.graphics.width - paddle.width)
        }

        // Ball movement and collision with screen edges
        ball.x += ballVelocity.x * Gdx.graphics.deltaTime * speedMultiplier
        ball.y += ballVelocity.y * Gdx.graphics.deltaTime * speedMultiplier

        if (ball.x < 0 || ball.x + ball.width > Gdx.graphics.width) ballVelocity.x = -ballVelocity.x
        if (ball.y + ball.height > Gdx.graphics.height) ballVelocity.y = -ballVelocity.y
        if (ball.y < 0) {
            lives--
            resetBall()
            if (lives <= 0) gameOver = true
        }

        // Ball collision with paddle
        if (ball.overlaps(paddle)) {
            ballVelocity.y = -ballVelocity.y
            ball.y = paddle.y + paddle.height // Prevents sticking to paddle
        }

        for (brick in bricks) {
            if (brick.overlaps(ball)) {
                ballVelocity.y = -ballVelocity.y // Reverse ball direction
                bricks.removeValue(brick, true) // Remove the brick from the array
                score += 10 // Update score
                break // Exit the loop after handling the collision
            }
        }


        // Ball collision with bricks
        val iterator = bricks.iterator()
        while (iterator.hasNext()) {
            val brick = iterator.next()
            if (ball.overlaps(brick)) {
                ballVelocity.y = -ballVelocity.y
                score += pointsPerBrick
                iterator.remove()
                break
            }
        }

        // Check if all bricks are cleared
        if (bricks.isEmpty) {
            advanceToNextLevel()
        }
    }

    private fun updateLevelMessage() {
        levelMessageTimer += Gdx.graphics.deltaTime
        if (levelMessageTimer > 2f) { // Show "Level X" for 2 seconds
            showLevelMessage = false
            createBrickLayout()
        }
    }

    private fun resetBall() {
        // Position the ball just above the paddle
        ball.setPosition(paddle.x + paddle.width / 2f - ball.width / 2f, paddle.y + paddle.height)
        ballVelocity.set(300f, 300f) // Set initial velocity upwards
    }


    private fun createBrickLayout() {
        val rows = 5
        val cols = 8
        val brickWidth = 80f
        val brickHeight = 30f
        val padding = 10f

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val brick = Rectangle().apply {
                    x = col * (brickWidth + padding)
                    y = Gdx.graphics.height - 100f - row * (brickHeight + padding)
                    width = brickWidth
                    height = brickHeight
                }
                bricks.add(brick)
            }
        }
    }

    private fun advanceToNextLevel() {
        level++
        lives = 3 // Reset lives to 3 for the new level
        pointsPerBrick = level * 10
        speedMultiplier = level.toFloat()
        showLevelMessage = true
        levelMessageTimer = 0f
        createBrickLayout() // Create a new layout of bricks
        resetBall() // Reset ball to bounce from the paddle
    }


    private fun initializeGame() {
        paddle = Rectangle().apply {
            x = Gdx.graphics.width / 2f - 50
            y = 50f
            width = 100f
            height = 20f
        }

        ball = Rectangle().apply {
            x = Gdx.graphics.width / 2f - 10
            y = Gdx.graphics.height / 2f - 10
            width = 20f
            height = 20f
        }

        score = 0
        lives = 3
        level = 1
        pointsPerBrick = 10
        speedMultiplier = 1.0f
        bricks.clear()
        createBrickLayout()
        resetBall()
        gameOver = false
        showLevelMessage = false
    }

    private fun handleRestart() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            initializeGame()
        }
    }

    override fun dispose() {
        batch.dispose()
        paddleTexture.dispose()
        ballTexture.dispose()
        brickTexture.dispose()
        font.dispose()
    }
}
