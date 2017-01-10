import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * <h1>Sound</h1>
 * <p>
 * Handles playing, stoping, and looping of sounds for the game.
 * </p>
 * 
 * @author Tyler Thomas
 * @author Alex Wong (modified for own use)
 * @since 2016-01-19
 */
public class Sound {
	private Clip clip;

	/**
	 * The constructor associates the sound file with the Sound object.
	 * 
	 * @param fileName
	 *            The sound file to be played.
	 */
	public Sound(String fileName) {
		// is in resource folder, so don't need path
		String filePath = fileName + ".wav";

		try {
			InputStream is = getClass().getResourceAsStream(filePath);
			// bufferedinputstream avoids the "mark supported" feature
			// encountered without it
			BufferedInputStream bis = new BufferedInputStream(is);
			AudioInputStream sound = AudioSystem.getAudioInputStream(bis);
			clip = AudioSystem.getClip();
			clip.open(sound);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException("Sound: Malformed URL: " + e);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			throw new RuntimeException("Sound: Unsupported Audio File: " + e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Sound: Input/Output Error: " + e);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Sound: Line Unavailable Exception Error: " + e);
		}

	}

	/**
	 * Plays the sound clip.
	 */
	public void play() {
		clip.setFramePosition(0); // Must always rewind!
		clip.start();
	}

	/**
	 * Loops the sound clip.
	 */
	public void loop() {
		clip.setFramePosition(0);
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	/**
	 * Stops the sound clip.
	 */
	public void stop() {
		clip.stop();
	}

	/**
	 * Resumes the sound clip from its previous location.
	 */
	public void resume() {
		clip.start();
	}
}
