package de.gitterrost4.voicenotes;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements CategoriesAdapter.OnStartDragListener {
    
    private static final String PREFS_NAME = "voice_notes_prefs";
    private static final String CATEGORIES_KEY = "categories";
    private static final String CREDENTIALS_KEY = "google_cloud_credentials";
    private static final String LANGUAGE_KEY = "transcription_language";
    private static final int DEFAULT_LANGUAGE_INDEX = 0; // German
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "ToDos", "Reminders", "Town Meeting"
    );
    
    private List<String> categories;
    private CategoriesAdapter adapter;
    private SharedPreferences prefs;
    private Gson gson;
    private ItemTouchHelper itemTouchHelper;
    private EditText credentialsEditText;
    private Spinner languageSpinner;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();
        
        loadCategories();
        setupViews();
        loadCredentials();
        loadLanguageSelection();
    }
    
    private void loadCategories() {
        String categoriesJson = prefs.getString(CATEGORIES_KEY, null);
        if (categoriesJson != null) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            categories = new ArrayList<>(gson.fromJson(categoriesJson, listType));
        } else {
            categories = new ArrayList<>(DEFAULT_CATEGORIES);
        }
    }
    
    private void saveCategories() {
        String categoriesJson = gson.toJson(categories);
        prefs.edit().putString(CATEGORIES_KEY, categoriesJson).apply();
    }
    
    private void setupViews() {
        // Categories views
        Button addCategoryButton = findViewById(R.id.addCategoryButton);
        RecyclerView categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        
        // Credentials views
        languageSpinner = findViewById(R.id.languageSpinner);
        credentialsEditText = findViewById(R.id.credentialsEditText);
        Button testCredentialsButton = findViewById(R.id.testCredentialsButton);
        ImageButton transcriptionInfoButton = findViewById(R.id.transcriptionInfoButton);
        
        // Setup language spinner
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(
            this, R.array.transcription_languages, android.R.layout.simple_spinner_item);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        
        adapter = new CategoriesAdapter(categories, this::deleteCategory, this);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(adapter);
        
        // Set up drag and drop
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(adapter, this::saveCategories);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(categoriesRecyclerView);
        
        // Set up click listeners
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
        testCredentialsButton.setOnClickListener(v -> testCredentials());
        transcriptionInfoButton.setOnClickListener(v -> showTranscriptionHelpDialog());
        
        // Auto-save credentials when text changes
        credentialsEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveCredentials();
            }
        });
        
        // Save language selection when changed
        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                saveLanguageSelection(position);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_category);
        
        final EditText input = new EditText(this);
        input.setHint(R.string.enter_category_name);
        builder.setView(input);
        
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                if (!categories.contains(categoryName)) {
                    categories.add(categoryName);
                    adapter.notifyItemInserted(categories.size() - 1);
                    saveCategories();
                } else {
                    Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void deleteCategory(String category) {
        if (categories.size() <= 1) {
            Toast.makeText(this, "Must have at least one category", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Category");
        builder.setMessage("Are you sure you want to delete '" + category + "'?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            int position = categories.indexOf(category);
            categories.remove(position);
            adapter.notifyItemRemoved(position);
            saveCategories();
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void loadCredentials() {
        String credentials = prefs.getString(CREDENTIALS_KEY, "");
        credentialsEditText.setText(credentials);
    }
    
    private void saveCredentials() {
        String credentials = credentialsEditText.getText().toString().trim();
        prefs.edit().putString(CREDENTIALS_KEY, credentials).apply();
        
        if (!credentials.isEmpty()) {
            Toast.makeText(this, R.string.credentials_saved, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testCredentials() {
        String credentials = credentialsEditText.getText().toString().trim();
        
        if (credentials.isEmpty()) {
            Toast.makeText(this, "Please enter credentials first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Validate JSON format
            JsonObject credentialsJson = gson.fromJson(credentials, JsonObject.class);
            
            // Check for required fields
            String[] requiredFields = {"type", "project_id", "private_key", "client_email"};
            for (String field : requiredFields) {
                if (!credentialsJson.has(field)) {
                    Toast.makeText(this, "Missing required field: " + field, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            // Check if it's a service account
            if (!"service_account".equals(credentialsJson.get("type").getAsString())) {
                Toast.makeText(this, "Must be a service account credential", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Save valid credentials
            saveCredentials();
            Toast.makeText(this, R.string.credentials_valid, Toast.LENGTH_LONG).show();
            
        } catch (JsonSyntaxException e) {
            Toast.makeText(this, R.string.credentials_invalid, Toast.LENGTH_LONG).show();
        }
    }
    
    private void showTranscriptionHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.transcription_help_title);
        builder.setMessage(R.string.transcription_help_content);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
    
    private void loadLanguageSelection() {
        int savedLanguageIndex = prefs.getInt(LANGUAGE_KEY, DEFAULT_LANGUAGE_INDEX);
        languageSpinner.setSelection(savedLanguageIndex);
    }
    
    private void saveLanguageSelection(int position) {
        prefs.edit().putInt(LANGUAGE_KEY, position).apply();
    }
    
    public static String getSelectedLanguageCode(SharedPreferences prefs) {
        int languageIndex = prefs.getInt(LANGUAGE_KEY, DEFAULT_LANGUAGE_INDEX);
        String[] languageCodes = {
            "de-DE", "en-US", "en-GB", "fr-FR", "es-ES", "it-IT", 
            "pt-BR", "nl-NL", "pl-PL", "ru-RU", "ja-JP", "ko-KR", 
            "zh-CN", "zh-TW"
        };
        
        if (languageIndex >= 0 && languageIndex < languageCodes.length) {
            return languageCodes[languageIndex];
        }
        return "de-DE"; // Default fallback
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}