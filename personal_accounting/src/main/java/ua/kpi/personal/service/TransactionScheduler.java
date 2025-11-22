package ua.kpi.personal.service;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.TransactionTemplate;
import ua.kpi.personal.model.TransactionTemplate.RecurringType;
import ua.kpi.personal.repo.TemplateDao;
import ua.kpi.personal.repo.TransactionDao;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class TransactionScheduler {
    
    private final TemplateDao templateDao = new TemplateDao();
    private final TransactionDao transactionDao = new TransactionDao(); 

    /**
     * «апускаЇ перев≥рку вс≥х регул€рних операц≥й дл€ конкретного користувача.
     * ” реальному проект≥ це краще запускати дл€ вс≥х користувач≥в циклом.
     */
    public void runScheduledChecks(Long userId) {
        List<TransactionTemplate> templates = templateDao.findRecurringByUserId(userId);
        LocalDate today = LocalDate.now();
        
        System.out.println("--- ѕланувальник: ѕерев≥рка регул€рних операц≥й на " + today + " ---");
        
        for (TransactionTemplate template : templates) {
            if (shouldExecute(template, today)) {
                executeTransaction(template, today);
            }
        }
    }

    /**
     * Ћог≥ка визначенн€, чи потр≥бно виконати операц≥ю сьогодн≥, 
     * враховуючи ≥нтервал та дату останнього виконанн€.
     */
    private boolean shouldExecute(TransactionTemplate template, LocalDate today) {
        LocalDate lastDate = template.getLastExecutionDate();
        LocalDate startDate = template.getStartDate();
        Integer interval = template.getRecurrenceInterval();

        // якщо ≥нтервал не заданий (хоча не мав би бути дл€ RecurringType != NONE), або дата початку п≥зн≥ше сьогодн≥, виходимо.
        if (interval == null || interval <= 0 || (startDate != null && today.isBefore(startDate))) {
            return false;
        }

        // 1. ¬изначаЇмо базову дату дл€ розрахунку наступного виконанн€.
        // якщо lastDate Ї, беремо њњ. якщо немаЇ, беремо startDate.
        LocalDate baseDate = (lastDate != null) ? lastDate : startDate;

        // якщо lastDate або startDate не встановлен≥ (помилка даних), виходимо.
        if (baseDate == null) {
            return false;
        }

        // якщо baseDate дор≥внюЇ today, це означаЇ, що ми або т≥льки-но почали, або вже виконали сьогодн≥, 
        // або lastDate була встановлена на майбутнЇ. 
        // якщо lastDate Ї, ≥ вона дор≥внюЇ today, то ми вже виконали.
        if (lastDate != null && lastDate.isEqual(today)) {
             return false;
        }

        // 2. Ћог≥ка залежно в≥д типу пер≥одичност≥
        switch (template.getRecurringType()) {
            case DAILY:
                // ¬иконати, €кщо сьогодн≥шн€ дата дор≥внюЇ або п≥зн≥ша за останню дату виконанн€ + ≤нтервал дн≥в
                LocalDate nextDaily = baseDate.plusDays(interval);
                return today.isEqual(nextDaily) || today.isAfter(nextDaily);

            case WEEKLY:
                // 1. якщо сьогодн≥ не той день тижн€, що запланований, повертаЇмо false.
                // ¬икористовуЇмо DayOfWeek з шаблону, €кщо в≥н Ї, або з startDate.
                DayOfWeek scheduledDayOfWeek = template.getDayOfWeek() != null ? template.getDayOfWeek() : startDate.getDayOfWeek();
                if (today.getDayOfWeek() != scheduledDayOfWeek) {
                    return false;
                }
                
                // 2. якщо день тижн€ зб≥гс€, перев≥р€Їмо, чи пройшов ≥нтервал тижн≥в
                // Ќаступна дата маЇ бути baseDate + ≥нтервал тижн≥в
                LocalDate nextWeekly = baseDate.plusWeeks(interval);
                return today.isEqual(nextWeekly) || today.isAfter(nextWeekly);

            case MONTHLY:
                // 1. ¬изначаЇмо запланований день м≥с€ц€
                int checkDayOfMonth = (template.getDayOfMonth() != null) ? template.getDayOfMonth() : startDate.getDayOfMonth();
                
                // 2. якщо сьогодн≥шн≥й день м≥с€ц€ не зб≥гаЇтьс€, повертаЇмо false.
                if (today.getDayOfMonth() != checkDayOfMonth) {
                    return false;
                }
                
                // 3. якщо день м≥с€ц€ зб≥гс€, перев≥р€Їмо, чи пройшов ≥нтервал м≥с€ц≥в
                // Ќаступна дата маЇ бути baseDate + ≥нтервал м≥с€ц≥в
                LocalDate nextMonthly = baseDate.plusMonths(interval);
                
                // ќск≥льки ми вже пор≥вн€ли день м≥с€ц€, достатньо пор≥вн€ти, чи сьогодн≥шн≥й м≥с€ць >= м≥с€цю наступного виконанн€
                // якщо lastDate була 28.02.2024 (≥нтервал 1) ≥ сьогодн≥ 28.03.2024, nextMonthly буде 28.03.2024.
                return today.isEqual(nextMonthly) || today.isAfter(nextMonthly);

            case YEARLY:
                // 1. ѕерев≥р€Їмо, чи сьогодн≥ той самий день ≥ м≥с€ць, що й у startDate
                if (today.getMonth() != startDate.getMonth() || today.getDayOfMonth() != startDate.getDayOfMonth()) {
                    return false;
                }
                
                // 2. якщо день ≥ м≥с€ць зб≥глис€, перев≥р€Їмо, чи пройшов ≥нтервал рок≥в
                LocalDate nextYearly = baseDate.plusYears(interval);
                return today.isEqual(nextYearly) || today.isAfter(nextYearly);

            case NONE:
            default:
                return false;
        }
    }

    /**
     *  лонуЇ шаблон, створюЇ реальну транзакц≥ю та оновлюЇ дату виконанн€ шаблону.
     */
    private void executeTransaction(TransactionTemplate template, LocalDate date) {
        try {
            // 1.  лонуванн€ шаблону в нову реальну модель Transaction
            Transaction newTransaction = template.createTransactionFromTemplate(date);
            
            // 2. «берегти реальну транзакц≥ю в Ѕƒ
            transactionDao.create(newTransaction);
            
            // 3. ќновити lastExecutionDate в шаблон≥
            templateDao.updateLastExecutionDate(template.getId(), date);
            
            System.out.println("? ”сп≥шно виконано: " + template.getName() + 
                               " на " + template.getDefaultAmount() + 
                               " (" + template.getRecurringType() + ")");
            
        } catch (Exception e) {
            System.err.println("? ѕомилка при виконанн≥ регул€рноњ операц≥њ " + template.getName());
            e.printStackTrace();
        }
    }
}