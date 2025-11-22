package ua.kpi.personal.repo;

import ua.kpi.personal.model.Category;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;

public class CategoryCache {

    private static final Map<Long, Category> CACHE = new ConcurrentHashMap<>();
    
    private CategoryCache() {}

    public static Category getById(Long id) {
        if (id == null) return null;
        return CACHE.get(id);
    }

    public static Category put(Category category) {
        if (category == null || category.getId() == null) {
            return category;
        }
        CACHE.put(category.getId(), category);
        return category;
    }

    public static void updateCache(List<Category> categories) {
        categories.forEach(CategoryCache::put);
    }
    
    public static void clearCache() {
        CACHE.clear();
    }
    
    public static void remove(Long id) {
        if (id != null) {
            CACHE.remove(id);
        }
    }
}