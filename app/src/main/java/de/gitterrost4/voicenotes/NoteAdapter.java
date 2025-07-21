package de.gitterrost4.voicenotes;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_AUDIO = 0;
    private static final int TYPE_TEXT = 1;
    
    private final List<Note> notes;
    private final OnNoteActionListener listener;
    private final MainActivity mainActivity;
    
    public interface OnNoteActionListener {
        void onPlayAudio(Note note);
        void onToggleDone(Note note);
        void onDelete(Note note);
    }
    
    public NoteAdapter(List<Note> notes, OnNoteActionListener listener, MainActivity mainActivity) {
        this.notes = notes;
        this.listener = listener;
        this.mainActivity = mainActivity;
    }
    
    @Override
    public int getItemViewType(int position) {
        return notes.get(position).getType() == Note.Type.AUDIO ? TYPE_AUDIO : TYPE_TEXT;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AUDIO) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_recording, parent, false);
            return new AudioViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_text_note, parent, false);
            return new TextViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Note note = notes.get(position);
        
        if (holder instanceof AudioViewHolder) {
            bindAudioNote((AudioViewHolder) holder, note);
        } else if (holder instanceof TextViewHolder) {
            bindTextNote((TextViewHolder) holder, note);
        }
    }
    
    private void bindAudioNote(AudioViewHolder holder, Note note) {
        String titleWithDuration = note.getCategory() + " " + note.getDurationString();
        holder.title.setText(titleWithDuration);
        holder.timestamp.setText(note.getFormattedTimestamp());
        
        // Set text colors based on completion status
        if (note.isDone()) {
            holder.title.setTextColor(Color.parseColor("#888888"));
            holder.timestamp.setTextColor(Color.parseColor("#666666"));
        } else {
            holder.title.setTextColor(Color.parseColor("#FFFFFF"));
            holder.timestamp.setTextColor(Color.parseColor("#CCCCCC"));
        }
        
        // Update play button based on playing/paused state
        boolean isPlaying = mainActivity.isCurrentlyPlaying(note);
        boolean isPaused = mainActivity.isCurrentlyPaused(note);
        
        if (isPlaying && !isPaused) {
            holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
            holder.playButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        } else if (isPaused) {
            holder.playButton.setImageResource(android.R.drawable.ic_media_play);
            holder.playButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        } else {
            holder.playButton.setImageResource(android.R.drawable.ic_media_play);
            holder.playButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
        
        // Update done button
        if (note.isDone()) {
            holder.doneButton.setImageResource(android.R.drawable.ic_menu_view);
            holder.doneButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            holder.doneButton.setImageResource(android.R.drawable.ic_menu_view);
            holder.doneButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        }
        
        // Set delete button color
        holder.deleteButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
        
        // Set click listeners
        holder.playButton.setOnClickListener(v -> listener.onPlayAudio(note));
        holder.doneButton.setOnClickListener(v -> listener.onToggleDone(note));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(note));
    }
    
    private void bindTextNote(TextViewHolder holder, Note note) {
        holder.title.setText(note.getCategory());
        holder.text.setText(note.getText());
        holder.timestamp.setText(note.getFormattedTimestamp());
        
        // Set text colors based on completion status
        if (note.isDone()) {
            holder.title.setTextColor(Color.parseColor("#888888"));
            holder.text.setTextColor(Color.parseColor("#666666"));
            holder.timestamp.setTextColor(Color.parseColor("#666666"));
        } else {
            holder.title.setTextColor(Color.parseColor("#FFFFFF"));
            holder.text.setTextColor(Color.parseColor("#CCCCCC"));
            holder.timestamp.setTextColor(Color.parseColor("#CCCCCC"));
        }
        
        // Update done button
        if (note.isDone()) {
            holder.doneButton.setImageResource(android.R.drawable.ic_menu_view);
            holder.doneButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            holder.doneButton.setImageResource(android.R.drawable.ic_menu_view);
            holder.doneButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        }
        
        // Set delete button color
        holder.deleteButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
        
        // Set click listeners
        holder.doneButton.setOnClickListener(v -> listener.onToggleDone(note));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(note));
    }
    
    @Override
    public int getItemCount() {
        return notes.size();
    }
    
    static class AudioViewHolder extends RecyclerView.ViewHolder {
        TextView title, timestamp;
        ImageView playButton;
        ImageButton doneButton, deleteButton;
        
        AudioViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recordingTitle);
            timestamp = itemView.findViewById(R.id.recordingTimestamp);
            playButton = itemView.findViewById(R.id.playButton);
            doneButton = itemView.findViewById(R.id.doneButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
    
    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView title, text, timestamp;
        ImageView textIcon;
        ImageButton doneButton, deleteButton;
        
        TextViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            text = itemView.findViewById(R.id.noteText);
            timestamp = itemView.findViewById(R.id.noteTimestamp);
            textIcon = itemView.findViewById(R.id.textIcon);
            doneButton = itemView.findViewById(R.id.doneButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}