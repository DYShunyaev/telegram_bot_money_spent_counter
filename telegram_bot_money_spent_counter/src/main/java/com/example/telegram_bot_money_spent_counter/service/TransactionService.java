package com.example.telegram_bot_money_spent_counter.service;

import com.example.telegram_bot_money_spent_counter.model.Transaction;
import com.example.telegram_bot_money_spent_counter.repository.TransactionRepository;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    void createNewTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElseThrow();
    }

    long getCashAccountIdByTransactionId(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow();
        return transaction.getCashAccount().getCashAccountId();
    }

    List<Transaction> getTransactionsByCashAccountId(Long cashAccountId) {
        return transactionRepository.findByCashAccountCashAccountId(cashAccountId);
    }

    void deleteTransactionById(Long id) {
        transactionRepository.deleteById(id);
    }

    public InputFile createExpenseChart(Long cashAccountId) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<String>();
        List<Transaction> transactions = transactionRepository.findByCashAccountCashAccountId(cashAccountId);
        for (Transaction transaction : transactions) {
            dataset.setValue(transaction.getName(), transaction.getPrice());
        }

        JFreeChart chart = ChartFactory.createPieChart("Диаграмма расходов", dataset, true,
                true, false);
        File chartFile = null;
        try {
            chartFile = new File("expense_chart.png");
            ChartUtils.saveChartAsPNG(chartFile, chart, 400, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new InputFile(chartFile);
    }

    public Double getTotalMoneySpent(Long cashAccountId) {
        Double totalMoneySpent = 0.0;
        List<Transaction> transactions = transactionRepository.findByCashAccountCashAccountId(cashAccountId);
        for (Transaction transaction : transactions) {
            totalMoneySpent += transaction.getPrice();
        }
        return totalMoneySpent;
    }
}
