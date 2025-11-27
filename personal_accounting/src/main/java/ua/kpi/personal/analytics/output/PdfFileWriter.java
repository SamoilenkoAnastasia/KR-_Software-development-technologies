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
import java.util.stream.Collectors;
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
import com.itextpdf.layout.properties.TextAlignment;

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
    
    private Paragraph createStyledParagraph(String text, PdfFont font, float fontSize, boolean bold) throws IOException {
        Paragraph p = new Paragraph(text)
                .setFont(font)
                .setFontSize(fontSize);
        if (bold) {
            p.setBold();
        }
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

                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA); 
                
                
                document.add(createStyledParagraph(reportTitle, font, 18, true).setTextAlignment(TextAlignment.CENTER));
                document.add(createStyledParagraph("Згенеровано: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE), font, 10, false));
                document.add(new Paragraph(" "));
                
                if (!dataPoints.isEmpty()) {
                    int numColumns = 3; 
                    float[] columnWidths = {33.3f, 33.3f, 33.3f};
                    
                    Table table = new Table(UnitValue.createPercentArray(columnWidths));
                    table.setWidth(UnitValue.createPercentValue(100));

                    table.addHeaderCell(new Cell().add(createStyledParagraph("Опис", font, 12, true)));
                    table.addHeaderCell(new Cell().add(createStyledParagraph("Сума", font, 12, true)));
                    table.addHeaderCell(new Cell().add(createStyledParagraph("Категорія/Дод. Значення", font, 12, true)));

                    for (ReportDataPoint point : dataPoints) {
                        
                        table.addCell(new Cell().add(createStyledParagraph(point.getKey(), font, 10, false)));
                        
                       
                        table.addCell(new Cell().add(createStyledParagraph(String.format("%.2f", point.getValue()), font, 10, false)));
                        
                        // Secondary Value / Label ()
                        String secondaryVal;
                        if (point.getSecondaryValue() != 0.0) {
                            secondaryVal = String.format("%.2f", point.getSecondaryValue());
                        } else {
                            secondaryVal = Objects.toString(point.getLabel(), "");
                        }
                        table.addCell(new Cell().add(createStyledParagraph(secondaryVal, font, 10, false)));
                    }

                    document.add(table);
                    document.add(new Paragraph(" "));
                }

                
                if (summary != null) {
                    document.add(createStyledParagraph("Підсумок:", font, 12, true));
                    document.add(createStyledParagraph(summary, font, 10, false));
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