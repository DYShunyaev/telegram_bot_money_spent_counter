package com.example.telegram_bot_money_spent_counter.model;

public enum Currency {
    RUB("Рубль", "RUB", null),
    USD("Доллар", "USD", null),
    EUR("Евро", "EUR", null),
    TRY("Лира", "TRY", null),
    CNH("Юань", "CNY", null);

    Currency(String currencyName, String abbreviated, Double value) {
        this.currencyName = currencyName;
        this.abbreviated = abbreviated;
        this.value = value;
    }

    private String currencyName;
    private String abbreviated;

    private Double value;

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getAbbreviated() {
        return abbreviated;
    }

    @Override
    public String toString() {
        return currencyName + ", " + abbreviated + ", " + value;
    }
}
