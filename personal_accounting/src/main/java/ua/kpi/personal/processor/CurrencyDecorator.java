package ua.kpi.personal.processor;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.service.ExchangeRateService; // ? Імпорт нового сервісу
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyDecorator extends TransactionDecorator {

    private static final String BASE_CURRENCY = "UAH";
    
    // ? Нове поле для сервісу курсів
    private final ExchangeRateService exchangeRateService; 
    
    // ? Кеш для курсів, щоб не робити запит до API при кожній транзакції
    private final Map<String, Double> ratesCache = new ConcurrentHashMap<>();

    public CurrencyDecorator(TransactionProcessor wrappedProcessor, ExchangeRateService exchangeRateService) {
        super(wrappedProcessor);
        this.exchangeRateService = exchangeRateService;
        // ?? Ініціалізуємо кеш при створенні декоратора
        loadRates();
    }

    /**
     * Завантажує актуальні курси з API в кеш.
     * Запуск виконується синхронно, оскільки декоратор потрібен для коректного створення транзакції.
     */
    private void loadRates() {
        try {
            Map<String, Double> rates = exchangeRateService.getRates();
            if (rates != null && !rates.isEmpty()) {
                ratesCache.putAll(rates);
                System.out.println("? CurrencyDecorator: Кеш курсів оновлено. Отримано: " + rates.keySet());
            } else {
                // Використовуємо заглушки або логуємо помилку, якщо API недоступне
                ratesCache.put("USD", 42.0); // Заглушка, якщо не вдалося завантажити
                ratesCache.put("EUR", 51.5); // Заглушка
                System.err.println("?? CurrencyDecorator: Не вдалося завантажити актуальні курси. Використовуються приблизні данні.");
            }
        } catch (Exception e) {
             // Використовуємо заглушки у разі винятку
            ratesCache.put("USD", 42.0);
            ratesCache.put("EUR", 51.5);
            System.err.println("?? CurrencyDecorator: Помилка під час завантаження курсів. Використовуються приблизні данні: " + e.getMessage());
        }
    }


    @Override
    public Transaction create(Transaction tx) {
        
        String inputCurrency = tx.getCurrency(); 
        double originalAmount = tx.getAmount();
        double exchangeRate = 1.0;

        if (inputCurrency != null && !BASE_CURRENCY.equals(inputCurrency)) {
            
            // ? Отримуємо курс із кешу (заповненого з API)
            exchangeRate = ratesCache.getOrDefault(inputCurrency, 1.0);
            
            if (exchangeRate <= 1.0) {
                // Це означає, що валюта не підтримується або курс не завантажено
                System.err.println("?? CurrencyDecorator: Курс для " + inputCurrency + " недоступний. Конвертація не виконана.");
                return super.create(tx);
            }
            
            double convertedAmount = originalAmount * exchangeRate;
            
            tx.setAmount(convertedAmount);
            tx.setDescription(tx.getDescription().trim() + 
                              " (конвертовано з " + inputCurrency + ": " + 
                              String.format("%.2f", originalAmount) + " @ " + String.format("%.2f", exchangeRate) + ")");
            
            System.out.println("? CurrencyDecorator: Конвертовано " + originalAmount + " " + inputCurrency + 
                               " на " + convertedAmount + " " + BASE_CURRENCY);
        }
        
        return super.create(tx); 
    }

    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        // Конвертація відбувається лише для updatedTx, оскільки originalTx вже було збережено в конвертованому вигляді.
        
        String inputCurrency = updatedTx.getCurrency(); 
        double originalAmount = updatedTx.getAmount();
        double exchangeRate = 1.0;

        if (inputCurrency != null && !BASE_CURRENCY.equals(inputCurrency)) {
            // Отримуємо курс із кешу
            exchangeRate = ratesCache.getOrDefault(inputCurrency, 1.0);
            
            if (exchangeRate > 1.0) {
                double convertedAmount = originalAmount * exchangeRate;
                updatedTx.setAmount(convertedAmount);
                updatedTx.setDescription(updatedTx.getDescription().trim() + 
                                         " (конвертовано з " + inputCurrency + ": " + 
                                         String.format("%.2f", originalAmount) + " @ " + String.format("%.2f", exchangeRate) + ")");
                
                System.out.println("? CurrencyDecorator: Конвертовано " + originalAmount + " " + inputCurrency + 
                                   " на " + convertedAmount + " " + BASE_CURRENCY + " для оновлення.");
            }
        }
        
        return super.update(originalTx, updatedTx); 
    }
}
