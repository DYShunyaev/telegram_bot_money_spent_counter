package com.example.telegram_bot_money_spent_counter.service.currency_paesers;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.example.telegram_bot_money_spent_counter.model.Currency;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.SneakyThrows;

import java.net.MalformedURLException;
import java.net.URL;

import static com.codeborne.selenide.Selenide.$$x;

@org.springframework.context.annotation.Configuration
public class SelenideConfig extends Thread {
    private final ElementsCollection tableCurrency = $$x("//*[contains(@class,\"data\")]//td");
    private final Currency[] currencies = Currency.values();

    public Currency[] getCurrency() {
        return currencies;
    }

    private void setUp() throws MalformedURLException {
        Configuration.headless = true;
        WebDriverManager.chromiumdriver().config()
                .setChromeDriverUrl(
                        new URL("https://bonigarcia.dev/selenium-webdriver-java/"));
        WebDriverManager.chromiumdriver().clearDriverCache().setup();
        String BASE_URL = "https://www.cbr.ru/currency_base/daily/";
        Selenide.open(BASE_URL);
    }

    @SneakyThrows
    private void checkActualCurrencyExchange() {
        for (Currency currency : currencies) {
            if (currency.getAbbreviated().equals("RUB")) {
                currency.setValue(1.0);
                continue;
            }
            for (int i = 1; i < tableCurrency.size(); i += 5) {
                if (currency.getAbbreviated().equals(tableCurrency.get(i).getText())) {
                    currency.setValue(
                            Double.parseDouble((tableCurrency.get(i + 3).getText()
                                    .replaceAll(",", "\\.")))
                                    / Integer.parseInt((tableCurrency.get(i + 1).getText())));
                    break;
                }
            }
        }
        long time = 24 * 60 * 60000;
        Thread.sleep(time);
        checkActualCurrencyExchange();
    }

    @Override
    public void run() {
        try {
            setUp();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        checkActualCurrencyExchange();
    }
}
