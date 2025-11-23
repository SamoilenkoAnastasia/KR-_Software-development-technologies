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
    
    // ? ВИПРАВЛЕННЯ 1: Додавання ScheduledExecutorService
    private final ScheduledExecutorService scheduler;
    
    // Використовуємо userId для ізоляції завдань
    private final AtomicLong currentUserId = new AtomicLong(-1L);
    
    public TemplateSchedulerService(TemplateDao templateDao, TransactionProcessor transactionProcessor) {
        this.templateDao = templateDao;
        this.transactionProcessor = transactionProcessor;
        // Ініціалізація планувальника при створенні сервісу
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true); // Дозволяє JVM завершити роботу, якщо це єдиний потік, що залишився
            t.setName("Template-Scheduler-Thread");
            return t;
        });
    }

    // ? ВИПРАВЛЕННЯ 2: Метод, який усуває помилку компіляції в ApplicationSession
    public void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            // М'яке завершення роботи, що дозволяє поточним завданням закінчитися
            scheduler.shutdown();
            this.currentUserId.set(-1L); 
            try {
                // Очікуємо завершення протягом 30 секунд
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    // Якщо не завершився, спробуємо примусово зупинити
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                // Перериваємо потік, якщо очікування було перервано
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("? Планувальник шаблонів успішно зупинено.");
        }
    }

    /**
     * Запускає перевірку пропущених транзакцій та встановлює регулярну перевірку.
     * @param userId ID користувача.
     */
    public void runScheduledChecks(Long userId) {
        if (userId == null || userId <= 0) {
            System.err.println("Помилка: Неможливо запустити планувальник, userId недійсний.");
            return;
        }

        if (this.currentUserId.get() == userId.longValue() && !scheduler.isShutdown()) {
            System.out.println("Планувальник вже запущено для користувача ID: " + userId);
            return;
        }
        
        // Зупиняємо попередні завдання, якщо вони були
        if (this.currentUserId.get() != -1L) {
             System.out.println("Планувальник переналаштовується для нового користувача.");
             stopScheduler();
             // Створюємо новий планувальник, щоб уникнути помилок після shutdownNow
             // У цьому прикладі ми припускаємо, що ApplicationSession створить новий екземпляр сервісу,
             // але для безпеки в багатопоточному середовищі краще створювати новий планувальник
             // при кожному логіні або перемиканні користувача, якщо це необхідно.
        }

        this.currentUserId.set(userId);

        // 1. Виконуємо першу перевірку негайно
        scheduler.execute(() -> runScheduledChecksTask(userId));

        // 2. Плануємо щоденну перевірку (наприклад, кожні 24 години)
        // У реальних програмах часто використовують щоденний фіксований час, 
        // але для простоти використовуємо інтервал.
        scheduler.scheduleAtFixedRate(
            () -> runScheduledChecksTask(userId),
            1, // Початкова затримка 1 секунда (після негайного виконання)
            24, // Інтервал 24 години
            TimeUnit.HOURS
        );
        
        System.out.println("? Щоденна перевірка шаблонів запланована для користувача ID: " + userId);
    }
    
    /**
     * Логіка, яка буде виконуватись планувальником.
     */
    private void runScheduledChecksTask(Long userId) {
        try {
            if (this.currentUserId.get() != userId.longValue()) {
                // Запобігання виконання завдання для неактивного користувача
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
        LocalDate startDate = template.getStartDate();
        LocalDate lastDate = template.getLastExecutionDate(); 
        Integer interval = template.getRecurrenceInterval();

        if (interval == null || interval <= 0 || startDate == null || template.getRecurringType() == RecurringType.NONE) {
            return;
        }

        // Початкова дата для перевірки:
        // Якщо lastDate існує, починаємо з неї. Якщо ні, починаємо з startDate.
        LocalDate currentDate = (lastDate != null ? lastDate : startDate).minusDays(1);

        if (startDate.isAfter(today)) return;

        System.out.println("--- Планувальник: Обробка шаблону '" + template.getName() + 
                            "'. Починаємо з " + currentDate.plusDays(1) + " ---");


        // Цикл виконання всіх пропущених транзакцій до сьогоднішнього дня включно
        while (true) {
            
            LocalDate nextExecutionDate = calculateNextExecutionDate(template, currentDate);

            // Якщо наступна дата після сьогоднішнього дня, ми закінчили
            if (nextExecutionDate.isAfter(today)) {
                break;
            }

            // Запобігання нескінченному циклу (якщо логіка calculateNextExecutionDate поверне ту ж саму дату)
            if (!nextExecutionDate.isAfter(currentDate)) {
                 System.err.println("? Помилка розрахунку дати для " + template.getName() + 
                                    ". Наступна дата (" + nextExecutionDate + ") не після поточної (" + currentDate + "). Цикл зупинено.");
                 break;
            }

            executeTransaction(template, nextExecutionDate);
            
            // Оновлюємо currentExecutionDate тільки після успішного виконання
            template.setLastExecutionDate(nextExecutionDate); 
            
            currentDate = nextExecutionDate;
        }
    }
    
    // ? ВИПРАВЛЕННЯ 3: Перероблена логіка calculateNextExecutionDate
    private LocalDate calculateNextExecutionDate(TransactionTemplate template, LocalDate lastDate) {
        RecurringType type = template.getRecurringType();
        Integer interval = template.getRecurrenceInterval() != null ? template.getRecurrenceInterval() : 1;
        Integer dayOfMonth = template.getDayOfMonth();
        DayOfWeek dayOfWeek = template.getDayOfWeek();
        
        LocalDate startDate = template.getStartDate();

        // 1. Визначаємо базову дату для розрахунку.
        // Це має бути остання відома виконана дата (або день перед початком, якщо немає виконання)
        LocalDate baseDate = lastDate.isBefore(startDate) ? startDate.minusDays(1) : lastDate;
        
        LocalDate nextDate = baseDate;

        switch (type) {
            case DAILY:
                nextDate = baseDate.plusDays(interval);
                break;
                
            case WEEKLY:
                // Якщо dayOfWeek вказано (наприклад, щопонеділка):
                if (dayOfWeek != null) {
                    // Знаходимо наступний день тижня (DayOfWeek) від baseDate
                    nextDate = baseDate.with(TemporalAdjusters.next(dayOfWeek)); 
                    
                    // Якщо nextDate все ще в межах того ж тижня, що й baseDate, 
                    // нам потрібно просунутися на 'interval' тижнів вперед.
                    if (nextDate.isAfter(baseDate.plusWeeks(interval))) {
                        // Якщо ми вже перетнули тиждень, але не досягли DayOfWeek, 
                        // нам потрібно знайти DayOfWeek в наступному інтервалі.
                        nextDate = baseDate.plusWeeks(interval).with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else if (interval > 1) {
                         // Якщо інтервал > 1, ми шукаємо наступний день тижня і додаємо (interval - 1) тижнів
                         // щоб переконатися, що ми просунулися на повний інтервал.
                         nextDate = nextDate.plusWeeks(interval - 1);
                    }
                    
                    // Фінальна перевірка: якщо nextDate не після lastDate (baseDate), 
                    // просуваємося на повний інтервал. Це виправляє ситуацію, коли 
                    // lastDate вже була цим DayOfWeek.
                    if (!nextDate.isAfter(baseDate)) {
                        nextDate = nextDate.plusWeeks(interval);
                    }

                } else {
                    // Якщо dayOfWeek не вказано, просто додаємо тижні
                    nextDate = baseDate.plusWeeks(interval);
                }
                break;

            case MONTHLY:
            case YEARLY:
                int amount = type == RecurringType.MONTHLY ? interval : interval * 12;
                
                // 1. Спочатку просуваємось на потрібну кількість місяців/років
                nextDate = baseDate.plusMonths(amount); 
                
                // 2. Встановлюємо правильний день місяця
                if (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) {
                    // Обробка крайніх випадків (лютий, місяці з 30 днями)
                    nextDate = nextDate.withDayOfMonth(Math.min(dayOfMonth, nextDate.lengthOfMonth()));
                } 
                
                // 3. Корекція: якщо після просування nextDate все ще "позаду" baseDate, 
                // це означає, що ми просунулися недостатньо (наприклад, baseDate=31.01, interval=1, nextDate=28.02).
                // Ця корекція не повинна бути потрібна після використання plusMonths(amount) 
                // від baseDate, але залишаємо для перестраховки, якщо логіка моделювання дат змінюється.
                if (!nextDate.isAfter(baseDate)) {
                    // Це означає, що наступна дата повинна бути у наступному інтервалі.
                    // Це складний випадок, зазвичай він не повинен виникати. 
                    // Для уникнення зациклення або помилок, повертаємося до простої логіки:
                    nextDate = type == RecurringType.MONTHLY ? baseDate.plusMonths(interval) : baseDate.plusYears(interval);
                    if (dayOfMonth != null) {
                         nextDate = nextDate.withDayOfMonth(Math.min(dayOfMonth, nextDate.lengthOfMonth()));
                    }
                }
                
                break;

            case NONE:
            default:
                // Якщо немає регулярності, повертаємо максимальну дату (ніколи не виконувати)
                return LocalDate.MAX;
        }

        // 4. Фінальна перевірка на startDate
        // Якщо розрахована дата раніше, ніж офіційна дата початку, використовуємо дату початку.
        if (nextDate.isBefore(startDate)) {
            return startDate;
        }

        return nextDate;
    }
    
    private void executeTransaction(TransactionTemplate template, LocalDate date) {
        try {
            // ? Змінюємо дату транзакції на заплановану
            Transaction newTransaction = template.createTransactionFromTemplate(date);

            // Виконуємо транзакцію (з використанням декораторів)
            transactionProcessor.create(newTransaction);        
            
            // Оновлюємо останню дату виконання в базі даних (критично важливо!)
            templateDao.updateLastExecutionDate(template.getId(), date);
            
            System.out.println("? Успішно виконано: " + template.getName() + 
                                 " на " + template.getDefaultAmount() + 
                                 " (" + template.getRecurringType() + ") на дату: " + date);
            
        } catch (Exception e) {
            System.err.println("? Помилка при виконанні регулярної операції " + template.getName() + " на дату " + date + ": " + e.getMessage());
            // У разі помилки ми не оновлюємо lastExecutionDate, щоб спробувати знову пізніше.
        }
    }
}