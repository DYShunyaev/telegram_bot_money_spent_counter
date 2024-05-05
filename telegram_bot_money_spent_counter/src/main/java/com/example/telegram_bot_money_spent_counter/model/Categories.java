package com.example.telegram_bot_money_spent_counter.model;

public enum Categories {
    ENTERTAINMENT("Развлечения"),
    FOOD("Еда"),
    HEALTH("Здоровье"),
    BEAUTY_AND_CARE("Красота и уход"),
    TRANSPORT("Транспорт"),
    CLOTH("Одежда"),
    EDUCATION("Образование"),
    MONETARY_OBLIGATIONS("Денежные обязательства"),
    OTHER("Прочее");

    Categories(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
