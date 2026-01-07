# StarkPlayer

A modern JavaFX-based music player application built with Java 21.

## Features

- **Music Playback**: Play MP3 audio files with JavaFX MediaPlayer
- **Playlist Management**: Create and manage playlists
- **Metadata Extraction**: Extract song information using mp3agic library
- **User-Friendly Interface**: Clean GUI built with JavaFX and FXML

## Requirements

- Java 21 or higher
- Maven 3.6+
- macOS (currently configured for Apple Silicon)

## Building and Running

1. Clone the repository:
   ```bash
   git clone https://github.com/monish-7-bot/starkplayer.git
   cd starkplayer
   ```

2. Build the project:
   ```bash
   mvn clean compile
   ```

3. Run the application:
   ```bash
   mvn javafx:run
   ```

## Dependencies

- **JavaFX**: For the GUI and media playback (version 21.0.3)
- **mp3agic**: For MP3 metadata extraction (version 0.9.1)

## Project Structure

- `src/main/java/com/starkplayer/`: Main application code
  - `MainApp.java`: Application entry point
  - `controller/`: FXML controllers
  - `model/`: Data models
  - `util/`: Utility classes
- `src/main/resources/`: FXML files and stylesheets

## Contributing

Feel free to fork and submit pull requests!