package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.model.analytics.ReportDataSet;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.repo.TransactionDao;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AllTransactionsReport extends FinancialReport {

    private static class SimpleReportDataPoint extends ReportDataPoint {
        
        public SimpleReportDataPoint(String key, double value) {
            super(key, value, 0.0);
        }
    }

    public AllTransactionsReport(TransactionDao transactionDao) {
        super(transactionDao); 
    }

    @Override 
    protected ReportDataSet analyze(ReportParams params, User user) {
        String title = "Загальний звіт доходів і витрат";
        String[] headers = {"Дата", "Тип", "Категорія", "Сума", "Рахунок"};
        
        List<Transaction> transactions = transactionDao.findTransactionsByDateRange(params, user.getId());

        double totalIncome = transactions.stream()
            .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
            .mapToDouble(Transaction::getAmount)
            .sum();
            
        double totalExpense = transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        String summary = String.format("? Дохід: %.2f | ? Витрата: %.2f | ?? Баланс: %.2f", 
                                       totalIncome, totalExpense, totalIncome - totalExpense);

        return new ReportDataSet(title, headers, (List<ReportDataPoint>) (List<?>) transactions, summary); 
    }

    @Override 
    protected void renderTable(ReportDataSet dataSet) {
        List<Transaction> transactions = (List<Transaction>) (List<?>) dataSet.getDataPoints(); 
        
        renderer.renderAllTransactionsTable(transactions);
    }
    
    @Override 
    protected void renderChart(ReportDataSet dataSet) {
        List<Transaction> transactions = (List<Transaction>) (List<?>) dataSet.getDataPoints(); 
        
        double totalIncome = transactions.stream()
            .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
            .mapToDouble(Transaction::getAmount)
            .sum();
            
        double totalExpense = transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
            .mapToDouble(Transaction::getAmount)
            .sum();

        List<ReportDataPoint> chartData = new ArrayList<>();
        
        if (totalIncome > 0) {
            chartData.add(new SimpleReportDataPoint("Дохід", totalIncome));
        }
        if (totalExpense > 0) {
            chartData.add(new SimpleReportDataPoint("Витрата", totalExpense));
        }

        renderer.renderChart(chartData);
    }
}