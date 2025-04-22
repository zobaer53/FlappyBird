package com.zobaer53.flappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import java.util.Random;


public class FlappyBird extends ApplicationAdapter {
	Preferences preferences ;

	SpriteBatch batch;
	Texture background;

	Music music;
	Sound sound;

	//ShapeRenderer shapeRenderer;

	Texture gameover;
	Texture startImage;

	Texture[] birds;
	int flapState = 0;
	float birdY = 0;
	float velocity = 0;
	Circle birdCircle;
	int score = 0;
	int highScore ;
	int scoringTube = 0;
	BitmapFont font;
	BitmapFont font1;

	int gameState = 0;
	float gravity = 2;

	Texture topTube;
	Texture bottomTube;
	float gap = 500;
	float maxTubeOffset;
	Random randomGenerator;
	float tubeVelocity = 4;
	int numberOfTubes = 4;
	float[] tubeX = new float[numberOfTubes];
	float[] tubeOffset = new float[numberOfTubes];
	float distanceBetweenTubes;
	Rectangle[] topTubeRectangles;
	Rectangle[] bottomTubeRectangles;
	
	// Mute functionality
	Texture soundOnIcon;
	Texture soundOffIcon;
	boolean isMuted = false;
	Rectangle muteButtonRect;


	@Override
	public void dispose() {
		super.dispose();
		if (music != null) {
			music.dispose();
		}
		if (sound != null) {
			sound.dispose();
		}
		background.dispose();
		gameover.dispose();
		startImage.dispose();
		topTube.dispose();
		bottomTube.dispose();
		for (Texture bird : birds) {
			bird.dispose();
		}
		soundOnIcon.dispose();
		soundOffIcon.dispose();
		font.dispose();
		font1.dispose();
		batch.dispose();
	}

	@Override
	public void create () {
		preferences = Gdx.app.getPreferences("flappybird");
		highScore = preferences.getInteger("highscore",0);
		// Load mute state from preferences
		isMuted = preferences.getBoolean("muted", false);
		
		batch = new SpriteBatch();
		background = new Texture("bg.png");
		gameover = new Texture("gameover.png");
		startImage = new Texture("start_image.png");
		
		// Load sound icons
		try {
			soundOnIcon = new Texture("sound_on.png");
			soundOffIcon = new Texture("sound_off.png");
		} catch (Exception e) {
			Gdx.app.log("Texture Error", "Could not load sound icons: " + e.getMessage());
			// Create fallback textures using Pixmap
			Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
			pixmap.setColor(Color.WHITE);
			pixmap.fill();
			soundOnIcon = new Texture(pixmap);
			soundOffIcon = new Texture(pixmap);
			pixmap.dispose();
		}
		
		// Set up mute button rectangle for touch detection - position at top right with good size
		float buttonSize = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) / 10;
		muteButtonRect = new Rectangle(
			Gdx.graphics.getWidth() - buttonSize - 20, // 20px from right edge
			Gdx.graphics.getHeight() - buttonSize - 20, // 20px from top edge
			buttonSize,
			buttonSize
		);

		try {
			music = Gdx.audio.newMusic(Gdx.files.internal("android_assets_music.mp3"));
			music.setLooping(true);
			music.setVolume(0.1f);
			if (!isMuted) {
				music.play();
			}
		} catch (Exception e) {
			Gdx.app.log("Audio Error", "Could not load music file: " + e.getMessage());
			music = null;
		}

		try {
			sound = Gdx.audio.newSound(Gdx.files.internal("android_assets_sfx_wing.ogg"));
		} catch (Exception e) {
			Gdx.app.log("Audio Error", "Could not load sound file: " + e.getMessage());
			sound = null;
		}

		birdCircle = new Circle();
		font = new BitmapFont();
		font.setColor(Color.WHITE);
		font.getData().setScale(10);

		font1 = new BitmapFont();
		font1.setColor(Color.YELLOW);
		font1.getData().setScale(5);

		birds = new Texture[2];
		birds[0] = new Texture("bird.png");
		birds[1] = new Texture("bird2.png");


		topTube = new Texture("toptube.png");
		bottomTube = new Texture("bottomtube.png");
		maxTubeOffset = Gdx.graphics.getHeight() / 2 - gap / 2 - 100;
		randomGenerator = new Random();
		distanceBetweenTubes = Gdx.graphics.getWidth() * 1;
		topTubeRectangles = new Rectangle[numberOfTubes];
		bottomTubeRectangles = new Rectangle[numberOfTubes];



		startGame();
	}

	public void startGame() {

		birdY = Gdx.graphics.getHeight() / 2 - birds[0].getHeight() / 2;

		for (int i = 0; i < numberOfTubes; i++) {

			tubeOffset[i] = (randomGenerator.nextFloat() - 0.5f) * (Gdx.graphics.getHeight() - gap - 200);

			tubeX[i] = Gdx.graphics.getWidth() / 2 - topTube.getWidth() / 2 + Gdx.graphics.getWidth() + i * distanceBetweenTubes;

			topTubeRectangles[i] = new Rectangle();
			bottomTubeRectangles[i] = new Rectangle();

		}

	}

	@Override
	public void render () {
		// Check for mute button touch
		if (Gdx.input.justTouched()) {
			float touchX = Gdx.input.getX();
			float touchY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Invert Y coordinate
			
			// Check if mute button was pressed
			if (muteButtonRect.contains(touchX, touchY)) {
				isMuted = !isMuted;
				// Save mute state to preferences
				preferences.putBoolean("muted", isMuted);
				preferences.flush();
				updateAudioState();
			}
		}

		batch.begin();
		batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		// Draw mute button with white background circle
		batch.setColor(1, 1, 1, 0.8f);  // Slightly transparent white
		if (isMuted) {
			batch.draw(soundOffIcon, muteButtonRect.x, muteButtonRect.y, muteButtonRect.width, muteButtonRect.height);
		} else {
			batch.draw(soundOnIcon, muteButtonRect.x, muteButtonRect.y, muteButtonRect.width, muteButtonRect.height);
		}
		batch.setColor(1, 1, 1, 1f);  // Reset to full opacity

		if (gameState == 1) {
			batch.draw(birds[flapState], Gdx.graphics.getWidth() / 2 - birds[flapState].getWidth() / 2, birdY);


			if (tubeX[scoringTube] < Gdx.graphics.getWidth() / 2) {

				score++;

				Gdx.app.log("Score", String.valueOf(score));

				if (scoringTube < numberOfTubes - 1) {

					scoringTube++;

				} else {

					scoringTube = 0;

				}

			}

			if (Gdx.input.justTouched()) {
				if (sound != null && !isMuted) {
					sound.play(0.5f);
				}

				velocity = -30;

			}

			for (int i = 0; i < numberOfTubes; i++) {

				if (tubeX[i] < - topTube.getWidth()) {

					tubeX[i] += numberOfTubes * distanceBetweenTubes;
					tubeOffset[i] = (randomGenerator.nextFloat() - 0.5f) * (Gdx.graphics.getHeight() - gap - 200);

				} else {

					tubeX[i] = tubeX[i] - tubeVelocity;



				}

				batch.draw(topTube, tubeX[i], Gdx.graphics.getHeight() / 2 + gap / 2 + tubeOffset[i]);
				batch.draw(bottomTube, tubeX[i], Gdx.graphics.getHeight() / 2 - gap / 2 - bottomTube.getHeight() + tubeOffset[i]);




				topTubeRectangles[i] = new Rectangle(tubeX[i], Gdx.graphics.getHeight() / 2 + gap / 2 + tubeOffset[i], topTube.getWidth(), topTube.getHeight());
				bottomTubeRectangles[i] = new Rectangle(tubeX[i], Gdx.graphics.getHeight() / 2 - gap / 2 - bottomTube.getHeight() + tubeOffset[i], bottomTube.getWidth(), bottomTube.getHeight());
			}



			if (birdY > 0) {

				velocity = velocity + gravity;
				birdY -= velocity;

			} else {

				gameState = 2;

			}
			font.draw(batch, String.valueOf(score), 100, 200);
			if(score>highScore){

				highScore=score;
				preferences.putInteger("highscore",highScore);
				preferences.flush();
			}


		} else if (gameState == 0) {
			// Draw the start image in the center of the screen
			float startX = Gdx.graphics.getWidth() / 2 - startImage.getWidth() / 2;
			float startY = Gdx.graphics.getHeight() / 2 - startImage.getHeight() / 2;
			batch.draw(startImage, startX, startY);

			if (Gdx.input.justTouched()) {
				gameState = 1;
			}

		} else if (gameState == 2) {


			batch.draw(gameover, Gdx.graphics.getWidth() / 2 - gameover.getWidth() / 2, Gdx.graphics.getHeight() / 2 - gameover.getHeight() / 2);
			font1.draw(batch,"High Score", 200, 600);

			font1.draw(batch, String.valueOf(highScore), 350, 500);

			if (Gdx.input.justTouched()) {

				gameState = 1;
				startGame();
				score = 0;
				scoringTube = 0;
				velocity = 0;
			}

		}

		if (flapState == 0) {
			flapState = 1;
		} else {
			flapState = 0;
		}



		birdCircle.set(Gdx.graphics.getWidth() / 2, birdY + birds[flapState].getHeight() / 2, birds[flapState].getWidth() / 2);





		for (int i = 0; i < numberOfTubes; i++) {



			if (Intersector.overlaps(birdCircle, topTubeRectangles[i]) || Intersector.overlaps(birdCircle, bottomTubeRectangles[i])) {

				gameState = 2;

			}

		}

		batch.end();




	}

	// Method to update audio state based on mute setting
	private void updateAudioState() {
		if (music != null) {
			if (isMuted) {
				music.pause();
			} else {
				music.play();
			}
		}
	}
}
