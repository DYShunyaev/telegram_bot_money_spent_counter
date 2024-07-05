package com.example.telegram_bot_money_spent_counter.service;

import com.example.telegram_bot_money_spent_counter.model.CashAccount;
import com.example.telegram_bot_money_spent_counter.model.Currency;
import com.example.telegram_bot_money_spent_counter.model.User;
import com.example.telegram_bot_money_spent_counter.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final CashAccountService cashAccountService;

    @Autowired
    public UserService(UserRepository userRepository, CashAccountService cashAccountService) {
        this.userRepository = userRepository;
        this.cashAccountService = cashAccountService;
    }

    public void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("User save: " + user);
        }
    }

    void updateUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    String deleteUserDate(long chatId, String name) {
        User user = userRepository.findById(chatId).orElseThrow();
        if (!user.getCashAccounts().isEmpty()) user.getCashAccounts().forEach(cashAccountService::deleteCashAccount);
        userRepository.deleteById(chatId);
        String answer = EmojiParser.parseToUnicode(name + ", everything is deleted." + " :smirk:");
        log.info("Delete data from " + name);
        return answer;
    }

    @Transactional
    User findUserByChatId(long chatId) {
        return userRepository.findById(chatId).orElseThrow();
    }

    @Transactional
    List<CashAccount> getCashAccountsByUserChatId(Long chatID) {
        return cashAccountService.getCashAccountsBuUserChatId(chatID);
    }

    String selectDataUser(long chatId) {
        return userRepository.findById(chatId).toString();
    }

    @Transactional
    String createNewCashAccount(long userID, String name, Currency currency) {
        User user = userRepository.findById(userID).orElseThrow();
        CashAccount cashAccount = new CashAccount(null, currency, new ArrayList<>());
        cashAccount.setUser(user);
        user.getCashAccounts().add(cashAccountService.createNewCashAccount(cashAccount));
        updateUser(user);
        return EmojiParser.parseToUnicode(name + " ваш Счет с id " + cashAccount.getCashAccountId() + " создан" + " :smirk:");
    }
}