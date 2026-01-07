package com.starkplayer.util;

import com.starkplayer.model.Song;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlaylistManager {
    
    public static void savePlaylist(ObservableList<Song> songs, Window window) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Playlist");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Playlist Files", "*.m3u")
        );
        fileChooser.setInitialFileName("playlist.m3u");
        
        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("#EXTM3U");
                for (Song song : songs) {
                    writer.println("#EXTINF:" + song.getDuration() + "," + song.getDisplayTitle());
                    writer.println(song.getFile().toAbsolutePath().toString());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to save playlist: " + e.getMessage(), e);
            }
        }
    }
    
    public static List<Song> loadPlaylist(Window window) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Playlist");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Playlist Files", "*.m3u")
        );
        
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            List<Song> songs = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                String line;
                String title = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#EXTINF:")) {
                        // Extract title from EXTINF line
                        int commaIndex = line.indexOf(',');
                        if (commaIndex > 0 && commaIndex < line.length() - 1) {
                            title = line.substring(commaIndex + 1);
                        }
                    } else if (!line.isEmpty() && !line.startsWith("#")) {
                        // This is a file path
                        Path path = Paths.get(line);
                        if (Files.exists(path)) {
                            Song song = new Song(path, title != null ? title : path.getFileName().toString());
                            MetadataExtractor.extractMetadata(song);
                            songs.add(song);
                            title = null; // Reset for next entry
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load playlist: " + e.getMessage(), e);
            }
            return songs;
        }
        return new ArrayList<>();
    }
}

