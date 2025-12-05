package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.analytics.ReportDataPoint;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects; 

import com.itextpdf.io.font.PdfEncodings; 
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
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.layout.properties.TextAlignment;

public class PdfFileWriter implements OutputRenderer {
    private static final String FONT_PATH = "fonts/arial.ttf"; 
    private static final String BOLD_FONT_PATH = "fonts/arialbd.ttf";
    
    private final Window ownerWindow;    
    
    private PdfFont regularFont;
    private PdfFont boldFont;

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
    
    private Paragraph createStyledParagraph(String text, float fontSize, boolean bold) throws IOException {
        
        PdfFont targetFont = bold ? boldFont : regularFont;
        if (targetFont == null) {
            targetFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        }

        Paragraph p = new Paragraph(text)
                .setFont(targetFont)
                .setFontSize(fontSize);
            
        return p;
    }

    @Override
    public void renderReport(String reportTitle, List<ReportDataPoint> dataPoints, String summary) {
        String defaultFileName = reportTitle.replaceAll("\\s+", "_") + "_" 
                               + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
        File file = showSaveDialog("Зберегти звіт у PDF", defaultFileName);

        if (file != null) {
            System.out.println("Форматуємо " + dataPoints.size() + " рядків та записуємо у PDF...");
            
            try (PdfWriter writer = new PdfWriter(new FileOutputStream(file))) {
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                
                try {
                    regularFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED); 
                    boldFont = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED);
                    
                } catch (IOException e) {
                     System.err.println("Помилка завантаження TrueType шрифту. Перевірте FONT_PATH та BOLD_FONT_PATH.");
                     System.err.println("Використовуються резервні шрифти (без повної підтримки кирилиці).");
                     regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                     boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                }
                
                document.add(createStyledParagraph(reportTitle, 18, true).setTextAlignment(TextAlignment.CENTER));
                document.add(createStyledParagraph("Згенеровано: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE), 10, false));
                document.add(new Paragraph(" "));
                
                if (!dataPoints.isEmpty()) {
                    float[] columnWidths = {33.3f, 33.3f, 33.3f};
                    
                    Table table = new Table(UnitValue.createPercentArray(columnWidths));
                    table.setWidth(UnitValue.createPercentValue(100));

                    table.addHeaderCell(new Cell().add(createStyledParagraph("Опис", 12, true)));
                    table.addHeaderCell(new Cell().add(createStyledParagraph("Сума", 12, true)));
                    table.addHeaderCell(new Cell().add(createStyledParagraph("Категорія/Дод. Значення", 12, true)));

                    for (ReportDataPoint point : dataPoints) {
                        
                        table.addCell(new Cell().add(createStyledParagraph(point.getKey(), 10, false)));
   
                        table.addCell(new Cell().add(createStyledParagraph(String.format("%.2f", point.getValue()), 10, false)));
  
                        String secondaryVal;
                        if (point.getSecondaryValue() != 0.0) {
                            secondaryVal = String.format("%.2f", point.getSecondaryValue());
                        } else {
                            secondaryVal = Objects.toString(point.getLabel(), "");
                        }
                        table.addCell(new Cell().add(createStyledParagraph(secondaryVal, 10, false)));
                    }

                    document.add(table);
                    document.add(new Paragraph(" "));
                }

                
                if (summary != null) {
                    document.add(createStyledParagraph("Підсумок:", 12, true));
                    document.add(createStyledParagraph(summary, 10, false));
                }

                document.close();
                
                System.out.printf("Експорт завершено. Файл збережено: %s%n", file.getAbsolutePath());
                
            } catch (IOException e) {
                System.err.println("Помилка експорту PDF: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Експорт скасовано користувачем.");
        }
    }
    
}