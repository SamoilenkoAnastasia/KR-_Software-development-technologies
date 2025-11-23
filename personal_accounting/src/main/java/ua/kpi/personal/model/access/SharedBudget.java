package ua.kpi.personal.model.access;

// Зверніть увагу: ми не імпортуємо User, оскільки зберігаємо лише ownerId (Long).

public class SharedBudget {

    private Long id;
    private String name;
    private Long ownerId; // ID користувача-власника (хто може керувати доступом)

    public SharedBudget() {
    }

    // --- Геттери та Сеттери ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        // Зручне відображення для списків/вибору
        return name;
    }
}