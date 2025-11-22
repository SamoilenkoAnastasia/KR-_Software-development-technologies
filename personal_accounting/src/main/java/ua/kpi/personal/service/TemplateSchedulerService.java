package ua.kpi.personal.service;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.TransactionTemplate;
import ua.kpi.personal.model.TransactionTemplate.RecurringType;
import ua.kpi.personal.repo.TemplateDao;
import ua.kpi.personal.processor.TransactionProcessor; 
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.time.temporal.TemporalAdjusters; 

public class TemplateSchedulerService {
    
    private final TemplateDao templateDao;
    private final TransactionProcessor transactionProcessor; 

    public TemplateSchedulerService(TemplateDao templateDao, TransactionProcessor transactionProcessor) {
        this.templateDao = templateDao;
        this.transactionProcessor = transactionProcessor;
    }

    /**
     * Запускає перевірку та виконання всіх регулярних операцій для конкретного користувача.
     */
    public void runScheduledChecks(Long userId) {
        List<TransactionTemplate> templates = templateDao.findRecurringByUserId(userId);
        // ? Використовуємо сьогоднішню дату як кінцеву точку для backlogged транзакцій
        LocalDate today = LocalDate.now(); 
        
        System.out.println("--- Планувальник: Перевірка регулярних операцій на " + today + " ---");
        
        for (TransactionTemplate template : templates) {
            // ? Обробляємо всі пропущені та поточну заплановану транзакції
            processMissedExecutions(template, today);
        }
    }

    /**
     * ? ВИПРАВЛЕНО МЕТОД: Обробляє всі пропущені виконання шаблону до сьогоднішньої дати.
     * Оновлює lastExecutionDate прямо в об'єкті template для коректної ітерації.
     */
    private void processMissedExecutions(TransactionTemplate template, LocalDate today) {
        LocalDate startDate = template.getStartDate();
        LocalDate lastDate = template.getLastExecutionDate(); 
        Integer interval = template.getRecurrenceInterval();

        if (interval == null || interval <= 0 || startDate == null || template.getRecurringType() == RecurringType.NONE) {
            return; // Не має сенсу виконувати
        }

        // 1. Встановлюємо початкову дату, з якої починаємо перевірку.
        // Беремо останню дату виконання. Якщо null, беремо дату початку.
        // Віднімаємо 1 день, тому що calculateNextExecutionDate шукає наступну дату ПІСЛЯ переданої.
        LocalDate currentDate = (lastDate != null ? lastDate : startDate).minusDays(1);
        
        // Якщо шаблон має початися лише у майбутньому
        if (startDate.isAfter(today)) return;

        System.out.println("--- Планувальник: Обробка шаблону '" + template.getName() + 
                           "'. Починаємо з " + currentDate.plusDays(1) + " ---");


        // Перевіряємо, чи наступна запланована дата вже настала
        while (true) {
            // nextExecutionDate розраховується на основі дати, що передує останньому виконанню (currentDate)
            LocalDate nextExecutionDate = calculateNextExecutionDate(template, currentDate);
            
            // 1. Умова виходу: якщо наступна дата у майбутньому (після today)
            if (nextExecutionDate.isAfter(today)) {
                break;
            }

            // 2. Запобігання нескінченному циклу (якщо calcNextDate повертає ту саму дату)
            if (nextExecutionDate.isEqual(currentDate)) {
                 System.err.println("? Помилка розрахунку дати для " + template.getName() + ". Цикл зупинено.");
                 break;
            }

            // ? ВИКОНАННЯ:
            // Виконуємо транзакцію на заплановану дату (може бути пропущена дата)
            executeTransaction(template, nextExecutionDate);
            
            // ? КЛЮЧОВЕ ВИПРАВЛЕННЯ: ОНОВЛЕННЯ lastExecutionDate В ОБ'ЄКТІ.
            // Це дозволяє циклу `while` правильно ітеруватися до сьогоднішньої дати!
            template.setLastExecutionDate(nextExecutionDate); 
            
            // Пересуваємо поточну дату для наступної ітерації
            currentDate = nextExecutionDate;
        }
    }
    
    /**
     * Розраховує наступну очікувану дату виконання на основі типу періодичності.
     * (Логіка без змін, оскільки вона вже була розроблена коректно для обчислення наступної дати ПІСЛЯ lastDate).
     */
    private LocalDate calculateNextExecutionDate(TransactionTemplate template, LocalDate lastDate) {
        RecurringType type = template.getRecurringType();
        Integer interval = template.getRecurrenceInterval() != null ? template.getRecurrenceInterval() : 1;
        Integer dayOfMonth = template.getDayOfMonth();
        DayOfWeek dayOfWeek = template.getDayOfWeek();

        // 1. Спочатку визначаємо базову дату, від якої потрібно рахувати
        LocalDate nextDate = null;
        
        // ? ВИПРАВЛЕННЯ ПОЧАТКОВОЇ ТОЧКИ
        // Якщо lastDate.isBefore(template.getStartDate()), ми повинні фактично 
        // стартувати з template.getStartDate(). 
        // lastDate = template.getStartDate().minusDays(1) при першому запуску, що коректно.
        
        LocalDate baseDate = lastDate.isBefore(template.getStartDate()) ? template.getStartDate().minusDays(1) : lastDate;


        switch (type) {
            case DAILY:
                nextDate = baseDate.plusDays(interval);
                break;
                
            case WEEKLY:
                // nextDate = baseDate.plusWeeks(interval); // - Це неправильно
                // Ми повинні знайти день тижня, що йде через 'interval' тижнів
                LocalDate nextWeekBase = baseDate.plusWeeks(interval); 
                
                if (dayOfWeek != null) {
                    // Знайти dayOfWeek у наступному базовому тижні
                    // nextOrSame(dayOfWeek) знайде день у цьому ж або наступному тижні.
                    // Якщо baseDate є початком тижня, nextWeekBase буде наступним початком.
                    
                    // Щоб уникнути помилки, коли nextOrSame повертає дату з поточного тижня
                    // Якщо dayOfWeek - це dayOfWeek на baseDate, nextOrSame поверне baseDate.
                    // Ми вже додали 'interval' тижнів, тому ми беремо день тижня,
                    // починаючи з базової дати, щоб знайти потрібний день у майбутньому.

                    // Потрібно знайти дату, що відповідає dayOfWeek, в тижні, що починається після baseDate
                    nextDate = baseDate.with(TemporalAdjusters.next(dayOfWeek));
                    
                    // Якщо ми просунулись на interval тижнів
                    if (nextDate.isBefore(nextWeekBase.plusDays(1))) {
                        // Якщо ми не просунулись, просуваємося вручну
                         nextDate = baseDate.plusWeeks(interval).with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else {
                        nextDate = baseDate.with(TemporalAdjusters.next(dayOfWeek)).plusWeeks(interval-1);
                    }
                    
                    // Більш просте рішення для WEEKLY:
                    // Просто додаємо 'interval' тижнів. Якщо DayOfWeek вказано, знаходимо його в цьому тижні.
                    LocalDate weekBase = baseDate.plusWeeks(interval);
                    if (dayOfWeek != null) {
                         // Це знайде DayOfWeek в тижні, де знаходиться weekBase
                         nextDate = weekBase.with(TemporalAdjusters.previousOrSame(dayOfWeek));
                         // АЛЕ ми хочемо наступний день тижня ПІСЛЯ baseDate
                         // Найпростіший спосіб: baseDate + 7*interval
                         // А потім знайти DayOfWeek, який ми шукаємо
                         
                         nextDate = baseDate.plusWeeks(interval).with(TemporalAdjusters.nextOrSame(dayOfWeek));
                         
                         // Якщо отримали стару дату, беремо наступний тиждень.
                         if (nextDate.isBefore(baseDate) || nextDate.isEqual(baseDate)) {
                             nextDate = nextDate.plusWeeks(1);
                         }

                         // ОСТАТОЧНЕ СПРОЩЕННЯ:
                         // Якщо lastDate була 15.11 (Пт), а DayOfWeek = Пн.
                         // nextDate має бути 18.11 (Пн)
                         nextDate = baseDate.with(TemporalAdjusters.next(dayOfWeek));
                         while(nextDate.isBefore(baseDate.plusWeeks(interval))) {
                             nextDate = nextDate.plusWeeks(1);
                         }
                         
                    } else {
                        nextDate = baseDate.plusWeeks(interval);
                    }
                } else {
                     nextDate = baseDate.plusWeeks(interval);
                }
                break;

            case MONTHLY:
            case YEARLY:
                int amount = type == RecurringType.MONTHLY ? interval : interval * 12;
                LocalDate nextMonthBase = baseDate.plusMonths(amount);
                
                if (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) {
                    // Встановлюємо вказаний день місяця
                    nextDate = nextMonthBase.withDayOfMonth(Math.min(dayOfMonth, nextMonthBase.lengthOfMonth()));
                } else {
                    // Якщо dayOfMonth не вказано, використовуємо той самий день, що був у lastDate
                    nextDate = nextMonthBase;
                }
                
                // ? КОРЕКЦІЯ ДЛЯ ПЕРШОГО ВИКОНАННЯ (тепер має бути менш потрібна завдяки baseDate)
                if (nextDate.isBefore(baseDate) || nextDate.isEqual(baseDate)) {
                    // Якщо дата все ще позаду (через проблеми з 31 числом), просуваємося далі
                    nextDate = type == RecurringType.MONTHLY ? baseDate.plusMonths(interval) : baseDate.plusYears(interval);
                    if (dayOfMonth != null) {
                         nextDate = nextDate.withDayOfMonth(Math.min(dayOfMonth, nextDate.lengthOfMonth()));
                    }
                }
                
                break;

            case NONE:
            default:
                return LocalDate.MAX;
        }
        
        // ? ФІНАЛЬНА ПЕРЕВІРКА: Якщо ми стартували з дати < startDate, 
        // наступна дата має бути як мінімум startDate
        if (lastDate.isBefore(template.getStartDate()) && nextDate.isBefore(template.getStartDate())) {
             return template.getStartDate();
        }

        return nextDate;
    }


    /**
     * Клонує шаблон, створює реальну транзакцію та оновлює дату виконання шаблону.
     * ? Використовує TransactionProcessor для бізнес-логіки.
     */
    private void executeTransaction(TransactionTemplate template, LocalDate date) {
        try {
            // 1. Клонування шаблону в нову реальну модель Transaction
            Transaction newTransaction = template.createTransactionFromTemplate(date);
            
            // 2. Збереження через ПРОЦЕСОР
            // ? Зверніть увагу: TransactionProcessor.create() відповідає за збереження транзакції
            transactionProcessor.create(newTransaction);
            
            // 3. Оновити lastExecutionDate в шаблоні в БД
            // ? Цей виклик зберігає дату в БД, щоб наступний запуск програми бачив, де зупинився
            templateDao.updateLastExecutionDate(template.getId(), date);
            
            System.out.println("? Успішно виконано: " + template.getName() + 
                               " на " + template.getDefaultAmount() + 
                               " (" + template.getRecurringType() + ") на дату: " + date);
            
        } catch (Exception e) {
            System.err.println("? Помилка при виконанні регулярної операції " + template.getName() + " на дату " + date + ": " + e.getMessage());
        }
    }
}