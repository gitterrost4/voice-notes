package de.gitterrost4.voicenotes;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
    
    private final CategoriesAdapter adapter;
    private final Runnable onOrderChanged;
    
    public ItemTouchHelperCallback(CategoriesAdapter adapter, Runnable onOrderChanged) {
        this.adapter = adapter;
        this.onOrderChanged = onOrderChanged;
    }
    
    @Override
    public boolean isLongPressDragEnabled() {
        return false; // We handle drag start manually via the drag handle
    }
    
    @Override
    public boolean isItemViewSwipeEnabled() {
        return false; // We don't want swipe to delete
    }
    
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }
    
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        adapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }
    
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Not used since swipe is disabled
    }
    
    @Override
    public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
        // Save the new order when an item is moved
        onOrderChanged.run();
    }
}