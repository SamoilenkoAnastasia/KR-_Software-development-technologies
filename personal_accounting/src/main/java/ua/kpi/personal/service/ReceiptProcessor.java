package ua.kpi.personal.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import ua.kpi.personal.model.ScanData;
import ua.kpi.personal.repo.CategoryDao;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptProcessor {

    private final CategoryDao categoryDao = new CategoryDao();

    // Словник для Автоматичної Категоризації (без змін)
    private static final Map<String, String> VENDOR_CATEGORY_MAP = Map.of(
        "АТБ", "Продукти",
        "СІЛЬПО", "Продукти",
        "НОВУС", "Продукти",
        "ЗАЛІЗНИЦЯ", "Подорожі",
        "АЗС", "Транспорт",
        "АПТЕКА", "Здоров'я"
    );

    /**
     * Основний метод обробки чека: викликає OCR та парсинг.
     */
    public ScanData processReceipt(File imageFile) throws IOException {
        String rawText = performLocalOCR(imageFile);

        if (rawText.startsWith("OCR_ERROR")) {
            throw new IOException(rawText);
        }

        return parseRawText(rawText);
    }

    // --- ЛОКАЛЬНИЙ OCR: ВИКОРИСТАННЯ TESSERACT (TESS4J) (без змін) ---
    private String performLocalOCR(File imageFile) throws IOException {
        ITesseract tesseract = new Tesseract();
        try {
            String dataPath = new File("tessdata").getAbsolutePath();
            tesseract.setDatapath(dataPath);
        } catch (Exception e) {
            System.err.println("Помилка налаштування шляху Tesseract: " + e.getMessage());
            return "OCR_ERROR: Не знайдено tessdata. Переконайтеся, що папка 'tessdata' з мовними файлами знаходиться поруч із JAR.";
        }
        tesseract.setLanguage("ukr+eng");
        tesseract.setPageSegMode(6);

        try {
            String rawText = tesseract.doOCR(imageFile);
            return rawText.toUpperCase();
        } catch (TesseractException e) {
            System.err.println("Помилка Tesseract OCR: " + e.getMessage());
            return "OCR_ERROR: Помилка виконання Tesseract OCR. Можливо, пошкоджений файл або не знайдено 'tessdata'.";
        }
    }

    // --- ПАРСИНГ ТА КАТЕГОРИЗАЦІЯ ---

    private ScanData parseRawText(String rawText) {
        double amount = extractAmount(rawText);
        LocalDate date = extractDate(rawText);
        String vendor = extractVendor(rawText);
        String suggestedCategoryName = categorizeVendor(vendor);

        return new ScanData(vendor, amount, date, rawText, suggestedCategoryName);
    }

    private double extractAmount(String rawText) {
        // Логіка вилучення суми (без змін)
        Pattern pattern = Pattern.compile("(ВСЬОГО|СУМА|TOTAL|СУММА|ДО СПЛАТИ)\\s*[:\\s]*(\\d+[.,]\\d{2})", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(rawText);

        double foundAmount = 0.0;
        while (matcher.find()) { 
            try {
                String amountStr = matcher.group(2).replace(',', '.');
                foundAmount = Double.parseDouble(amountStr);
            } catch (NumberFormatException ignored) {}
        }
        return foundAmount;
    }

    private LocalDate extractDate(String rawText) {
        // ? ПОКРАЩЕННЯ: Більш гнучкий регулярний вираз та формати
        
        // Патерни: DD.MM.YYYY або DD/MM/YYYY, YYYY-MM-DD, DD.MM.YY
        Pattern pattern = Pattern.compile("(\\d{1,2}[./]\\d{1,2}[./]\\d{2,4})|(\\d{4}[-/]\\d{2}[-/]\\d{2})");
        Matcher matcher = pattern.matcher(rawText);

        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd.MM.yy")
        };

        while (matcher.find()) {
            String dateStr = matcher.group(0).replace('/', '.').replace('-', '.'); // Нормалізація до DD.MM.YYYY
            
            // Якщо рік двозначний, пробуємо додати 2000
            if (dateStr.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) {
                 String[] parts = dateStr.split("\\.");
                 dateStr = parts[0] + "." + parts[1] + ".20" + parts[2];
            }
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException ignored) {
                    // Пробуємо наступний формат
                }
            }
        }
        return LocalDate.now();
    }

    private String extractVendor(String rawText) {
        // Логіка вилучення продавця (без змін)
        String[] lines = rawText.split("\\r?\\n");
        if (lines.length > 0) {
            String vendor = lines[0].trim();
            return vendor.substring(0, Math.min(vendor.length(), 50)); 
        }
        return "Невідомий продавець";
    }

    private String categorizeVendor(String vendor) {
        // Логіка категоризації (без змін)
        String upperVendor = vendor.toUpperCase();
        for (Map.Entry<String, String> entry : VENDOR_CATEGORY_MAP.entrySet()) {
            if (upperVendor.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Різне";
    }
}