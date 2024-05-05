package com.example.telegram_bot_money_spent_counter.repository;

import com.example.telegram_bot_money_spent_counter.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User,Long> {
}
