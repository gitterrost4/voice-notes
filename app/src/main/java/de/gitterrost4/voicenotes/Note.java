package de.gitterrost4.voicenotes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import android.media.MediaMetadataRetriever;

public final class Note {
    public enum Type {
        AUDIO, TEXT
    }
    
    private final String id;
    private final Type type;
    private final String category;
    private final LocalDateTime timestamp;
    private final String content; // For text notes: the text content; For audio notes: the file path
    private boolean done;
    
    public Note(String id, Type type, String category, LocalDateTime timestamp, String content) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.timestamp = timestamp;
        this.content = content;
        this.done = false;
    }
    
    // Getters
    public String getId() { return id; }
    public Type getType() { return type; }
    public String getCategory() { return category; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getContent() { return content; }
    public boolean isDone() { return done; }
    
    // For audio notes, content is the file path
    public String getFilePath() {
        return type == Type.AUDIO ? content : null;
    }
    
    // For text notes, content is the text
    public String getText() {
        return type == Type.TEXT ? content : null;
    }
    
    public void setDone(boolean done) {
        this.done = done;
    }
    
    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        return timestamp.format(formatter);
    }
    
    public String getDurationString() {
        if (type != Type.AUDIO) {
            return "";
        }
        
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(content);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            
            if (durationStr != null) {
                long durationMs = Long.parseLong(durationStr);
                long seconds = (durationMs / 1000) % 60;
                long minutes = (durationMs / (1000 * 60)) % 60;
                
                if (minutes > 0) {
                    return String.format("(%d:%02d)", minutes, seconds);
                } else {
                    return String.format("(0:%02d)", seconds);
                }
            }
        } catch (Exception e) {
            // If we can't get duration, just return empty string
        }
        return "";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Note note = (Note) obj;
        return Objects.equals(id, note.id) &&
               type == note.type &&
               Objects.equals(category, note.category) &&
               Objects.equals(timestamp, note.timestamp) &&
               Objects.equals(content, note.content);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, type, category, timestamp, content);
    }
}