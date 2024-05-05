package com.example.telegram_bot_money_spent_counter.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private Long chatId;

    private String firstName;

    private String lastName;

    private String userName;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CashAccount> cashAccounts = new ArrayList<>();

    private Timestamp registeredAt;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "chatId = " + chatId + ", " +
                "firstName = " + firstName + ", " +
                "lastName = " + lastName + ", " +
                "userName = " + userName + ", " +
                "registeredAt = " + registeredAt + ")";
    }
}
