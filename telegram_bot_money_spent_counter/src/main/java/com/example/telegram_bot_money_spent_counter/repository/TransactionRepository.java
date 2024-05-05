package com.example.telegram_bot_money_spent_counter.repository;

import com.example.telegram_bot_money_spent_counter.model.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    List<Transaction> findByCashAccountCashAccountId(Long cashAccountId);
}
