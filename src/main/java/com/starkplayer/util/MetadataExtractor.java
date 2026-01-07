package com.starkplayer.util;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.starkplayer.model.Song;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;

public class MetadataExtractor {
    
    public static void extractMetadata(Song song) {
        try {
            File file = song.getFile().toFile();
            Mp3File mp3file = new Mp3File(file);
            
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                extractFromID3v2(song, id3v2Tag);
            } else if (mp3file.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                extractFromID3v1(song, id3v1Tag);
            }
            
            // Set duration
            if (mp3file.getLengthInSeconds() > 0) {
                song.setDuration(mp3file.getLengthInSeconds());
            }
            
        } catch (Exception e) {
            // If metadata extraction fails, use defaults
            System.err.println("Failed to extract metadata for: " + song.getFile() + " - " + e.getMessage());
        }
    }
    
    private static void extractFromID3v2(Song song, ID3v2 tag) {
        // Extract title
        String title = tag.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            song.setTitle(title.trim());
        }
        
        // Extract artist
        String artist = tag.getArtist();
        song.setArtist(artist);
        
        // Extract album
        String album = tag.getAlbum();
        song.setAlbum(album);
        
        // Extract genre
        String genre = tag.getGenreDescription();
        song.setGenre(genre != null ? genre : "");
        
        // Extract year
        String year = tag.getYear();
        song.setYear(year != null ? year : "");
        
        // Extract album art
        byte[] imageData = tag.getAlbumImage();
        if (imageData != null && imageData.length > 0) {
            try {
                String mimeType = tag.getAlbumImageMimeType();
                if (mimeType != null && (mimeType.startsWith("image/"))) {
                    Image image = new Image(new ByteArrayInputStream(imageData), 200, 200, true, true);
                    song.setAlbumArt(image);
                }
            } catch (Exception e) {
                // Ignore image loading errors
            }
        }
    }
    
    private static void extractFromID3v1(Song song, ID3v1 tag) {
        // Extract title
        String title = tag.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            song.setTitle(title.trim());
        }
        
        // Extract artist
        String artist = tag.getArtist();
        song.setArtist(artist);
        
        // Extract album
        String album = tag.getAlbum();
        song.setAlbum(album);
        
        // Extract genre
        String genre = tag.getGenreDescription();
        song.setGenre(genre != null ? genre : "");
        
        // Extract year
        String year = tag.getYear();
        song.setYear(year != null ? year : "");
    }
}
