package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.analytics.ReportDataPoint;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    
    // ? УНІФІКОВАНИЙ МЕТОД
    @Override
    public void renderReport(String reportTitle, List<ReportDataPoint> dataPoints, String summary) {
        String defaultFileName = reportTitle.replaceAll("\\s+", "_") + "_" 
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
        File file = showSaveDialog("Зберегти звіт у Excel", defaultFileName);

        if (file != null) {
            System.out.println("Форматуємо дані та записуємо в XLSX...");
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(file)) {

                Sheet sheet = workbook.createSheet(reportTitle);

                // Створення стилів
                CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle currencyStyle = createCurrencyStyle(workbook);

                int rowNum = 0;
                
                // 1. Заголовок
                Row titleRow = sheet.createRow(rowNum++);
                titleRow.createCell(0).setCellValue(reportTitle);
                
                // 2. Підсумок
                if (summary != null && !summary.isEmpty()) {
                    rowNum++; // Пропуск рядка
                    Row summaryRow = sheet.createRow(rowNum++);
                    summaryRow.createCell(0).setCellValue("Підсумок:");
                    summaryRow.createCell(1).setCellValue(summary);
                }
                
                rowNum++; // Пропуск рядка перед таблицею

                // 3. Заголовки таблиці
                String[] headers = {"Ключ/Період", "Основне Значення", "Мітка/Дод. Значення"};
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // 4. Рядки даних
                for (ReportDataPoint point : dataPoints) {
                    Row row = sheet.createRow(rowNum++);
                    
                    row.createCell(0).setCellValue(point.getKey());
                    
                    Cell valueCell = row.createCell(1);
                    valueCell.setCellValue(point.getValue());
                    valueCell.setCellStyle(currencyStyle); // Застосовуємо валютний формат
                    
                    // Secondary Value / Label
                    String secondaryVal;
                    if (point.getSecondaryValue() != 0.0) {
                        secondaryVal = String.format("%.2f", point.getSecondaryValue());
                    } else {
                        secondaryVal = point.getLabel();
                    }
                    row.createCell(2).setCellValue(secondaryVal);
                }
                
                // Автоматичне підлаштування ширини стовпців
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(fos);
                System.out.printf("? Експорт %d рядків до %s завершено успішно.%n", 
                                 dataPoints.size(), file.getAbsolutePath());

            } catch (IOException e) {
                System.err.println("? Помилка експорту Excel: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Експорт скасовано користувачем.");
        }
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        // Встановлюємо формат числа з двома знаками після коми (можна додати "UAH" або інший символ)
        style.setDataFormat(format.getFormat("0.00")); 
        return style;
    }

    // ВИДАЛЕНО старі методи: render(ReportDataSet), renderAllTransactionsTable(List<Transaction>), renderChart(...)
}