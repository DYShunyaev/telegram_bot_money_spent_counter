package com.example.telegram_bot_money_spent_counter.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CashAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long cashAccountId;

    @ManyToOne
    @JoinColumn(name = "user_chat_id")
    @ToString.Exclude
    private User user;

    private Long totalMoneySpent;

    private Currency currencyAcc;

    @OneToMany(mappedBy = "cashAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    public CashAccount(Long totalMoneySpent, Currency currencyAcc, List<Transaction> transactions) {
        this.totalMoneySpent = totalMoneySpent;
        this.currencyAcc = currencyAcc;
        this.transactions = transactions;
    }

    public String toString() {
        if (transactions.isEmpty())
            return "cashAccountId=" + cashAccountId + ", totalMoneySpent=" + totalMoneySpent + ", currencyAcc=" + currencyAcc;
        else
            return "cashAccountId=" + cashAccountId + ", totalMoneySpent=" + totalMoneySpent + ", currencyAcc=" + currencyAcc + ", transactions=" + Arrays.toString(new List[]{transactions});
    }
}
