package com.example.telegram_bot_money_spent_counter.repository;

import com.example.telegram_bot_money_spent_counter.model.CashAccount;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CashAccountRepository extends CrudRepository<CashAccount,Long> {

    List<CashAccount> findByUserChatId(Long chatId);
}
