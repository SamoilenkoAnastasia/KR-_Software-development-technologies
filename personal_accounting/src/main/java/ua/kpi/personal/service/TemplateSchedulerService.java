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
            System.out.println("Планувальник шаблонів успішно зупинено.");
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
            
        }

        this.currentUserId.set(userId);

        scheduler.execute(() -> runScheduledChecksTask(userId));

        scheduler.scheduleAtFixedRate(
            () -> runScheduledChecksTask(userId),
            1, 
            24, 
            TimeUnit.HOURS
        );
        
        System.out.println("Щоденна перевірка шаблонів запланована для користувача ID: " + userId);
    }

    private void runScheduledChecksTask(Long userId) {
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
        LocalDate startDate = template.getStartDate();
        LocalDate lastDate = template.getLastExecutionDate(); 
        Integer interval = template.getRecurrenceInterval();

        if (interval == null || interval <= 0 || startDate == null || template.getRecurringType() == RecurringType.NONE) {
            return;
        }
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
                 System.err.println("Помилка розрахунку дати для " + template.getName() + 
                                     ". Наступна дата (" + nextExecutionDate + ") не після поточної (" + currentDate + "). Цикл зупинено.");
                 break;
            }

            executeTransaction(template, nextExecutionDate);
          
            template.setLastExecutionDate(nextExecutionDate); 

            currentDate = nextExecutionDate;
        }
    }
    
    private LocalDate calculateNextExecutionDate(TransactionTemplate template, LocalDate lastDate) {
        RecurringType type = template.getRecurringType();
        Integer interval = template.getRecurrenceInterval() != null ? template.getRecurrenceInterval() : 1;
        Integer dayOfMonth = template.getDayOfMonth();
        DayOfWeek dayOfWeek = template.getDayOfWeek();
        
        LocalDate startDate = template.getStartDate();

        LocalDate baseDate = lastDate.isBefore(startDate) ? startDate.minusDays(1) : lastDate;
        
        LocalDate nextDate = baseDate;

        switch (type) {
            case DAILY:
                nextDate = baseDate.plusDays(interval);
                break;
                
            case WEEKLY:
                if (dayOfWeek != null) {
                    
                    LocalDate nextWeekBase = baseDate.plusDays(1);

                    LocalDate nextDayOfWeek = nextWeekBase.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    if (nextDayOfWeek.isBefore(baseDate.plusWeeks(interval)) || nextDayOfWeek.isEqual(baseDate.plusWeeks(interval))) {
                        nextDate = nextDayOfWeek;
                    } else {
                        nextDate = baseDate.with(TemporalAdjusters.next(dayOfWeek)).plusWeeks(interval - 1);
                    }

                 
                    LocalDate candidate = baseDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    
                    if (candidate.isAfter(baseDate)) {
                        if (candidate.isAfter(baseDate.plusWeeks(interval))) {
                            nextDate = candidate.minusWeeks(interval - 1); 
                        } else {
                            nextDate = candidate;
                        }
                    } else {
                        nextDate = candidate.plusWeeks(interval);
                    }
 
                    LocalDate checkDate = (lastDate == null || lastDate.isBefore(startDate)) ? startDate : lastDate;
                    
                    do {
                        checkDate = checkDate.plusWeeks(interval);
                    } while (checkDate.isBefore(baseDate.plusDays(1))); 
                    if (checkDate.getDayOfWeek() != dayOfWeek) {
                        nextDate = checkDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else {
                        nextDate = checkDate;
                    }

                    nextDate = baseDate.plusWeeks(interval);
                    if (nextDate.getDayOfWeek() != dayOfWeek && baseDate.getDayOfWeek() == dayOfWeek) {
                        nextDate = nextDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else if (nextDate.getDayOfWeek() != dayOfWeek) {
                         nextDate = nextDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    }

                    LocalDate candidateDate = baseDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    if (candidateDate.isEqual(baseDate)) {
                        candidateDate = candidateDate.plusWeeks(interval);
                    } else if (candidateDate.isAfter(baseDate)) {

                        candidateDate = candidateDate.plusWeeks(interval - 1);
                    }

                    do {
                        candidateDate = candidateDate.plusWeeks(interval);
                    } while (candidateDate.isBefore(baseDate.plusDays(1)));
                    
                    nextDate = candidateDate;
                    

                    LocalDate potentialNext = baseDate.plusWeeks(interval);

                    if (potentialNext.getDayOfWeek() != dayOfWeek) {
   
                         nextDate = potentialNext.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    } else {
                        nextDate = potentialNext;
                    }

                } else {
                    nextDate = baseDate.plusWeeks(interval);
                }
                break;

            case MONTHLY:
            case YEARLY:
                int monthAmount = type == RecurringType.MONTHLY ? interval : interval * 12;

                nextDate = baseDate.plusMonths(monthAmount); 

                if (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) {

                    nextDate = nextDate.withDayOfMonth(Math.min(dayOfMonth, nextDate.lengthOfMonth()));
                } 

                break;

            case NONE:
            default:
                return LocalDate.MAX;
        }

        if (nextDate.isBefore(startDate)) {

            return startDate;
        }

        if (!nextDate.isAfter(baseDate)) {

            return nextDate.plusDays(1);
        }

        return nextDate;
    }
    
    private void executeTransaction(TransactionTemplate template, LocalDate date) {
        try {
            
            Transaction newTransaction = template.createTransactionFromTemplate(date);

            transactionProcessor.create(newTransaction);     

            templateDao.updateLastExecutionDate(template.getId(), date);
            
            System.out.println("Успішно виконано: " + template.getName() + 
                                 " на " + template.getDefaultAmount() + 
                                 " (" + template.getRecurringType() + ") на дату: " + date);
            
        } catch (Exception e) {
            System.err.println("Помилка при виконанні регулярної операції " + template.getName() + " на дату " + date + ": " + e.getMessage()); 
        }
    }
}