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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptProcessor {

    private final CategoryDao categoryDao = new CategoryDao();
    private static final Map<String, String> VENDOR_CATEGORY_MAP;

    static {
        
        Map<String, String> map = new HashMap<>();

        // Продукти харчування / Супермаркети
        map.put("АТБ", "Продукти");
        map.put("СІЛЬПО", "Продукти");
        map.put("NOVUS", "Продукти");
        map.put("НОВУС", "Продукти");
        map.put("AUCHAN", "Продукти");
        map.put("АШАН", "Продукти");
        map.put("МЕТРО", "Продукти");
        map.put("METRO", "Продукти");
        map.put("FOZZY", "Продукти");
        map.put("VARUS", "Продукти");
        map.put("ВАРУС", "Продукти");
        map.put("КОЛО", "Продукти");
        map.put("ЕКО МАРКЕТ", "Продукти");
        map.put("РУКАВИЧКА", "Продукти");
        map.put("DELIVERY", "Продукти");
        map.put("ДОСТАВКА", "Продукти");

        // Кафе / Ресторани
        map.put("MC DONALDS", "Ресторани");
        map.put("MCDONALDS", "Ресторани");
        map.put("ПУЗАТА ХАТА", "Ресторани");
        map.put("DOMINOS", "Ресторани");
        map.put("SUSHI", "Ресторани");
        map.put("PIZZA", "Ресторани");
        map.put("КОФІЙ", "Ресторани");
        map.put("COFFEE", "Ресторани");
        map.put("КЕБАБ", "Ресторани");

        // Транспорт / Паливо
        map.put("АЗС", "Транспорт");
        map.put("OKKO", "Транспорт");
        map.put("WOG", "Транспорт");
        map.put("SOCAR", "Транспорт");
        map.put("UPG", "Транспорт");
        map.put("БРСМ", "Транспорт");
        map.put("ГАЗПРОМ", "Транспорт");
        map.put("KLO", "Транспорт");
        map.put("UBER", "Транспорт");
        map.put("BOLT", "Транспорт");
        map.put("ЖД", "Подорожі");
        map.put("УЗ", "Подорожі");
        map.put("ЗАЛІЗНИЦЯ", "Подорожі");
        map.put("АВІА", "Подорожі");
        map.put("SKY", "Подорожі");

        // Комунальні / Послуги / Зв'язок
        map.put("КОМУНАЛ", "Комунальні платежі");
        map.put("ОСББ", "Комунальні платежі");
        map.put("ГАЗ", "Комунальні платежі");
        map.put("ЕЛЕКТРО", "Комунальні платежі");
        map.put("ВОДА", "Комунальні платежі");
        map.put("ТЕЛЕКОМ", "Зв'язок");
        map.put("МОБІЛ", "Зв'язок");
        map.put("KYIVSTAR", "Зв'язок");
        map.put("VODAFONE", "Зв'язок");

        // Здоров'я / Аптеки
        map.put("АПТЕКА", "Здоров'я");
        map.put("APTEKA", "Здоров'я");
        map.put("ФАРМ", "Здоров'я");
        map.put("DOBRA", "Здоров'я");

        // Одяг / Краса
        map.put("INDITEX", "Одяг");
        map.put("ZARA", "Одяг");
        map.put("H&M", "Одяг");
        map.put("MAKEUP", "Краса");
        map.put("BROCARD", "Краса");
        map.put("COSMO", "Краса");
        map.put("PARFUM", "Краса");

        //  Техніка / Електроніка
        map.put("ROZETKA", "Техніка");
        map.put("ФОКСТРОТ", "Техніка");
        map.put("ЦИТРУС", "Техніка");
        map.put("COMFY", "Техніка");

        // Розваги / Послуги
        map.put("KINO", "Розваги");
        map.put("ТЕАТР", "Розваги");
        map.put("NETFLIX", "Підписки");
        map.put("SPOTIFY", "Підписки");
        map.put("YOUTUBE", "Підписки");
        map.put("PSN", "Розваги");

        //  Банківські / Інше
        map.put("КОМІСІЯ", "Комісії/штрафи");
        map.put("ПОПОВНЕННЯ", "Перекази");

        VENDOR_CATEGORY_MAP = Collections.unmodifiableMap(map);
    }

    public ScanData processReceipt(File imageFile) throws IOException {
        String rawText = performLocalOCR(imageFile);

        if (rawText.startsWith("OCR_ERROR")) {
            throw new IOException(rawText);
        }

        return parseRawText(rawText);
    }

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


    private ScanData parseRawText(String rawText) {
        double amount = extractAmount(rawText);
        LocalDate date = extractDate(rawText);
        String vendor = extractVendor(rawText);
        String suggestedCategoryName = categorizeVendor(vendor);

        return new ScanData(vendor, amount, date, rawText, suggestedCategoryName);
    }

    private double extractAmount(String rawText) {
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

        Pattern pattern = Pattern.compile("(\\d{1,2}[./]\\d{1,2}[./]\\d{2,4})|(\\d{4}[-/]\\d{2}[-/]\\d{2})");
        Matcher matcher = pattern.matcher(rawText);

        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd.MM.yy")
        };

        while (matcher.find()) {
            String dateStr = matcher.group(0).replace('/', '.').replace('-', '.');

            if (dateStr.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) {
                String[] parts = dateStr.split("\\.");
                dateStr = parts[0] + "." + parts[1] + ".20" + parts[2];
            }

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return LocalDate.now();
    }

    private String extractVendor(String rawText) {
        String[] lines = rawText.split("\\r?\\n");
        if (lines.length > 0) {
            String vendor = lines[0].trim();
            return vendor.substring(0, Math.min(vendor.length(), 50));
        }
        return "Невідомий продавець";
    }

    private String categorizeVendor(String vendor) {
        String upperVendor = vendor.toUpperCase();
        for (Map.Entry<String, String> entry : VENDOR_CATEGORY_MAP.entrySet()) {
            if (upperVendor.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Різне";
    }
}