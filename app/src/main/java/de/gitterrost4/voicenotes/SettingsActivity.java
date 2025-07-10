package de.gitterrost4.voicenotes;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements CategoriesAdapter.OnStartDragListener {
    
    private static final String PREFS_NAME = "voice_notes_prefs";
    private static final String CATEGORIES_KEY = "categories";
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "ToDos", "Reminders", "Town Meeting"
    );
    
    private List<String> categories;
    private CategoriesAdapter adapter;
    private SharedPreferences prefs;
    private Gson gson;
    private ItemTouchHelper itemTouchHelper;
    
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
        Button addCategoryButton = findViewById(R.id.addCategoryButton);
        RecyclerView categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        
        adapter = new CategoriesAdapter(categories, this::deleteCategory, this);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(adapter);
        
        // Set up drag and drop
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(adapter, this::saveCategories);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(categoriesRecyclerView);
        
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
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
    
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}