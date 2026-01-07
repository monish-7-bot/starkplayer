package com.starkplayer.controller;

import com.starkplayer.model.Song;
import com.starkplayer.util.IconFactory;
import com.starkplayer.util.MetadataExtractor;
import com.starkplayer.util.PlaylistManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlayerController {

    @FXML private ListView<Song> playlistView;
    @FXML private Button btnPlay, btnPause, btnStop, btnPrev, btnNext, btnLoadFolder, btnLoadPlaylist, btnSavePlaylist, btnAddFiles;
    @FXML private ToggleButton btnShuffle, btnRepeat;
    @FXML private Slider volumeSlider, progressSlider;
    @FXML private Label lblCurrentTime, lblTotalTime, lblNowPlaying, lblArtist, lblAlbum, lblGenre, lblYear;
    @FXML private ImageView albumArt;
    @FXML private Canvas visualizerCanvas;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> vizModeCombo;
    @FXML private HBox equalizerBox;
    @FXML private HBox controlButtons;

    private MediaPlayer mediaPlayer;
    private final ObservableList<Song> songs = FXCollections.observableArrayList();
    private final FilteredList<Song> filteredSongs;
    private final Random random = new Random();
    private int currentIndex = -1;
    private boolean seeking = false;
    private boolean shuffle = false;
    private int repeatMode = 0; // 0 = none, 1 = repeat all, 2 = repeat one
    private int vizMode = 0; // 0 = bars, 1 = circle, 2 = wave

    // Visualizer state
    private double[] magnitudes = new double[128];
    private AnimationTimer vizTimer;
    
    // Equalizer bands
    private final Slider[] eqSliders = new Slider[10];
    private final double[] eqValues = new double[10];
    
    // Thread pool for metadata extraction
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public MusicPlayerController() {
        filteredSongs = new FilteredList<>(songs, p -> true);
    }

    @FXML
    public void initialize() {
        volumeSlider.setValue(50);
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        
        // Initialize equalizer
        initializeEqualizer();
        
        // Initialize visualizer modes
        vizModeCombo.getItems().addAll("Bars", "Circle", "Wave", "Spectrum");
        vizModeCombo.setValue("Bars");
        vizModeCombo.setOnAction(e -> vizMode = vizModeCombo.getSelectionModel().getSelectedIndex());

        // Setup playlist view
        playlistView.setItems(filteredSongs);
        playlistView.setCellFactory(param -> new ListCell<Song>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(song.getDisplayTitle());
                    if (songs.indexOf(song) == currentIndex) {
                        setStyle("-fx-background-color: #00eaff33; -fx-text-fill: #00eaff;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = playlistView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    int index = songs.indexOf(s);
                    if (index >= 0) playSong(index);
                }
            }
        });

        // Drag and drop support
        playlistView.setOnDragOver(event -> {
            if (event.getGestureSource() != playlistView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        playlistView.setOnDragDropped(event -> {
            boolean success = false;
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    if (f.getName().toLowerCase().endsWith(".mp3")) {
                        addSongFile(f.toPath());
                    }
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Progress slider
        progressSlider.setOnMousePressed(e -> seeking = true);
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double percent = progressSlider.getValue() / 100.0;
                Duration seekTime = mediaPlayer.getTotalDuration().multiply(percent);
                mediaPlayer.seek(seekTime);
            }
            seeking = false;
        });

        // Volume slider
        volumeSlider.valueProperty().addListener((obs, o, n) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(n.doubleValue() / 100.0);
        });

        // Search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSongs.setPredicate(song -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                return song.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       song.getArtist().toLowerCase().contains(lowerCaseFilter) ||
                       song.getAlbum().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // Visualizer timer
        vizTimer = new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (now - last < 16_666_666) return; // ~60 FPS
                last = now;
                drawVisualizer();
            }
        };
        
        // Initialize button states
        updatePlayButtonState(false);
        
        // Set icons on control buttons
        setupButtonIcons();
    }
    
    private void setupButtonIcons() {
        if (btnPrev != null) {
            btnPrev.setGraphic(IconFactory.createPreviousIcon());
            btnPrev.setText("");
        }
        if (btnNext != null) {
            btnNext.setGraphic(IconFactory.createNextIcon());
            btnNext.setText("");
        }
        if (btnStop != null) {
            btnStop.setGraphic(IconFactory.createStopIcon());
            btnStop.setText("");
        }
        // Play button icon will be set by updatePlayButtonState
        if (btnPlay != null) {
            btnPlay.setGraphic(IconFactory.createPlayIcon());
            btnPlay.setText("");
        }
    }

    private void initializeEqualizer() {
        if (equalizerBox == null) return;
        equalizerBox.getChildren().clear();
        String[] bandLabels = {"60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz", "14kHz", "16kHz"};
        for (int i = 0; i < 10; i++) {
            final int bandIndex = i;
            VBox bandBox = new VBox(5);
            Label label = new Label(bandLabels[i]);
            label.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");
            Slider slider = new Slider(-12, 12, 0);
            slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
            slider.setPrefHeight(150);
            slider.valueProperty().addListener((obs, o, n) -> {
                eqValues[bandIndex] = n.doubleValue();
                updateEqualizer();
            });
            eqSliders[i] = slider;
            bandBox.getChildren().addAll(label, slider);
            equalizerBox.getChildren().add(bandBox);
        }
    }

    private void updateEqualizer() {
        // Note: JavaFX MediaPlayer doesn't have built-in equalizer
        // This is a placeholder for future implementation
        // You would need a library like JAudioTagger or implement audio processing
    }

    @FXML
    private void onLoadFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Folder with Music (MP3)");
        File dir = dc.showDialog(playlistView.getScene().getWindow());
        if (dir != null) {
            loadSongsFromDirectory(dir);
        }
    }

    @FXML
    private void onAddFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select MP3 Files");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("MP3 Files", "*.mp3")
        );
        List<File> files = fileChooser.showOpenMultipleDialog(playlistView.getScene().getWindow());
        if (files != null) {
            for (File f : files) {
                addSongFile(f.toPath());
            }
        }
    }

    @FXML
    private void onLoadPlaylist() {
        try {
            List<Song> loadedSongs = PlaylistManager.loadPlaylist(playlistView.getScene().getWindow());
            if (!loadedSongs.isEmpty()) {
                songs.clear();
                songs.addAll(loadedSongs);
                if (!songs.isEmpty() && currentIndex < 0) {
                    playSong(0);
                }
            }
        } catch (Exception e) {
            showError("Failed to load playlist", e.getMessage());
        }
    }

    @FXML
    private void onSavePlaylist() {
        try {
            PlaylistManager.savePlaylist(songs, playlistView.getScene().getWindow());
        } catch (Exception e) {
            showError("Failed to save playlist", e.getMessage());
        }
    }

    private void addSongFile(Path path) {
        Song song = new Song(path, path.getFileName().toString());
        songs.add(song);
        // Extract metadata in background
        executor.submit(() -> {
            MetadataExtractor.extractMetadata(song);
            Platform.runLater(() -> {
                playlistView.refresh();
                if (song == songs.get(currentIndex)) {
                    updateSongInfo(song);
                }
            });
        });
    }

    private void loadSongsFromDirectory(File dir) {
        songs.clear();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files != null && files.length > 0) {
            for (File f : files) {
                addSongFile(f.toPath());
            }
            if (!songs.isEmpty()) {
                playSong(0);
            }
        } else {
            showInfo("No MP3 files found", "The selected folder doesn't contain any MP3 files.");
        }
    }

    private void prepareMediaPlayer(Song s) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            
            Media media = new Media(s.getFile().toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

            mediaPlayer.setOnReady(() -> {
                updateSongInfo(s);
                lblTotalTime.setText(format(media.getDuration()));
                updatePlayButtonState(true);

                mediaPlayer.currentTimeProperty().addListener((o, ov, nv) -> {
                    if (!seeking && media.getDuration() != null) {
                        update(nv, media.getDuration());
                    }
                });
                
                // Update button state when status changes
                mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                    Platform.runLater(() -> {
                        if (newStatus == MediaPlayer.Status.PLAYING) {
                            updatePlayButtonState(true);
                        } else if (newStatus == MediaPlayer.Status.PAUSED || 
                                   newStatus == MediaPlayer.Status.STOPPED) {
                            updatePlayButtonState(false);
                        }
                    });
                });

                mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                    handleEndOfMedia();
                }));

                mediaPlayer.setOnError(() -> {
                    Platform.runLater(() -> {
                        showError("Playback Error", mediaPlayer.getError().getMessage());
                        updatePlayButtonState(false);
                    });
                });

                // Setup visualizer
                try {
                    mediaPlayer.setAudioSpectrumInterval(0.05);
                    mediaPlayer.setAudioSpectrumNumBands(64);
                    mediaPlayer.setAudioSpectrumListener((timestamp, duration, mags, phases) -> {
                        int len = Math.min(mags.length, magnitudes.length);
                        for (int i = 0; i < len; i++) {
                            magnitudes[i] = Math.max(0, mags[i] + 60);
                        }
                    });
                    if (vizTimer != null) vizTimer.start();
                } catch (Exception ex) {
                    if (vizTimer != null) vizTimer.stop();
                }

                // Update album art
                updateAlbumArt(s);

                mediaPlayer.play();
                updatePlayButtonState(true);
                playlistView.refresh();
            });

            mediaPlayer.setOnError(() -> {
                Platform.runLater(() -> {
                    showError("Media Error", "Failed to load: " + s.getFile().getFileName());
                });
            });

        } catch (Exception e) {
            showError("Error", "Failed to load media: " + e.getMessage());
        }
    }

    private void updateSongInfo(Song s) {
        lblNowPlaying.setText(s.getTitle());
        lblArtist.setText(s.getArtist());
        lblAlbum.setText(s.getAlbum());
        lblGenre.setText(s.getGenre());
        lblYear.setText(s.getYear());
    }

    private void updateAlbumArt(Song s) {
        try {
            Image art = s.getAlbumArt();
            if (art != null) {
                albumArt.setImage(art);
            } else {
                // Try to load from file system
                Path file = s.getFile();
                File jpg = new File(file.toString().replaceAll("(?i)\\.mp3$", ".jpg"));
                File png = new File(file.toString().replaceAll("(?i)\\.mp3$", ".png"));
                if (jpg.exists()) {
                    albumArt.setImage(new Image(new FileInputStream(jpg), 200, 200, true, true));
                } else if (png.exists()) {
                    albumArt.setImage(new Image(new FileInputStream(png), 200, 200, true, true));
                } else {
                    // Set default placeholder
                    albumArt.setImage(null);
                }
            }
        } catch (Exception e) {
            albumArt.setImage(null);
        }
    }

    private void playSong(int index) {
        if (index < 0 || index >= songs.size()) return;
        currentIndex = index;
        Song s = songs.get(index);
        
        // Ensure metadata is extracted
        if (s.getArtist().equals("Unknown Artist")) {
            executor.submit(() -> {
                MetadataExtractor.extractMetadata(s);
                Platform.runLater(() -> {
                    updateSongInfo(s);
                    updateAlbumArt(s);
                });
            });
        }
        
        prepareMediaPlayer(s);
    }

    private void handleEndOfMedia() {
        if (repeatMode == 2) {
            playSong(currentIndex);
            return;
        }
        if (shuffle) {
            if (songs.size() > 1) {
                int next;
                do {
                    next = random.nextInt(songs.size());
                } while (next == currentIndex && songs.size() > 1);
                playSong(next);
            }
            return;
        }
        if (repeatMode == 1) {
            if (currentIndex + 1 < songs.size()) {
                playSong(currentIndex + 1);
            } else {
                playSong(0);
            }
            return;
        }
        if (currentIndex + 1 < songs.size()) {
            playSong(currentIndex + 1);
        }
    }

    private void update(Duration current, Duration total) {
        if (total == null || total.isUnknown()) return;
        if (total.toMillis() > 0) {
            progressSlider.setValue(current.toMillis() / total.toMillis() * 100.0);
        }
        lblCurrentTime.setText(format(current));
    }

    private String format(Duration d) {
        if (d == null || d.isUnknown()) return "00:00";
        int s = (int) Math.floor(d.toSeconds());
        int m = s / 60;
        int sec = s % 60;
        return String.format("%02d:%02d", m, sec);
    }

    @FXML
    private void onPlay() {
        if (mediaPlayer == null) {
            // No media player, start playing first song
            if (!songs.isEmpty()) {
                if (currentIndex < 0) {
                    playSong(0);
                } else {
                    playSong(currentIndex);
                }
            }
        } else {
            // Toggle play/pause
            MediaPlayer.Status status = mediaPlayer.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                updatePlayButtonState(false);
            } else if (status == MediaPlayer.Status.STOPPED) {
                // If stopped, restart from beginning
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
                updatePlayButtonState(true);
            } else {
                // Paused, resume playing
                mediaPlayer.play();
                updatePlayButtonState(true);
            }
        }
    }

    @FXML
    private void onPause() {
        // This is now handled by onPlay toggle, but keep for compatibility
        onPlay();
    }

    @FXML
    private void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO); // Reset to beginning
            progressSlider.setValue(0);
            lblCurrentTime.setText("00:00");
            updatePlayButtonState(false);
        }
    }
    
    private void updatePlayButtonState(boolean isPlaying) {
        if (btnPlay != null) {
            if (isPlaying) {
                btnPlay.setGraphic(IconFactory.createPauseIcon());
                btnPlay.getStyleClass().remove("play-button");
                if (!btnPlay.getStyleClass().contains("pause-button")) {
                    btnPlay.getStyleClass().add("pause-button");
                }
            } else {
                btnPlay.setGraphic(IconFactory.createPlayIcon());
                btnPlay.getStyleClass().remove("pause-button");
                if (!btnPlay.getStyleClass().contains("play-button")) {
                    btnPlay.getStyleClass().add("play-button");
                }
            }
        }
    }

    @FXML
    private void onPrev() {
        if (songs.isEmpty()) return;
        
        if (shuffle) {
            if (songs.size() > 1) {
                int prev;
                do {
                    prev = random.nextInt(songs.size());
                } while (prev == currentIndex && songs.size() > 1);
                playSong(prev);
            } else if (songs.size() == 1) {
                // If only one song and shuffle, restart it
                playSong(0);
            }
        } else {
            if (currentIndex > 0) {
                playSong(currentIndex - 1);
            } else if (repeatMode == 1) {
                // Repeat all - go to last song
                playSong(songs.size() - 1);
            } else {
                // At beginning, restart current song
                if (currentIndex >= 0) {
                    playSong(currentIndex);
                }
            }
        }
    }

    @FXML
    private void onNext() {
        if (songs.isEmpty()) return;
        nextSong();
    }

    private void nextSong() {
        if (shuffle) {
            if (songs.size() > 1) {
                int next;
                do {
                    next = random.nextInt(songs.size());
                } while (next == currentIndex && songs.size() > 1);
                playSong(next);
            }
        } else {
            if (currentIndex + 1 < songs.size()) {
                playSong(currentIndex + 1);
            } else if (repeatMode == 1) {
                playSong(0);
            }
        }
    }

    public void togglePlayPause() {
        onPlay(); // Use the same logic as the play button
    }

    public void nextFromShortcut() {
        nextSong();
    }

    public void prevFromShortcut() {
        onPrev();
    }

    @FXML
    private void onShuffleToggle() {
        shuffle = btnShuffle.isSelected();
    }

    @FXML
    private void onRepeatToggle() {
        if (!btnRepeat.isSelected()) {
            repeatMode = 0;
            return;
        }
        if (repeatMode == 0) {
            repeatMode = 1;
            btnRepeat.setText("Repeat All");
        } else if (repeatMode == 1) {
            repeatMode = 2;
            btnRepeat.setText("Repeat One");
        } else {
            repeatMode = 0;
            btnRepeat.setSelected(false);
            btnRepeat.setText("Repeat");
        }
    }

    private void drawVisualizer() {
        if (visualizerCanvas == null) return;
        GraphicsContext gc = visualizerCanvas.getGraphicsContext2D();
        double w = visualizerCanvas.getWidth();
        double h = visualizerCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        switch (vizMode) {
            case 0: // Bars
                drawBars(gc, w, h);
                break;
            case 1: // Circle
                drawCircle(gc, w, h);
                break;
            case 2: // Wave
                drawWave(gc, w, h);
                break;
            case 3: // Spectrum
                drawSpectrum(gc, w, h);
                break;
        }
    }

    private void drawBars(GraphicsContext gc, double w, double h) {
        int bands = 64;
        double bandWidth = w / bands;
        gc.setFill(Color.web("#00eaff"));
        for (int i = 0; i < bands; i++) {
            double mag = 0;
            if (i < magnitudes.length) mag = Math.max(0, Math.min(60, magnitudes[i]));
            double barH = (mag / 60.0) * h * 0.9;
            gc.fillRect(i * bandWidth, h - barH, bandWidth * 0.8, barH);
        }
    }

    private void drawCircle(GraphicsContext gc, double w, double h) {
        double centerX = w / 2;
        double centerY = h / 2;
        double radius = Math.min(w, h) / 2 - 20;
        int bands = 64;
        
        for (int i = 0; i < bands; i++) {
            double angle = (2 * Math.PI * i) / bands;
            double mag = 0;
            if (i < magnitudes.length) mag = Math.max(0, Math.min(60, magnitudes[i]));
            double barLength = (mag / 60.0) * radius * 0.5;
            
            double x1 = centerX + Math.cos(angle) * radius;
            double y1 = centerY + Math.sin(angle) * radius;
            double x2 = centerX + Math.cos(angle) * (radius + barLength);
            double y2 = centerY + Math.sin(angle) * (radius + barLength);
            
            gc.setStroke(Color.web("#00eaff"));
            gc.setLineWidth(2);
            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    private void drawWave(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web("#00eaff"));
        gc.setLineWidth(2);
        gc.beginPath();
        gc.moveTo(0, h / 2);
        
        int bands = 64;
        for (int i = 0; i < bands; i++) {
            double mag = 0;
            if (i < magnitudes.length) mag = Math.max(0, Math.min(60, magnitudes[i]));
            double x = (i / (double) bands) * w;
            double y = h / 2 - (mag / 60.0) * h * 0.4;
            gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private void drawSpectrum(GraphicsContext gc, double w, double h) {
        int bands = 64;
        double bandWidth = w / bands;
        for (int i = 0; i < bands; i++) {
            double mag = 0;
            if (i < magnitudes.length) mag = Math.max(0, Math.min(60, magnitudes[i]));
            double barH = (mag / 60.0) * h;
            
            // Color gradient based on frequency
            double hue = (i / (double) bands) * 240; // Blue to red
            gc.setFill(Color.hsb(hue, 0.8, 1.0));
            gc.fillRect(i * bandWidth, h - barH, bandWidth * 0.9, barH);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        executor.shutdown();
        if (vizTimer != null) vizTimer.stop();
    }
}
