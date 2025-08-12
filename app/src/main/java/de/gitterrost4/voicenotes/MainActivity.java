package de.gitterrost4.voicenotes;

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
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.auth.oauth2.GoogleCredentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import java.io.InputStream;
import android.util.Base64;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String NOTES_FILE = "notes.json";
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
    private ImageButton addTextNoteButton;
    private View recordIndicator;
    private Spinner categorySpinner;
    private RecyclerView activeNotesRecyclerView;
    private RecyclerView completedNotesRecyclerView;
    private TextView completedSectionHeader;
    private NoteAdapter activeAdapter;
    private NoteAdapter completedAdapter;
    private List<Note> notes;
    private List<Note> activeNotes;
    private List<Note> completedNotes;
    private Optional<String> currentRecordingPath = Optional.empty();
    private Optional<Note> currentlyPlaying = Optional.empty();
    private boolean isRecording = false;
    private boolean isCompletedSectionExpanded = false;
    private ExecutorService speechExecutor = Executors.newSingleThreadExecutor();
    
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
        loadNotes();
        
        checkPermissions();
    }
    
    private void initializeGson() {
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .registerTypeAdapter(Note.class, new NoteDeserializer())
            .create();
    }
    
    private void initializeViews() {
        recordButton = findViewById(R.id.recordButton);
        addTextNoteButton = findViewById(R.id.addTextNoteButton);
        recordIndicator = findViewById(R.id.recordIndicator);
        categorySpinner = findViewById(R.id.categorySpinner);
        activeNotesRecyclerView = findViewById(R.id.activeRecordingsRecyclerView);
        completedNotesRecyclerView = findViewById(R.id.completedRecordingsRecyclerView);
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
        notes = new ArrayList<>();
        activeNotes = new ArrayList<>();
        completedNotes = new ArrayList<>();
        
        NoteAdapter.OnNoteActionListener listener = new NoteAdapter.OnNoteActionListener() {
            @Override
            public void onPlayAudio(Note note) { playNote(note); }
            @Override
            public void onToggleDone(Note note) { toggleDoneStatus(note); }
            @Override
            public void onDelete(Note note) { deleteNote(note); }
        };
        
        activeAdapter = new NoteAdapter(activeNotes, listener, this);
        activeNotesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activeNotesRecyclerView.setAdapter(activeAdapter);
        
        completedAdapter = new NoteAdapter(completedNotes, listener, this);
        completedNotesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedNotesRecyclerView.setAdapter(completedAdapter);
        
        setupCompletedSectionHeader();
    }
    
    private void setupCompletedSectionHeader() {
        completedSectionHeader.setOnClickListener(v -> toggleCompletedSection());
        updateCompletedSectionHeader();
    }
    
    private void setupButtonListeners() {
        recordButton.setOnClickListener(v -> toggleRecording());
        addTextNoteButton.setOnClickListener(v -> showAddTextNoteDialog());
    }
    
    private void showAddTextNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_text_note);
        
        final EditText input = new EditText(this);
        input.setHint(R.string.enter_note_text);
        input.setMinLines(3);
        input.setMaxLines(10);
        builder.setView(input);
        
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String noteText = input.getText().toString().trim();
            if (!noteText.isEmpty()) {
                String selectedCategory = categories.get(categorySpinner.getSelectedItemPosition());
                String noteId = "text_" + System.currentTimeMillis();
                
                Note textNote = new Note(
                    noteId,
                    Note.Type.TEXT,
                    selectedCategory,
                    LocalDateTime.now(),
                    noteText
                );
                
                notes.add(0, textNote);
                refreshNoteLists();
                saveNotes();
                
                Toast.makeText(this, R.string.text_note_saved, Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
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
                String noteId = "audio_" + System.currentTimeMillis();
                Note audioNote = new Note(
                    noteId,
                    Note.Type.AUDIO,
                    category,
                    LocalDateTime.now(),
                    "", // Empty transcription initially
                    currentRecordingPath.get()
                );
                
                notes.add(0, audioNote);
                refreshNoteLists();
                
                Toast.makeText(this, R.string.recording_saved, Toast.LENGTH_SHORT).show();
                saveNotes(); // Save to persistent storage
                
                // Start transcription in background
                transcribeAudio(audioNote);
                
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
    
    private void transcribeAudio(Note audioNote) {
        speechExecutor.execute(() -> {
            try {
                // Read audio file
                byte[] audioData = Files.readAllBytes(Paths.get(audioNote.getFilePath()));
                String audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP);
                
                // Get access token
                try (InputStream credentialsStream = getAssets().open("credentials.json")) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                            .createScoped("https://www.googleapis.com/auth/cloud-platform");
                    credentials.refresh();
                    String accessToken = credentials.getAccessToken().getTokenValue();
                    
                    // Create JSON request body
                    String requestJson = "{"
                            + "\"config\": {"
                            + "\"encoding\": \"AMR\","
                            + "\"sampleRateHertz\": 8000,"
                            + "\"languageCode\": \"de-DE\""
                            + "},"
                            + "\"audio\": {"
                            + "\"content\": \"" + audioBase64 + "\""
                            + "}"
                            + "}";
                    
                    // Make HTTP request to Speech-to-Text REST API
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(requestJson, MediaType.parse("application/json"));
                    Request request = new Request.Builder()
                            .url("https://speech.googleapis.com/v1/speech:recognize")
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();
                    
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseJson = response.body().string();
                            String transcription = parseTranscriptionResponse(responseJson);
                            
                            // Update note with transcription on UI thread
                            runOnUiThread(() -> {
                                updateNoteTranscription(audioNote, transcription);
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Transcription failed: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Transcription failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private String parseTranscriptionResponse(String responseJson) {
        try {
            JsonObject response = gson.fromJson(responseJson, JsonObject.class);
            StringBuilder transcript = new StringBuilder();
            
            if (response.has("results")) {
                for (JsonElement result : response.getAsJsonArray("results")) {
                    JsonObject resultObj = result.getAsJsonObject();
                    if (resultObj.has("alternatives")) {
                        for (JsonElement alternative : resultObj.getAsJsonArray("alternatives")) {
                            JsonObject altObj = alternative.getAsJsonObject();
                            if (altObj.has("transcript")) {
                                transcript.append(altObj.get("transcript").getAsString());
                            }
                        }
                    }
                }
            }
            
            String transcription = transcript.toString().trim();
            return transcription.isEmpty() ? "[No speech detected]" : transcription;
            
        } catch (Exception e) {
            return "[Transcription parsing failed]";
        }
    }
    
    private void transcribeExistingNotes() {
        // Find all non-completed audio notes without transcription
        List<Note> notesToTranscribe = notes.stream()
            .filter(note -> note.getType() == Note.Type.AUDIO)
            .filter(note -> !note.isDone())
            .filter(note -> note.getText() == null || note.getText().trim().isEmpty())
            .collect(Collectors.toList());
        
        if (notesToTranscribe.isEmpty()) {
            Toast.makeText(this, R.string.no_notes_to_transcribe, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress message
        String progressMessage = getString(R.string.transcribing_notes, notesToTranscribe.size());
        Toast.makeText(this, progressMessage, Toast.LENGTH_LONG).show();
        
        // Start batch transcription
        batchTranscribeNotes(notesToTranscribe);
    }
    
    private void batchTranscribeNotes(List<Note> notesToTranscribe) {
        speechExecutor.execute(() -> {
            int processedCount = 0;
            
            for (Note note : notesToTranscribe) {
                try {
                    // Add delay between requests to avoid rate limiting
                    if (processedCount > 0) {
                        Thread.sleep(1000); // 1 second delay
                    }
                    
                    transcribeNoteSync(note);
                    processedCount++;
                    
                    // Update UI on main thread
                    final int currentCount = processedCount;
                    runOnUiThread(() -> {
                        refreshNoteLists();
                        saveNotes();
                    });
                    
                } catch (Exception e) {
                    // Continue with next note if one fails
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to transcribe note: " + note.getCategory(), 
                                     Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            // Show completion message
            final int finalCount = processedCount;
            runOnUiThread(() -> {
                String completeMessage = getString(R.string.transcription_complete, finalCount);
                Toast.makeText(this, completeMessage, Toast.LENGTH_LONG).show();
            });
        });
    }
    
    private void transcribeNoteSync(Note audioNote) throws Exception {
        // Read audio file
        byte[] audioData = Files.readAllBytes(Paths.get(audioNote.getFilePath()));
        String audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP);
        
        // Get access token
        try (InputStream credentialsStream = getAssets().open("credentials.json")) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refresh();
            String accessToken = credentials.getAccessToken().getTokenValue();
            
            // Create JSON request body
            String requestJson = "{"
                    + "\"config\": {"
                    + "\"encoding\": \"AMR\","
                    + "\"sampleRateHertz\": 8000,"
                    + "\"languageCode\": \"de-DE\""
                    + "},"
                    + "\"audio\": {"
                    + "\"content\": \"" + audioBase64 + "\""
                    + "}"
                    + "}";
            
            // Make HTTP request to Speech-to-Text REST API
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(requestJson, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://speech.googleapis.com/v1/speech:recognize")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseJson = response.body().string();
                    String transcription = parseTranscriptionResponse(responseJson);
                    
                    // Update note with transcription synchronously
                    runOnUiThread(() -> {
                        updateNoteTranscription(audioNote, transcription);
                    });
                } else {
                    throw new Exception("HTTP " + response.code());
                }
            }
        }
    }
    
    private void updateNoteTranscription(Note audioNote, String transcription) {
        // Find the note in our list and update it
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.getId().equals(audioNote.getId())) {
                // Create new note with transcription
                Note updatedNote = new Note(
                    note.getId(),
                    note.getType(),
                    note.getCategory(),
                    note.getTimestamp(),
                    transcription,
                    note.getFilePath()
                );
                updatedNote.setDone(note.isDone());
                
                notes.set(i, updatedNote);
                refreshNoteLists();
                saveNotes();
                
                Toast.makeText(this, "Transcription completed", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }
    
    private void playNote(Note note) {
        if (note.getType() != Note.Type.AUDIO) {
            return; // Only audio notes can be played
        }
        
        // If this note is currently playing, pause it
        if (currentlyPlaying.isPresent() && currentlyPlaying.get().equals(note)) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                activeAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Paused: " + note.getCategory(), Toast.LENGTH_SHORT).show();
                return;
            } else if (mediaPlayer != null) {
                // Resume playback
                mediaPlayer.start();
                activeAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Resumed: " + note.getCategory(), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Stop any currently playing audio
        stopCurrentPlayback();
        
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(note.getFilePath());
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
            
            currentlyPlaying = Optional.of(note);
            activeAdapter.notifyDataSetChanged();
            completedAdapter.notifyDataSetChanged();
            
            Toast.makeText(this, "Playing: " + note.getCategory(), Toast.LENGTH_SHORT).show();
            
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
    
    public boolean isCurrentlyPlaying(Note note) {
        return currentlyPlaying.map(playing -> playing.equals(note)).orElse(false);
    }
    
    public boolean isCurrentlyPaused(Note note) {
        return currentlyPlaying.map(playing -> playing.equals(note) && mediaPlayer != null && !mediaPlayer.isPlaying()).orElse(false);
    }
    
    public NoteAdapter getAdapter() {
        // Return the adapter that contains the currently playing note
        if (currentlyPlaying.isPresent()) {
            Note playing = currentlyPlaying.get();
            if (activeNotes.contains(playing)) {
                return activeAdapter;
            } else if (completedNotes.contains(playing)) {
                return completedAdapter;
            }
        }
        return activeAdapter; // Default to active adapter
    }
    
    private void toggleDoneStatus(Note note) {
        note.setDone(!note.isDone());
        refreshNoteLists();
        saveNotes(); // Save updated status
    }
    
    private void deleteNote(Note note) {
        notes.remove(note);
        refreshNoteLists();
        
        // Delete the actual file if it's an audio note
        if (note.getType() == Note.Type.AUDIO && note.getFilePath() != null) {
            new File(note.getFilePath()).delete();
        }
        saveNotes(); // Save updated list
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
        if (speechExecutor != null) {
            speechExecutor.shutdown();
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
        } else if (item.getItemId() == R.id.action_transcribe_existing) {
            transcribeExistingNotes();
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
    
    private void saveNotes() {
        try {
            File file = new File(getFilesDir(), NOTES_FILE);
            // Filter out audio notes where the audio file no longer exists
            List<Note> validNotes = notes.stream()
                .filter(note -> {
                    if (note.getType() == Note.Type.AUDIO) {
                        return new File(note.getFilePath()).exists();
                    }
                    return true; // Text notes are always valid
                })
                .collect(Collectors.toList());
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(validNotes, writer);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save notes", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadNotes() {
        try {
            File file = new File(getFilesDir(), NOTES_FILE);
            if (!file.exists()) {
                return; // No saved notes yet
            }
            
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<Note>>(){}.getType();
                List<Note> loadedNotes = gson.fromJson(reader, listType);
                
                if (loadedNotes != null) {
                    // Filter out audio notes where the audio file no longer exists
                    List<Note> validNotes = loadedNotes.stream()
                        .filter(note -> {
                            if (note.getType() == Note.Type.AUDIO) {
                                return new File(note.getFilePath()).exists();
                            }
                            return true; // Text notes are always valid
                        })
                        .collect(Collectors.toList());
                    
                    notes.clear();
                    notes.addAll(validNotes);
                    refreshNoteLists();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load notes", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void refreshNoteLists() {
        activeNotes.clear();
        completedNotes.clear();
        
        // Sort notes by timestamp (newest first) for chronological order
        notes.sort((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()));
        
        for (Note note : notes) {
            if (note.isDone()) {
                completedNotes.add(note);
            } else {
                activeNotes.add(note);
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
        completedNotesRecyclerView.setVisibility(visibility);
    }
    
    private void updateCompletedSectionHeader() {
        int completedCount = completedNotes.size();
        
        if (completedCount == 0) {
            completedSectionHeader.setVisibility(View.GONE);
            completedNotesRecyclerView.setVisibility(View.GONE);
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
    
    // Custom deserializer for Note to handle backward compatibility
    private static class NoteDeserializer implements JsonDeserializer<Note> {
        @Override
        public Note deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            
            String id = jsonObject.get("id").getAsString();
            Note.Type noteType = Note.Type.valueOf(jsonObject.get("type").getAsString());
            String category = jsonObject.get("category").getAsString();
            LocalDateTime timestamp = context.deserialize(jsonObject.get("timestamp"), LocalDateTime.class);
            String content = jsonObject.get("content").getAsString();
            boolean done = jsonObject.has("done") ? jsonObject.get("done").getAsBoolean() : false;
            
            Note note;
            if (noteType == Note.Type.TEXT) {
                note = new Note(id, noteType, category, timestamp, content);
            } else {
                // For audio notes, check if filePath exists (new format) or use content as filePath (old format)
                String filePath;
                String transcription;
                if (jsonObject.has("filePath")) {
                    // New format: content is transcription, filePath is separate
                    filePath = jsonObject.get("filePath").getAsString();
                    transcription = content;
                } else {
                    // Old format: content was the file path
                    filePath = content;
                    transcription = "";
                }
                note = new Note(id, noteType, category, timestamp, transcription, filePath);
            }
            
            note.setDone(done);
            return note;
        }
    }
}