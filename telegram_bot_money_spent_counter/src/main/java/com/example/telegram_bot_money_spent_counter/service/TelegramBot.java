package com.example.telegram_bot_money_spent_counter.service;

import com.example.telegram_bot_money_spent_counter.config.BotConfig;
import com.example.telegram_bot_money_spent_counter.model.CashAccount;
import com.example.telegram_bot_money_spent_counter.model.Categories;
import com.example.telegram_bot_money_spent_counter.model.Currency;
import com.example.telegram_bot_money_spent_counter.model.Transaction;
import com.example.telegram_bot_money_spent_counter.service.currency_paesers.SelenideConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    SelenideConfig selenideConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private CashAccountService cashAccountService;

    @Autowired
    private TransactionService transactionService;

    final BotConfig config;

    Transaction transaction = new Transaction();

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> myCommands = new ArrayList<>();
        myCommands.add(new BotCommand("/start", "Start this bot!"));
        myCommands.add(new BotCommand("/mydata", "get your data stored."));
        myCommands.add(new BotCommand("/deletedata", "delete my data"));
        myCommands.add(new BotCommand("/help", "info how to use this bot"));
        myCommands.add(new BotCommand("/settings", "set your preferences"));
        myCommands.add(new BotCommand("/getcurrency", "set your currency"));
        try {
            this.execute(new SetMyCommands(myCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.matches("\\D*\\s\\d*\\s\\d{4}-\\d{2}-\\d{2}")
                    || messageText.matches("\\D*\\s\\D*\\s\\d*\\s\\d{4}-\\d{2}-\\d{2}")) {
                String[] values = messageText.split("\\s");
                transaction.setName(values[0]);
                transaction.setPrice(Double.parseDouble(values[1]));
                transaction.setDateTransaction(Date.valueOf(values[2]));
                long cashAccountId = transaction.getCashAccount().getCashAccountId();
                createNewTransaction();
                getTransactionsByCashAccountId(chatId, cashAccountId);
            }
            switch (messageText) {
                case "/start" -> {
                    userService.registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/mydata" -> sendMessage(chatId, userService.selectDataUser(chatId));
                case "/deletedata" ->
                        sendMessage(chatId, userService.deleteUserDate(chatId, update.getMessage().getChat().getFirstName()));
                case "/getcurrency" -> sendMessage(chatId, getCurrency());
                case "/newcashaccount" -> chooseYourCurrency(chatId);
                case "/getmycashaccounts" -> sendUserCashAccounts(chatId);
                case "/help" -> {
                    try {
                        sendHelpMessage(chatId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                default -> sendMessage(chatId, "Sorry, command was not recognized.");
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String name = userService.findUserByChatId(chatId).getFirstName();
            if (callBackData.contains("CashAccountId-")) {
                getCashAccountsById(chatId, Long.parseLong(callBackData.replaceAll("CashAccountId-", "")));
            } else if (callBackData.contains("gettransactionsbycashaccountid")) {
                getTransactionsByCashAccountId(chatId, Long.parseLong(callBackData.replaceAll("gettransactionsbycashaccountid ", "")));
            } else if (callBackData.contains("newtransaction")) {
                chooseYourCategory(chatId, Long.parseLong(callBackData.replaceAll("newtransaction ", "")));
            } else if (callBackData.contains("transactionsExpenses")) {
                getTransactionsExpenses(chatId, Long.parseLong(callBackData.replaceAll("transactionsExpenses ", "")));
            } else if (callBackData.matches("[A-Z]*\\s\\d*")) {
                Categories categories = Categories.valueOf(callBackData.replaceAll("\\s\\d*", ""));
                transaction.setCashAccount(cashAccountService.getEntityCashAccountByCashAccountId(Long.parseLong(callBackData.replaceAll("[A-Z]*\s", ""))));
                transaction.setCategory(categories);
                addParametersToTransaction(chatId);
            } else if (callBackData.contains("TransactionId-")) {
                long id = Long.parseLong(callBackData.replaceAll("TransactionId-", ""));
                getTransactionInfo(chatId, id);
            } else if (callBackData.contains("deleteTransaction")) {
                long id = Long.parseLong(callBackData.replaceAll("deleteTransaction-", ""));
                transaction = transactionService.getTransactionById(id);
                transactionService.deleteTransactionById(id);
                getTransactionsByCashAccountId(chatId, transaction.getCashAccount().getCashAccountId());
            }
            switch (callBackData) {
                case "RUB" -> {
                    sendMessage(chatId, userService.createNewCashAccount(chatId,
                            name, Currency.RUB));
                    sendUserCashAccounts(chatId);
                }
                case "USD" -> {
                    sendMessage(chatId, userService.createNewCashAccount(chatId,
                            name, Currency.USD));
                    sendUserCashAccounts(chatId);
                }
                case "newcashaccount" -> chooseYourCurrency(chatId);
                case "getmycashaccounts" -> sendUserCashAccounts(chatId);
            }
        }
    }

    private void getTransactionInfo(Long chatId, Long transactionId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        Transaction transaction1 = transactionService.getTransactionById(transactionId);
        message.setText(transaction1.toString());
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        var buttonAccount = new InlineKeyboardButton();
        buttonAccount.setText("Назад");
        buttonAccount.setCallbackData("gettransactionsbycashaccountid "
                + transactionService.getCashAccountIdByTransactionId(transactionId));
        buttonList.add(buttonAccount);
        var keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("Удалить");
        keyboardButton.setCallbackData("deleteTransaction-" + transactionId);
        buttonList.add(keyboardButton);
        rowsInline.add(buttonList);
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private String getCurrency() {
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < selenideConfig.getCurrency().length; i++) {
            if (i != selenideConfig.getCurrency().length - 1) {
                answer.append(selenideConfig.getCurrency()[i]).append(",\n");
            } else answer.append(selenideConfig.getCurrency()[i]);
        }
        return answer.toString();
    }

    private void createNewTransaction() {
        transactionService.createNewTransaction(transaction);
        transaction = new Transaction();
    }

    private void addParametersToTransaction(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Введите через пробел: Наименование, сумму, дату \nНапример: Кино 600р 2024-05-03");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void chooseYourCategory(Long chatId, Long cashAccountId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберете категорию:");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (Categories categories : Categories.values()) {
            List<InlineKeyboardButton> buttonList = new ArrayList<>();
            var buttonAccount = new InlineKeyboardButton();
            buttonAccount.setText(categories.getName());
            buttonAccount.setCallbackData(categories + " " + cashAccountId);
            buttonList.add(buttonAccount);
            rowsInline.add(buttonList);
        }
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void getTransactionsByCashAccountId(Long chatId, Long cashAccountId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Транзации по счету " + cashAccountId + ":");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (Transaction transaction : transactionService.getTransactionsByCashAccountId(cashAccountId)) {
            List<InlineKeyboardButton> buttonList = new ArrayList<>();
            var buttonAccount = new InlineKeyboardButton();
            buttonAccount.setText(transaction.getName() + "-" + transaction.getPrice() + " руб.");
            buttonAccount.setCallbackData("TransactionId-" + transaction.getTransactionId());
            buttonList.add(buttonAccount);
            rowsInline.add(buttonList);
        }
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        var buttonNewTransaction = new InlineKeyboardButton();
        buttonNewTransaction.setText("Создать новую транзакцию");
        buttonNewTransaction.setCallbackData("newtransaction " + cashAccountId);
        var buttonGetTransactionsExpenses = new InlineKeyboardButton();
        buttonGetTransactionsExpenses.setText("Получить выписку по транзакциям");
        buttonGetTransactionsExpenses.setCallbackData("transactionsExpenses " + cashAccountId);
        buttonList.add(buttonNewTransaction);
        buttonList.add(buttonGetTransactionsExpenses);
        rowsInline.add(buttonList);
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void getTransactionsExpenses(Long chatId, Long cashAccountId) {
        SendPhoto message = new SendPhoto();
        message.setChatId(chatId);
        message.setPhoto(transactionService.createExpenseChart(cashAccountId));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void getCashAccountsById(Long chatId, Long cashAccountId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(cashAccountService.getCashAccountByCashAccountId(cashAccountId));
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        var buttonGetMyAccounts = new InlineKeyboardButton();
        buttonGetMyAccounts.setText("Транзакции по счету");
        buttonGetMyAccounts.setCallbackData("gettransactionsbycashaccountid " + cashAccountId);
        buttonList.add(buttonGetMyAccounts);
        rowsInline.add(buttonList);
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }

    }

    private void startCommandReceived(long chatId, String name) {
        log.info("Replied to user " + name);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :relaxed:"));
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        var buttonGetMyAccounts = new InlineKeyboardButton();
        buttonGetMyAccounts.setText("Посмотртеть мои счета");
        buttonGetMyAccounts.setCallbackData("getmycashaccounts");
        buttonList.add(buttonGetMyAccounts);
        rowsInline.add(buttonList);
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void chooseYourCurrency(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберете валюту, в которой будет вестись счет:");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        int count = 0;
        Currency[] currencies = selenideConfig.getCurrency();
        while (count < currencies.length) {
            List<InlineKeyboardButton> buttonList = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                try {
                    var buttonAccount = new InlineKeyboardButton();
                    buttonAccount.setText(currencies[count].getCurrencyName());
                    buttonAccount.setCallbackData(currencies[count].getAbbreviated());
                    buttonList.add(buttonAccount);
                    count++;
                } catch (ArrayIndexOutOfBoundsException exception) {
                    break;
                }
            }
            rowsInline.add(buttonList);
        }
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendHelpMessage(Long chatId) throws IOException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(readFile(Paths.get
                ("D:/Java/telegram_bot_money_spent_counter/telegram_bot_money_spent_counter/help.txt"))
                .toString());
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendUserCashAccounts(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберете Ваш счет:");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (CashAccount cashAccount : userService.getCashAccountsByUserChatId(chatId)) {
            List<InlineKeyboardButton> buttonList = new ArrayList<>();
            var buttonAccount = new InlineKeyboardButton();
            buttonAccount.setText(String.valueOf(cashAccount.getCashAccountId()));
            buttonAccount.setCallbackData("CashAccountId-" + cashAccount.getCashAccountId());
            buttonList.add(buttonAccount);
            rowsInline.add(buttonList);
        }
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        var buttonNewAccount = new InlineKeyboardButton();
        buttonNewAccount.setText("Создать новый счет");
        buttonNewAccount.setCallbackData("newcashaccount");
        buttonList.add(buttonNewAccount);
        rowsInline.add(buttonList);
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }

    }

    private StringBuilder readFile(Path pathToFile) throws IOException {
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(pathToFile);
        while (scanner.hasNext()) {
            String row = scanner.nextLine();
            builder.append(row).append("\n");
        }
        return builder;
    }
}