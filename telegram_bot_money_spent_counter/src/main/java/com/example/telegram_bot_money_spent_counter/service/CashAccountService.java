package com.example.telegram_bot_money_spent_counter.service;

import com.example.telegram_bot_money_spent_counter.model.CashAccount;
import com.example.telegram_bot_money_spent_counter.model.Currency;
import com.example.telegram_bot_money_spent_counter.repository.CashAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CashAccountService {

    private final CashAccountRepository cashAccountRepository;
    private final TransactionService transactionService;



    @Autowired
    public CashAccountService(CashAccountRepository cashAccountRepository, TransactionService transactionService) {
        this.cashAccountRepository = cashAccountRepository;
        this.transactionService = transactionService;
    }
    CashAccount getEntityCashAccountByCashAccountId(long cashAccountId) {
        return cashAccountRepository.findById(cashAccountId).orElseThrow();
    }
    @Transactional
    String getCashAccountByCashAccountId (long cashAccountId){
        CashAccount cashAccount = cashAccountRepository.findById(cashAccountId).orElseThrow();
        StringBuilder builder = new StringBuilder();
        builder.append("ID счета: ").append(cashAccount.getCashAccountId()).append("\n")
                .append("Общая сумма транзакций: ").append(transactionService.getTotalMoneySpent(cashAccountId));
        if (cashAccount.getCurrencyAcc() == null) builder
                        .append("\n").append("Валюта счета: ").append(cashAccount.getCurrencyAcc()).append("\n");
        else  builder
                .append("\n").append("Валюта счета: ").append(cashAccount.getCurrencyAcc().getCurrencyName()).append("\n");
        return builder.toString();
    }
    void setCurrencyToCashAccount(Long id, Currency currency) {
        CashAccount cashAccount = cashAccountRepository.findById(id).orElseThrow();
        cashAccount.setCurrencyAcc(currency);
        cashAccountRepository.save(cashAccount);
    }

    List<CashAccount> getCashAccountsBuUserChatId (Long chatId) {
        return cashAccountRepository.findByUserChatId(chatId);
    }
    CashAccount createNewCashAccount(CashAccount cashAccount) {
        cashAccountRepository.save(cashAccount);
        return cashAccount;
    }
    @Transactional
    void deleteCashAccount(CashAccount cashAccount) {
        if (!cashAccount.getTransactions().isEmpty()) cashAccount.getTransactions()
                .forEach(transaction -> transactionService.deleteTransactionById(transaction.getTransactionId()));
        cashAccountRepository.deleteById(cashAccount.getCashAccountId());
    }
}
