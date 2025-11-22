package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.analytics.ReportDataSet;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.Transaction;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

public class PdfFileWriter implements OutputRenderer {
    
    private final Window ownerWindow;    

    public PdfFileWriter(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    private File showSaveDialog(String title, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultFileName);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showSaveDialog(ownerWindow);
    }
    
    private Paragraph createStyledParagraph(String text, PdfFont font, float fontSize, boolean bold) {
        Paragraph p = new Paragraph(text)
                .setFont(font)
                .setFontSize(fontSize);
        if (bold) {
            p.setBold();
        }
        return p;
    }
    
    @Override
    public void render(ReportDataSet dataSet) {
        String defaultFileName = dataSet.getTitle().replaceAll("\\s+", "_") + "_" 
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
        File file = showSaveDialog("Зберегти агрегований звіт у PDF", defaultFileName);

        if (file != null) {
            System.out.println("Форматуємо " + dataSet.getDataPoints().size() + " рядків та записуємо у PDF...");
            
            try (PdfWriter writer = new PdfWriter(new FileOutputStream(file))) {
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA); 
                
                document.add(createStyledParagraph(dataSet.getTitle(), font, 18, true));
                document.add(createStyledParagraph("Згенеровано: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE), font, 10, false));
                document.add(new Paragraph(" "));
                
                float[] columnWidths = {33.3f, 33.3f, 33.3f};
                Table table = new Table(UnitValue.createPercentArray(columnWidths));
                table.setWidth(UnitValue.createPercentValue(100));

                for (String header : dataSet.getColumnHeaders()) {
                    table.addHeaderCell(new Cell().add(createStyledParagraph(header, font, 12, true)));
                }

                boolean isCategoryReport = dataSet.getTitle().contains("Категоріях");

                for (ReportDataPoint point : dataSet.getDataPoints()) {
                    table.addCell(new Cell().add(createStyledParagraph(point.getKey(), font, 10, false)));
                    table.addCell(new Cell().add(createStyledParagraph(String.format("%.2f", point.getValue()), font, 10, false)));
                    
                    String secondaryVal;
                    if (isCategoryReport) {
                        secondaryVal = String.format("%.1f %%", point.getSecondaryValue() * 100);
                    } else {
                        secondaryVal = String.format("%.2f", point.getSecondaryValue());
                    }
                    table.addCell(new Cell().add(createStyledParagraph(secondaryVal, font, 10, false)));
                }

                document.add(table);
                document.add(new Paragraph(" "));
                
                document.add(createStyledParagraph("Підсумок:", font, 12, true));
                document.add(createStyledParagraph(dataSet.getSummaryText(), font, 10, false));

                document.close();
                
                System.out.printf("? Експорт завершено. Файл збережено: %s%n", file.getAbsolutePath());
                
            } catch (IOException e) {
                 System.err.println("? Помилка експорту PDF: " + e.getMessage());
                 e.printStackTrace();
            }
        } else {
            System.out.println("Експорт скасовано користувачем.");
        }
    }

    @Override
    public void renderAllTransactionsTable(List<Transaction> transactions) {
        String defaultFileName = "Детальний_звіт_транзакцій_"
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
        File file = showSaveDialog("Зберегти детальний звіт транзакцій у PDF", defaultFileName);
        
        if (file != null) {
            try (PdfWriter writer = new PdfWriter(new FileOutputStream(file))) {
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA); 

                document.add(createStyledParagraph("Детальний звіт транзакцій", font, 18, true));
                document.add(createStyledParagraph("Згенеровано: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE), font, 10, false));
                document.add(new Paragraph(" "));
                
                float[] columnWidths = {15f, 10f, 20f, 15f, 20f, 20f};
                Table table = new Table(UnitValue.createPercentArray(columnWidths));
                table.setWidth(UnitValue.createPercentValue(100));

                String[] headers = {"Дата", "Тип", "Категорія", "Сума", "Рахунок", "Опис"};
                for (String header : headers) {
                    table.addHeaderCell(new Cell().add(createStyledParagraph(header, font, 10, true)));
                }

                for (Transaction tx : transactions) {
                    String categoryName = Objects.toString(
                        tx.getCategory() != null ? tx.getCategory().getName() : null, 
                        "Без категорії"
                    );
                    
                    table.addCell(new Cell().add(createStyledParagraph(tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")), font, 8, false)));
                    table.addCell(new Cell().add(createStyledParagraph(tx.getType(), font, 8, false)));
                    table.addCell(new Cell().add(createStyledParagraph(categoryName, font, 8, false)));
                    table.addCell(new Cell().add(createStyledParagraph(String.format("%.2f", tx.getAmount()), font, 8, false)));
                    table.addCell(new Cell().add(createStyledParagraph(Objects.toString(tx.getAccount().getName(), "N/A"), font, 8, false)));
                    table.addCell(new Cell().add(createStyledParagraph(tx.getDescription(), font, 8, false)));
                }

                document.add(table);
                document.close();
                
                System.out.printf("? Експорт детальних транзакцій завершено. Файл збережено: %s%n", file.getAbsolutePath());
                
            } catch (IOException e) {
                 System.err.println("? Помилка експорту детальних транзакцій PDF: " + e.getMessage());
                 e.printStackTrace();
            }
        }
    }
    
    @Override
    public void renderChart(List<? extends ReportDataPoint> chartData) {
        
        System.out.println("--- Експорт даних для діаграми у PDF ---");
        
        String defaultFileName = "Дані_для_діаграми_"
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
        File file = showSaveDialog("Зберегти дані для діаграми у PDF", defaultFileName);

        if (file != null) {
            try (PdfWriter writer = new PdfWriter(new FileOutputStream(file))) {
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA); 

                document.add(createStyledParagraph("Дані для діаграми: Дохід/Витрата", font, 14, true));
                document.add(new Paragraph(" "));
                
                float[] columnWidths = {50f, 50f};
                Table table = new Table(UnitValue.createPercentArray(columnWidths));
                table.setWidth(UnitValue.createPercentValue(50));

                table.addHeaderCell(new Cell().add(createStyledParagraph("Елемент", font, 10, true)));
                table.addHeaderCell(new Cell().add(createStyledParagraph("Значення", font, 10, true)));

                for (ReportDataPoint dp : chartData) {
                    table.addCell(new Cell().add(createStyledParagraph(dp.getKey(), font, 10, false)));
                    table.addCell(new Cell().add(createStyledParagraph(String.format("%.2f", dp.getValue()), font, 10, false)));
                }

                document.add(table);
                document.close();
                
                System.out.printf("? Експорт даних для діаграми завершено. Файл збережено: %s%n", file.getAbsolutePath());
                
            } catch (IOException e) {
                 System.err.println("? Помилка експорту даних діаграми PDF: " + e.getMessage());
            }
        }
    }
}