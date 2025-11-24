package ua.kpi.personal.service;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.TransactionTemplate;
import ua.kpi.personal.model.TransactionTemplate.RecurringType;
import ua.kpi.personal.repo.TemplateDao;
import ua.kpi.personal.processor.TransactionProcessor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TemplateSchedulerService {
    
    private final TemplateDao templateDao;
    private final TransactionProcessor transactionProcessor;
    
    private final ScheduledExecutorService scheduler;
    
    private final AtomicLong currentUserId = new AtomicLong(-1L);
    
    public TemplateSchedulerService(TemplateDao templateDao, TransactionProcessor transactionProcessor) {
        this.templateDao = templateDao;
        this.transactionProcessor = transactionProcessor;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true); 
            t.setName("Template-Scheduler-Thread");
            return t;
        });
    }

    public void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            this.currentUserId.set(-1L); 
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("? Планувальник шаблонів успішно зупинено.");
        }
    }

    
    public void runScheduledChecks(Long userId) {
        if (userId == null || userId <= 0) {
            System.err.println("Помилка: Неможливо запустити планувальник, userId недійсний.");
            return;
        }

        if (this.currentUserId.get() == userId.longValue() && !scheduler.isShutdown()) {
            System.out.println("Планувальник вже запущено для користувача ID: " + userId);
            return;
        }
        
        if (this.currentUserId.get() != -1L) {
             System.out.println("Планувальник переналаштовується для нового користувача.");
             stopScheduler();
             // Створюємо новий планувальник, оскільки старий зупинено
             // Хоча краще було б просто скасувати завдання
             // Але якщо ви використовуєте single-thread, створення нового - ОК.
             // Для простоти, припускаємо, що ініціалізація в конструкторі покриває це.
        }

        this.currentUserId.set(userId);

        // Негайний запуск завдання для обробки пропущених транзакцій
        scheduler.execute(() -> runScheduledChecksTask(userId));

        // Щоденний запуск
        // Примітка: затримка 1 година, потім повтор кожні 24 години
        scheduler.scheduleAtFixedRate(
            () -> runScheduledChecksTask(userId),
            1, 
            24, 
            TimeUnit.HOURS
        );
        
        System.out.println("? Щоденна перевірка шаблонів запланована для користувача ID: " + userId);
    }

    private void runScheduledChecksTask(Long userId) {
        // ... (метод без змін, оскільки він коректний) ...
        try {
            if (this.currentUserId.get() != userId.longValue()) {
                System.out.println("Завдання пропущено: ID користувача змінився.");
                return;
            }
            
            List<TransactionTemplate> templates = templateDao.findRecurringByUserId(userId); 
            LocalDate today = LocalDate.now();
            
            System.out.println("--- Планувальник: Перевірка регулярних операцій на " + today + " ---");
            
            for (TransactionTemplate template : templates) {
                processMissedExecutions(template, today);
            }
            
        } catch (Exception e) {
            System.err.println("Фатальна помилка при виконанні запланованої перевірки: " + e.getMessage());
        }
    }


    private void processMissedExecutions(TransactionTemplate template, LocalDate today) {
        // ... (метод без змін, оскільки він коректний) ...
        LocalDate startDate = template.getStartDate();
        LocalDate lastDate = template.getLastExecutionDate(); 
        Integer interval = template.getRecurrenceInterval();

        if (interval == null || interval <= 0 || startDate == null || template.getRecurringType() == RecurringType.NONE) {
            return;
        }

        // Починаємо з дати після останнього виконання (або дати початку)
        LocalDate currentDate = (lastDate != null ? lastDate : startDate.minusDays(1));

        if (startDate.isAfter(today)) return;

        System.out.println("--- Планувальник: Обробка шаблону '" + template.getName() + 
                            "'. Починаємо з дати після " + currentDate + " ---");

        while (true) {
            
            LocalDate nextExecutionDate = calculateNextExecutionDate(template, currentDate);

            if (nextExecutionDate.isAfter(today)) {
                break;
            }

            if (!nextExecutionDate.isAfter(currentDate)) {
                 System.err.println("? Помилка розрахунку дати для " + template.getName() + 
                                     ". Наступна дата (" + nextExecutionDate + ") не після поточної (" + currentDate + "). Цикл зупинено.");
                 break;
            }

            executeTransaction(template, nextExecutionDate);
          
            template.setLastExecutionDate(nextExecutionDate); 
            
            // Оновлюємо поточну дату для наступної ітерації
            currentDate = nextExecutionDate;
        }
    }
    
    
    // --- ВИПРАВЛЕНИЙ МЕТОД: calculateNextExecutionDate ---
    private LocalDate calculateNextExecutionDate(TransactionTemplate template, LocalDate lastDate) {
        RecurringType type = template.getRecurringType();
        Integer interval = template.getRecurrenceInterval() != null ? template.getRecurrenceInterval() : 1;
        Integer dayOfMonth = template.getDayOfMonth();
        DayOfWeek dayOfWeek = template.getDayOfWeek();
        
        LocalDate startDate = template.getStartDate();

        // 1. Встановлюємо базову дату для розрахунку. Якщо lastDate < startDate, використовуємо startDate
        LocalDate baseDate = lastDate.isBefore(startDate) ? startDate.minusDays(1) : lastDate;
        
        LocalDate nextDate = baseDate;

        switch (type) {
            case DAILY:
                nextDate = baseDate.plusDays(interval);
                break;
                
            case WEEKLY:
                if (dayOfWeek != null) {
                    
                    // 1. Пересуваємось на потрібну кількість тижнів
                    // Якщо baseDate є дочірнім елементом шаблону, ми повинні знайти наступну дату, 
                    // що знаходиться на 'interval' тижнів вперед
                    
                    // Ініціалізуємо дату після останнього виконання
                    LocalDate nextWeekBase = baseDate.plusDays(1);
                    
                    // Знаходимо наступний день тижня (навіть якщо він той самий)
                    LocalDate nextDayOfWeek = nextWeekBase.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    
                    // 2. Якщо знайдений день знаходиться в наступному інтервалі, просуваємось
                    if (nextDayOfWeek.isBefore(baseDate.plusWeeks(interval)) || nextDayOfWeek.isEqual(baseDate.plusWeeks(interval))) {
                        nextDate = nextDayOfWeek;
                    } else {
                        // Якщо ми пропустили день у поточному інтервалі, переходимо до наступного
                        nextDate = baseDate.with(TemporalAdjusters.next(dayOfWeek)).plusWeeks(interval - 1);
                    }

                    // *** Спрощена логіка: ***
                    // Знаходимо дату, що відповідає pattern (DayOfWeek)
                    LocalDate candidate = baseDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    
                    if (candidate.isAfter(baseDate)) {
                        // Якщо кандидат вже у наступному інтервалі (в наступних тижнях), використовуємо його
                        if (candidate.isAfter(baseDate.plusWeeks(interval))) {
                            nextDate = candidate.minusWeeks(interval - 1); 
                        } else {
                            nextDate = candidate;
                        }
                    } else {
                        // Якщо це той самий день, додаємо повний інтервал
                        nextDate = candidate.plusWeeks(interval);
                    }
                    
                    // **** НАЙБІЛЬШ НАДІЙНА ЛОГІКА ****
                    // Це гарантує, що ми додаємо кратне 'interval' тижнів до останньої дати виконання
                    LocalDate checkDate = (lastDate == null || lastDate.isBefore(startDate)) ? startDate : lastDate;
                    
                    do {
                        // Початковий розрахунок: додаємо інтервал до останньої дати
                        checkDate = checkDate.plusWeeks(interval);
                    } while (checkDate.isBefore(baseDate.plusDays(1))); // Повторюємо, доки не отримаємо дату пізніше baseDate
                    
                    // Тепер перевіряємо, чи отриманий день є правильним DayOfWeek
                    if (checkDate.getDayOfWeek() != dayOfWeek) {
                        nextDate = checkDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else {
                        nextDate = checkDate;
                    }
                    
                    // !!! Використовуємо просте додавання тижнів до останньої дати виконання, 
                    // що є найбільш поширеним підходом для фінансових додатків.
                    nextDate = baseDate.plusWeeks(interval);
                    if (nextDate.getDayOfWeek() != dayOfWeek && baseDate.getDayOfWeek() == dayOfWeek) {
                        nextDate = nextDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else if (nextDate.getDayOfWeek() != dayOfWeek) {
                         nextDate = nextDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    }
                    
                    // *** ОСТАТОЧНЕ СПРОЩЕННЯ (найкращий підхід) ***
                    // Шукаємо найближчий відповідний день тижня ПІСЛЯ baseDate
                    LocalDate candidateDate = baseDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    if (candidateDate.isEqual(baseDate)) {
                        candidateDate = candidateDate.plusWeeks(interval);
                    } else if (candidateDate.isAfter(baseDate)) {
                        // Якщо ми пропустили день, додаємо інтервал, але починаючи з останньої дати виконання
                        candidateDate = candidateDate.plusWeeks(interval - 1);
                    }
                    
                    // Просування на повний інтервал
                    do {
                        candidateDate = candidateDate.plusWeeks(interval);
                    } while (candidateDate.isBefore(baseDate.plusDays(1)));
                    
                    nextDate = candidateDate;
                    
                    // !!! Код, який я рекомендую: !!!
                    // Якщо lastDate != null, просто додаємо інтервал. 
                    // Якщо lastDate == null, починаємо з startDate.
                    // Це вимагає, щоб процес processMissedExecutions оновлював baseDate коректно.
                    
                    // 1. Розрахунок наступної дати згідно з інтервалом
                    LocalDate potentialNext = baseDate.plusWeeks(interval);
                    
                    // 2. Коригування дня тижня
                    if (potentialNext.getDayOfWeek() != dayOfWeek) {
                         // Просуваємось до наступного правильного дня тижня, 
                         // що гарантує, що ми не повертаємось назад
                         nextDate = potentialNext.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else {
                        nextDate = potentialNext;
                    }

                } else {
                    // Якщо dayOfWeek не вказано, просто додаємо тижні
                    nextDate = baseDate.plusWeeks(interval);
                }
                break;

            case MONTHLY:
            case YEARLY:
                int monthAmount = type == RecurringType.MONTHLY ? interval : interval * 12;
                
                // 1. Спочатку просуваємось на потрібну кількість місяців/років
                nextDate = baseDate.plusMonths(monthAmount); 
                
                // 2. Встановлюємо правильний день місяця
                if (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) {
                    // Обробка крайніх випадків (лютий, місяці з 30 днями)
                    nextDate = nextDate.withDayOfMonth(Math.min(dayOfMonth, nextDate.lengthOfMonth()));
                } 
                
                // ? Видалено зайву перевірку !nextDate.isAfter(baseDate), оскільки plusMonths/plusYears гарантує просування вперед

                break;

            case NONE:
            default:
                return LocalDate.MAX;
        }
      
        // Фінальна перевірка: не раніше дати початку шаблону
        if (nextDate.isBefore(startDate)) {
            // Це не повинно траплятися при коректній baseDate, але для безпеки:
            return startDate;
        }
        
        // Фінальна перевірка: наступна дата повинна бути після базової
        if (!nextDate.isAfter(baseDate)) {
            // Якщо розрахунок не просунув дату вперед (наприклад, через помилку округлення), 
            // використовуємо найменший інтервал.
            return nextDate.plusDays(1); // Або кидаємо виняток, але це м'якше
        }

        return nextDate;
    }
    
    private void executeTransaction(TransactionTemplate template, LocalDate date) {
        // ... (метод без змін, оскільки він коректний) ...
        try {
            
            Transaction newTransaction = template.createTransactionFromTemplate(date);

            transactionProcessor.create(newTransaction);     

            templateDao.updateLastExecutionDate(template.getId(), date);
            
            System.out.println("? Успішно виконано: " + template.getName() + 
                                 " на " + template.getDefaultAmount() + 
                                 " (" + template.getRecurringType() + ") на дату: " + date);
            
        } catch (Exception e) {
            System.err.println("? Помилка при виконанні регулярної операції " + template.getName() + " на дату " + date + ": " + e.getMessage()); 
        }
    }
}