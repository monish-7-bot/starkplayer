package com.starkplayer.model;

import javafx.scene.image.Image;
import java.nio.file.Path;

public class Song {
    private final Path file;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String year;
    private Image albumArt;
    private long duration; // in seconds

    public Song(Path file, String title) {
        this.file = file;
        this.title = title != null ? title : file.getFileName().toString();
        this.artist = "Unknown Artist";
        this.album = "Unknown Album";
        this.genre = "";
        this.year = "";
        this.duration = 0;
    }

    public Path getFile() { return file; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public String getYear() { return year; }
    public Image getAlbumArt() { return albumArt; }
    public long getDuration() { return duration; }

    public void setTitle(String title) { this.title = title != null ? title : file.getFileName().toString(); }
    public void setArtist(String artist) { this.artist = artist != null && !artist.isEmpty() ? artist : "Unknown Artist"; }
    public void setAlbum(String album) { this.album = album != null && !album.isEmpty() ? album : "Unknown Album"; }
    public void setGenre(String genre) { this.genre = genre != null ? genre : ""; }
    public void setYear(String year) { this.year = year != null ? year : ""; }
    public void setAlbumArt(Image albumArt) { this.albumArt = albumArt; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getDisplayTitle() {
        if (artist != null && !artist.equals("Unknown Artist") && !artist.isEmpty()) {
            return artist + " - " + title;
        }
        return title;
    }

    public String toString() {
        return getDisplayTitle();
    }
}
