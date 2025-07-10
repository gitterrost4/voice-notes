package com.voicenotes;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String RECORDINGS_FILE = "recordings.json";
    private static final String PREFS_NAME = "voice_notes_prefs";
    private static final String CATEGORIES_KEY = "categories";
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "ToDos", "Reminders", "Town Meeting"
    );
    
    private List<String> categories;
    
    private Gson gson;
    
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private Button recordButton;
    private View recordIndicator;
    private Spinner categorySpinner;
    private RecyclerView activeRecordingsRecyclerView;
    private RecyclerView completedRecordingsRecyclerView;
    private TextView completedSectionHeader;
    private AudioRecordingAdapter activeAdapter;
    private AudioRecordingAdapter completedAdapter;
    private List<AudioRecording> recordings;
    private List<AudioRecording> activeRecordings;
    private List<AudioRecording> completedRecordings;
    private Optional<String> currentRecordingPath = Optional.empty();
    private Optional<AudioRecording> currentlyPlaying = Optional.empty();
    private boolean isRecording = false;
    private boolean isCompletedSectionExpanded = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeGson();
        loadCategories();
        initializeViews();
        setupCategorySpinner();
        setupRecyclerView();
        setupButtonListeners();
        loadRecordings();
        
        checkPermissions();
    }
    
    private void initializeGson() {
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .create();
    }
    
    private void initializeViews() {
        recordButton = findViewById(R.id.recordButton);
        recordIndicator = findViewById(R.id.recordIndicator);
        categorySpinner = findViewById(R.id.categorySpinner);
        activeRecordingsRecyclerView = findViewById(R.id.activeRecordingsRecyclerView);
        completedRecordingsRecyclerView = findViewById(R.id.completedRecordingsRecyclerView);
        completedSectionHeader = findViewById(R.id.completedSectionHeader);
    }
    
    private void loadCategories() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String categoriesJson = prefs.getString(CATEGORIES_KEY, null);
        if (categoriesJson != null) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            categories = new ArrayList<>(gson.fromJson(categoriesJson, listType));
        } else {
            categories = new ArrayList<>(DEFAULT_CATEGORIES);
        }
    }
    
    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }
    
    private void setupRecyclerView() {
        recordings = new ArrayList<>();
        activeRecordings = new ArrayList<>();
        completedRecordings = new ArrayList<>();
        
        activeAdapter = new AudioRecordingAdapter(activeRecordings, this::playRecording, this::deleteRecording, this::toggleDoneStatus, this);
        activeRecordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activeRecordingsRecyclerView.setAdapter(activeAdapter);
        
        completedAdapter = new AudioRecordingAdapter(completedRecordings, this::playRecording, this::deleteRecording, this::toggleDoneStatus, this);
        completedRecordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedRecordingsRecyclerView.setAdapter(completedAdapter);
        
        setupCompletedSectionHeader();
    }
    
    private void setupCompletedSectionHeader() {
        completedSectionHeader.setOnClickListener(v -> toggleCompletedSection());
        updateCompletedSectionHeader();
    }
    
    private void setupButtonListeners() {
        recordButton.setOnClickListener(v -> toggleRecording());
    }
    
    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }
    
    private void updateRecordButtonAppearance() {
        if (isRecording) {
            recordIndicator.setBackgroundResource(R.drawable.stop_square);
        } else {
            recordIndicator.setBackgroundResource(R.drawable.record_dot);
        }
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    private void startRecording() {
        if (!hasRecordingPermission()) {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedCategory = categories.get(categorySpinner.getSelectedItemPosition());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s.3gp", selectedCategory, timestamp);
        
        File recordingsDir = new File(getExternalFilesDir(null), "recordings");
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
        
        File outputFile = new File(recordingsDir, fileName);
        currentRecordingPath = Optional.of(outputFile.getAbsolutePath());
        
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(currentRecordingPath.get());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            updateRecordButtonAppearance();
            
        } catch (IOException e) {
            Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            currentRecordingPath = Optional.empty();
            isRecording = false;
            updateRecordButtonAppearance();
        }
    }
    
    private void stopRecording() {
        if (mediaRecorder != null && currentRecordingPath.isPresent()) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                String category = categories.get(categorySpinner.getSelectedItemPosition());
                AudioRecording recording = new AudioRecording(
                    currentRecordingPath.get(),
                    category,
                    LocalDateTime.now()
                );
                
                recordings.add(0, recording);
                refreshRecordingLists();
                
                Toast.makeText(this, R.string.recording_saved, Toast.LENGTH_SHORT).show();
                saveRecordings(); // Save to persistent storage
                
            } catch (RuntimeException e) {
                Toast.makeText(this, "Stop recording failed", Toast.LENGTH_SHORT).show();
            } finally {
                currentRecordingPath = Optional.empty();
                isRecording = false;
                updateRecordButtonAppearance();
            }
        }
    }
    
    private boolean hasRecordingPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void playRecording(AudioRecording recording) {
        // Stop any currently playing audio
        stopCurrentPlayback();
        
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(recording.getFilePath());
            mediaPlayer.setOnCompletionListener(mp -> {
                stopCurrentPlayback();
                activeAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show();
                stopCurrentPlayback();
                activeAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();
                return true;
            });
            
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            currentlyPlaying = Optional.of(recording);
            activeAdapter.notifyDataSetChanged();
            completedAdapter.notifyDataSetChanged();
            
            Toast.makeText(this, "Playing: " + recording.getCategory(), Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            Toast.makeText(this, "Failed to play recording: " + e.getMessage(), 
                         Toast.LENGTH_SHORT).show();
            stopCurrentPlayback();
        }
    }
    
    public void stopCurrentPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentlyPlaying = Optional.empty();
    }
    
    public boolean isCurrentlyPlaying(AudioRecording recording) {
        return currentlyPlaying.map(playing -> playing.equals(recording)).orElse(false);
    }
    
    public AudioRecordingAdapter getAdapter() {
        // Return the adapter that contains the currently playing recording
        if (currentlyPlaying.isPresent()) {
            AudioRecording playing = currentlyPlaying.get();
            if (activeRecordings.contains(playing)) {
                return activeAdapter;
            } else if (completedRecordings.contains(playing)) {
                return completedAdapter;
            }
        }
        return activeAdapter; // Default to active adapter
    }
    
    private void toggleDoneStatus(AudioRecording recording) {
        recording.setDone(!recording.isDone());
        refreshRecordingLists();
        saveRecordings(); // Save updated status
    }
    
    private void deleteRecording(AudioRecording recording) {
        recordings.remove(recording);
        refreshRecordingLists();
        
        // Delete the actual file
        new File(recording.getFilePath()).delete();
        saveRecordings(); // Save updated list
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopCurrentPlayback();
        if (activeAdapter != null) {
            activeAdapter.notifyDataSetChanged();
        }
        if (completedAdapter != null) {
            completedAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCurrentPlayback();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload categories in case they were changed in settings
        loadCategories();
        setupCategorySpinner();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void saveRecordings() {
        try {
            File file = new File(getFilesDir(), RECORDINGS_FILE);
            // Filter out recordings where the audio file no longer exists
            List<AudioRecording> validRecordings = recordings.stream()
                .filter(recording -> new File(recording.getFilePath()).exists())
                .collect(Collectors.toList());
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(validRecordings, writer);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save recordings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadRecordings() {
        try {
            File file = new File(getFilesDir(), RECORDINGS_FILE);
            if (!file.exists()) {
                return; // No saved recordings yet
            }
            
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<AudioRecording>>(){}.getType();
                List<AudioRecording> loadedRecordings = gson.fromJson(reader, listType);
                
                if (loadedRecordings != null) {
                    // Filter out recordings where the audio file no longer exists
                    List<AudioRecording> validRecordings = loadedRecordings.stream()
                        .filter(recording -> new File(recording.getFilePath()).exists())
                        .collect(Collectors.toList());
                    
                    recordings.clear();
                    recordings.addAll(validRecordings);
                    refreshRecordingLists();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load recordings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void refreshRecordingLists() {
        activeRecordings.clear();
        completedRecordings.clear();
        
        for (AudioRecording recording : recordings) {
            if (recording.isDone()) {
                completedRecordings.add(recording);
            } else {
                activeRecordings.add(recording);
            }
        }
        
        activeAdapter.notifyDataSetChanged();
        completedAdapter.notifyDataSetChanged();
        updateCompletedSectionHeader();
    }
    
    private void toggleCompletedSection() {
        isCompletedSectionExpanded = !isCompletedSectionExpanded;
        updateCompletedSectionVisibility();
        updateCompletedSectionHeader();
    }
    
    private void updateCompletedSectionVisibility() {
        int visibility = isCompletedSectionExpanded ? View.VISIBLE : View.GONE;
        completedRecordingsRecyclerView.setVisibility(visibility);
    }
    
    private void updateCompletedSectionHeader() {
        int completedCount = completedRecordings.size();
        
        if (completedCount == 0) {
            completedSectionHeader.setVisibility(View.GONE);
            completedRecordingsRecyclerView.setVisibility(View.GONE);
        } else {
            completedSectionHeader.setVisibility(View.VISIBLE);
            
            String headerText = getString(R.string.show_completed, completedCount);
            if (isCompletedSectionExpanded) {
                headerText = getString(R.string.hide_completed);
                completedSectionHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_up_float, 0);
            } else {
                completedSectionHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
            }
            
            completedSectionHeader.setText(headerText);
            updateCompletedSectionVisibility();
        }
    }
    
    // Custom serializer for LocalDateTime
    private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime dateTime, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
    
    // Custom deserializer for LocalDateTime
    private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}