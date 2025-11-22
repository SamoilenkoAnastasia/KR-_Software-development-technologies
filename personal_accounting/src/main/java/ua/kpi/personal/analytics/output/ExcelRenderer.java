package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.analytics.ReportDataSet;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.Transaction;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Objects;
import java.io.File;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelRenderer implements OutputRenderer {
    
    private final Window ownerWindow;    

    public ExcelRenderer(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    private File showSaveDialog(String title, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultFileName);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showSaveDialog(ownerWindow);
    }
    
    @Override
    public void render(ReportDataSet dataSet) {
        String defaultFileName = dataSet.getTitle().replaceAll("\\s+", "_") + "_" 
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
        File file = showSaveDialog("Зберегти агрегований звіт у Excel", defaultFileName);

        if (file != null) {
            System.out.println("Форматуємо дані та записуємо в XLSX...");
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(file)) {

                Sheet sheet = workbook.createSheet(dataSet.getTitle());
                AtomicInteger rowNum = new AtomicInteger(0);

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                
                Row headerRow = sheet.createRow(rowNum.getAndIncrement());
                for (int i = 0; i < dataSet.getColumnHeaders().length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(dataSet.getColumnHeaders()[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (var point : dataSet.getDataPoints()) {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    
                    row.createCell(0).setCellValue(point.getKey());
                    row.createCell(1).setCellValue(point.getValue());
                    
                    String secondaryVal;
                    if (dataSet.getTitle().contains("Категоріях")) {
                        secondaryVal = String.format("%.1f %%", point.getSecondaryValue() * 100);
                    } else {
                        secondaryVal = String.format("%.2f", point.getSecondaryValue());
                    }
                    row.createCell(2).setCellValue(secondaryVal);
                }
                
                Row summaryRow = sheet.createRow(rowNum.getAndIncrement() + 1);
                summaryRow.createCell(0).setCellValue("Підсумок:");
                summaryRow.createCell(1).setCellValue(dataSet.getSummaryText());

                for (int i = 0; i < dataSet.getColumnHeaders().length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(fos);
                System.out.printf("? Експорт %d рядків до %s завершено успішно.%n", 
                                  dataSet.getDataPoints().size(), file.getAbsolutePath());

            } catch (IOException e) {
                System.err.println("? Помилка експорту: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Експорт скасовано користувачем.");
        }
    }

    @Override
    public void renderAllTransactionsTable(List<Transaction> transactions) {
        String defaultFileName = "Детальний_звіт_транзакцій_"
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
        File file = showSaveDialog("Зберегти детальний звіт транзакцій у Excel", defaultFileName);

        if (file != null) {
            System.out.println("Форматуємо детальні транзакції та записуємо в XLSX...");
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(file)) {

                Sheet sheet = workbook.createSheet("Транзакції");
                AtomicInteger rowNum = new AtomicInteger(0);
                
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                String[] headers = {"Дата", "Тип", "Категорія", "Сума", "Рахунок", "Опис"};
                
                Row headerRow = sheet.createRow(rowNum.getAndIncrement());
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (Transaction tx : transactions) {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    
                    row.createCell(0).setCellValue(tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                    row.createCell(1).setCellValue(tx.getType());

                    String categoryName = Objects.toString(
                        tx.getCategory() != null ? tx.getCategory().getName() : null,
                        "Без категорії"
                    );
                    row.createCell(2).setCellValue(categoryName);
                    
                    row.createCell(3).setCellValue(tx.getAmount());
                    row.createCell(4).setCellValue(Objects.toString(tx.getAccount().getName(), "N/A"));
                    row.createCell(5).setCellValue(tx.getDescription());
                }
                
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(fos);
                System.out.printf("? Експорт %d транзакцій до %s завершено успішно.%n", 
                                  transactions.size(), file.getAbsolutePath());

            } catch (IOException e) {
                System.err.println("? Помилка експорту детальних транзакцій: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Експорт детальних транзакцій скасовано користувачем.");
        }
    }
    
    @Override
    public void renderChart(List<? extends ReportDataPoint> chartData) {
        
        System.out.println("--- Експорт даних для діаграми у Excel ---");
        
        String defaultFileName = "Дані_для_діаграми_"
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
        File file = showSaveDialog("Зберегти дані для діаграми у Excel", defaultFileName);

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(file)) {

                Sheet sheet = workbook.createSheet("Дані діаграми");
                AtomicInteger rowNum = new AtomicInteger(0);

                Row headerRow = sheet.createRow(rowNum.getAndIncrement());
                headerRow.createCell(0).setCellValue("Елемент");
                headerRow.createCell(1).setCellValue("Значення");
                
                for (ReportDataPoint dp : chartData) {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    row.createCell(0).setCellValue(dp.getKey());
                    row.createCell(1).setCellValue(dp.getValue());
                }
                
                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);

                workbook.write(fos);
                System.out.printf("? Експорт даних для діаграми (%d елементів) до %s завершено успішно.%n", 
                                  chartData.size(), file.getAbsolutePath());

            } catch (IOException e) {
                 System.err.println("? Помилка експорту даних діаграми: " + e.getMessage());
            }
        }
    }
}