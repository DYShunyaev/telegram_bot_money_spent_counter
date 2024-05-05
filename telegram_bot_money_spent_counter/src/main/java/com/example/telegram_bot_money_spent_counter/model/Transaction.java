package com.example.telegram_bot_money_spent_counter.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long transactionId;

    private String name;

    private Categories category;

    private Date dateTransaction;

    private Double Price;

    @ManyToOne
    @ToString.Exclude
    private CashAccount cashAccount;

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", dateTransaction=" + dateTransaction +
                ", Price=" + Price +
                '}';
    }
}
