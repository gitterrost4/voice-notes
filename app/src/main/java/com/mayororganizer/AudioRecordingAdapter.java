package com.mayororganizer;

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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class AudioRecordingAdapter extends RecyclerView.Adapter<AudioRecordingAdapter.ViewHolder> {
    
    private final List<AudioRecording> recordings;
    private final Consumer<AudioRecording> onPlayClick;
    private final Consumer<AudioRecording> onDeleteClick;
    private final Consumer<AudioRecording> onDoneClick;
    private final MainActivity mainActivity;
    
    public AudioRecordingAdapter(List<AudioRecording> recordings,
                                Consumer<AudioRecording> onPlayClick,
                                Consumer<AudioRecording> onDeleteClick,
                                Consumer<AudioRecording> onDoneClick,
                                MainActivity mainActivity) {
        this.recordings = recordings;
        this.onPlayClick = onPlayClick;
        this.onDeleteClick = onDeleteClick;
        this.onDoneClick = onDoneClick;
        this.mainActivity = mainActivity;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio_recording, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioRecording recording = recordings.get(position);
        holder.bind(recording);
    }
    
    @Override
    public int getItemCount() {
        return recordings.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView timestampText;
        private final ImageView playButton;
        private final ImageButton doneButton;
        private final ImageButton deleteButton;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.recordingTitle);
            timestampText = itemView.findViewById(R.id.recordingTimestamp);
            playButton = itemView.findViewById(R.id.playButton);
            doneButton = itemView.findViewById(R.id.doneButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
        
        void bind(AudioRecording recording) {
            // Apply visual styling based on done status
            String duration = recording.getDurationString();
            String categoryWithDuration = recording.getCategory() + " " + duration;
            
            if (recording.isDone()) {
                titleText.setTextColor(0xFF888888); // Gray color
                titleText.setText("✓ " + categoryWithDuration);
                timestampText.setTextColor(0xFF666666);
            } else {
                titleText.setTextColor(0xFFFFFFFF); // White color for dark mode
                titleText.setText(categoryWithDuration);
                timestampText.setTextColor(0xFFBBBBBB); // Light gray for dark mode
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm");
            timestampText.setText(recording.getTimestamp().format(formatter));
            
            // Update play button based on playback state
            boolean isPlaying = mainActivity.isCurrentlyPlaying(recording);
            playButton.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            playButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
            playButton.setOnClickListener(v -> {
                if (isPlaying) {
                    mainActivity.stopCurrentPlayback();
                    mainActivity.getAdapter().notifyDataSetChanged();
                } else {
                    onPlayClick.accept(recording);
                }
            });
            
            // Update done button based on current status
            doneButton.setImageResource(android.R.drawable.ic_menu_view); // Same checkmark icon for both states
            if (recording.isDone()) {
                doneButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green when done
            } else {
                doneButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#757575"))); // Gray when not done
            }
            doneButton.setOnClickListener(v -> onDoneClick.accept(recording));
            
            // Set delete button color
            deleteButton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336"))); // Red
            deleteButton.setOnClickListener(v -> onDeleteClick.accept(recording));
        }
    }
}