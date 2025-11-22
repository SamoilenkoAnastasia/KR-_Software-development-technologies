package ua.kpi.personal.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Category {
    
    private final Long id;
    private final Long userId; 
    private final String name;
    private final String type;
    private final Long parentId; 
    private final LocalDateTime createdAt;
    
    
    // --- ОСНОВНИЙ КОНСТРУКТОР ---
    public Category(Long id, Long userId, String name, String type, Long parentId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.parentId = parentId; 
        this.createdAt = createdAt;
    }

    // --- КОНСТРУКТОР ДЛЯ СТВОРЕННЯ НОВОЇ КАТЕГОРІЇ ---
    public Category(Long userId, String name, String type, Long parentId) {
        this(null, userId, name, type, parentId, LocalDateTime.now());
    }

    // =========================================================================
    // ? ВИПРАВЛЕННЯ: КОНСТРУКТОР ДЛЯ DAO/МАПЕРА
    // Дозволяє створити "легкий" об'єкт категорії при завантаженні шаблонів через JOIN
    // Цей об'єкт буде використовуватися в mapFullResultSetToTemplate
    // =========================================================================
    /**
     * Конструктор для створення об'єкта Category з мінімальними даними,
     * отриманими з JOIN-запиту (наприклад, у TemplateDao).
     */
    public Category(Long id, String name, String type) {
        this.id = id;
        this.userId = null; 
        this.name = name;
        this.type = type;
        this.parentId = null; 
        this.createdAt = null; // Або встановіть LocalDateTime.MIN
    }
    
    // --- МЕТОДИ ІМУТАБЕЛЬНОСТІ (Без змін) ---
    
    public Category withId(Long newId) {
        return new Category(newId, this.userId, this.name, this.type, this.parentId, this.createdAt);
    }
    
    
    public Category withUpdate(String newName, String newType, Long newParentId) {
        return new Category(this.id, this.userId, newName, newType, newParentId, this.createdAt);
    }
    
    // --- Геттери (Без змін) ---
    
    public Long getId() {
        return id;
    }
    
    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // --- equals, hashCode, toString (Без змін) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id); 
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}