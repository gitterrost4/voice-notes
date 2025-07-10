package com.voicenotes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import android.media.MediaMetadataRetriever;

public final class AudioRecording {
    private final String filePath;
    private final String category;
    private final LocalDateTime timestamp;
    private boolean isDone;
    
    public AudioRecording(String filePath, String category, LocalDateTime timestamp) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.isDone = false;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getCategory() {
        return category;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public boolean isDone() {
        return isDone;
    }
    
    public void setDone(boolean done) {
        this.isDone = done;
    }
    
    public String getDisplayName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        return String.format("%s - %s", category, timestamp.format(formatter));
    }
    
    public String getDurationString() {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filePath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            
            if (durationStr != null) {
                long durationMs = Long.parseLong(durationStr);
                long seconds = durationMs / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                
                if (minutes > 0) {
                    return String.format("(%d:%02d)", minutes, seconds);
                } else {
                    return String.format("(0:%02d)", seconds);
                }
            }
        } catch (Exception e) {
            // If we can't get duration, don't show anything
        }
        return "";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioRecording that = (AudioRecording) o;
        return Objects.equals(filePath, that.filePath) &&
               Objects.equals(category, that.category) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(filePath, category, timestamp);
    }
    
    @Override
    public String toString() {
        return "AudioRecording{" +
                "filePath='" + filePath + '\'' +
                ", category='" + category + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}