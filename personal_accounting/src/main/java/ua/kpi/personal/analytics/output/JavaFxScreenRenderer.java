package ua.kpi.personal.analytics.output;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox; // ? ДОДАНО: Для вертикального розміщення керування
import ua.kpi.personal.model.analytics.ReportDataPoint;

import java.util.List;
import java.util.stream.Collectors;

public class JavaFxScreenRenderer implements OutputRenderer {

    private final TableView<ReportDataPoint> tableView;
    private final Label summaryLabel;
    private final AnchorPane chartContainer;
    
    // ? НОВІ ПОЛЯ: Для зберігання згенерованих діаграм
    private Node currentPieChart;
    private Node currentLineChart;
    private List<ReportDataPoint> lastDataPoints;

    public JavaFxScreenRenderer(TableView<ReportDataPoint> tableView, Label summaryLabel, AnchorPane chartContainer) {
        this.tableView = tableView;
        this.summaryLabel = summaryLabel;
        this.chartContainer = chartContainer;
    }

    @Override
    public void renderReport(String reportTitle, List<ReportDataPoint> dataPoints, String summary) {
        summaryLabel.setText(summary);
        this.lastDataPoints = dataPoints;
        
        // 1. Рендеринг таблиці
        updateTableView(dataPoints);

        // 2. Генерація та відображення діаграм з вибором
        renderChartsWithSelector(dataPoints, reportTitle);

        System.out.println("? Відображення звіту '" + reportTitle + "' на екрані завершено.");
    }
    
    // **********************************************
    // ? ЛОГІКА З ВИБОРОМ ДІАГРАМИ ?
    // **********************************************
    
    private void renderChartsWithSelector(List<ReportDataPoint> dataPoints, String title) {
        chartContainer.getChildren().clear();

        if (dataPoints.isEmpty()) {
            chartContainer.getChildren().add(new Label("Немає даних для діаграм."));
            return;
        }

        // 1. Попередня генерація обох діаграм
        currentPieChart = createPieChartNode(dataPoints, title);
        currentLineChart = createLineChartNode(dataPoints, title);

        // 2. Створення випадаючого списку (Selector)
        ComboBox<String> chartSelector = new ComboBox<>(FXCollections.observableArrayList(
            "Кругова діаграма (Розподіл)",
            "Лінійна діаграма (Динаміка)"
        ));
        chartSelector.getSelectionModel().selectFirst();
        
        // 3. Створення контейнера для відображення діаграми
        AnchorPane displayPane = new AnchorPane();
        
        // 4. Логіка зміни діаграми при виборі
        chartSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            displayPane.getChildren().clear();
            if ("Кругова діаграма (Розподіл)".equals(newVal)) {
                addChartToPane(displayPane, currentPieChart);
            } else if ("Лінійна діаграма (Динаміка)".equals(newVal)) {
                addChartToPane(displayPane, currentLineChart);
            }
        });

        // 5. Вертикальний контейнер для ComboBox та діаграми
        VBox selectorBox = new VBox(10, chartSelector, displayPane);
        selectorBox.setPadding(new Insets(10));
        VBox.setVgrow(displayPane, javafx.scene.layout.Priority.ALWAYS);
        
        // 6. Початкове відображення (за замовчуванням: Кругова діаграма)
        addChartToPane(displayPane, currentPieChart);
        
        // 7. Додавання елементів до основного контейнера
        chartContainer.getChildren().add(selectorBox);
        AnchorPane.setTopAnchor(selectorBox, 0.0);
        AnchorPane.setBottomAnchor(selectorBox, 0.0);
        AnchorPane.setLeftAnchor(selectorBox, 0.0);
        AnchorPane.setRightAnchor(selectorBox, 0.0);
    }
    
    // Допоміжний метод для розміщення діаграми в AnchorPane
    private void addChartToPane(AnchorPane pane, Node chart) {
        pane.getChildren().add(chart);
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
    }

    // **********************************************
    // ЛОГІКА ДЛЯ TABLE VIEW
    // **********************************************
    
    private void updateTableView(List<ReportDataPoint> dataPoints) {
        tableView.setItems(FXCollections.observableList(dataPoints));

        if (tableView.getColumns().isEmpty()) {
            TableColumn<ReportDataPoint, String> keyCol = new TableColumn<>("Ключ/Період");
            keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
            
            TableColumn<ReportDataPoint, String> valueCol = new TableColumn<>("Основне Значення (UAH)");
            valueCol.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cellData.getValue().getValue()))
            );

            TableColumn<ReportDataPoint, String> secondaryCol = new TableColumn<>("Мітка/Дод. Значення");
            secondaryCol.setCellValueFactory(cellData -> {
                ReportDataPoint point = cellData.getValue();
                String result;
                if (point.getSecondaryValue() != 0.0) {
                    result = String.format("%.2f", point.getSecondaryValue());
                } else {
                    result = point.getLabel();
                }
                return new javafx.beans.property.SimpleStringProperty(result);
            });

            tableView.getColumns().addAll(keyCol, valueCol, secondaryCol);
        }
    }

    // **********************************************
    // ЛОГІКА ДЛЯ ДІАГРАМ
    // **********************************************

    private Node createLineChartNode(List<ReportDataPoint> dataPoints, String title) {
       final CategoryAxis xAxis = new CategoryAxis();
       final NumberAxis yAxis = new NumberAxis();
       final LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);

       lineChart.setTitle("Динаміка: " + title);
       xAxis.setLabel("Період");
       yAxis.setLabel("Сума (UAH)");

       // ? ВИПРАВЛЕННЯ: Встановлюємо нахил міток, щоб запобігти перекриттю
       xAxis.setTickLabelRotation(90); 

       // ? ВИПРАВЛЕННЯ ПОМИЛКИ КОМПІЛЯЦІЇ
       // Рядок 159, що викликав помилку (setTickMarksVisible), видалено або закоментовано.

       // ? УМОВНЕ ПРИХОВУВАННЯ МІТОК
       if (dataPoints.size() > 20) {
           xAxis.setTickLabelsVisible(false);
           // ВИДАЛЕНО: xAxis.setTickMarksVisible(false); // Цей рядок викликав помилку
           xAxis.setLabel("Період (деталі див. у таблиці)");
       }


       XYChart.Series<String, Number> valueSeries = new XYChart.Series<>();
       valueSeries.setName("Основне Значення"); 

       XYChart.Series<String, Number> secondarySeries = new XYChart.Series<>();
       secondarySeries.setName("Додаткове Значення"); 

       for (ReportDataPoint dp : dataPoints) {
           valueSeries.getData().add(new XYChart.Data<>(dp.getKey(), dp.getValue()));
           if (dp.getSecondaryValue() != 0.0) {
                secondarySeries.getData().add(new XYChart.Data<>(dp.getKey(), dp.getSecondaryValue()));
           }
       }

       lineChart.getData().add(valueSeries);
       if (!secondarySeries.getData().isEmpty()) {
            lineChart.getData().add(secondarySeries);
       }

       return lineChart; 
   }

    private Node createPieChartNode(List<ReportDataPoint> dataPoints, String title) {
        
        // Групування та сумування по ключу для кругової діаграми
        List<PieChart.Data> pieData = dataPoints.stream()
            .collect(Collectors.groupingBy(ReportDataPoint::getKey, 
                                           Collectors.summingDouble(p -> Math.abs(p.getValue()))))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 0.01)
            .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());


        if (pieData.isEmpty()) {
            return new Label("Недостатньо даних для кругової діаграми.");
        }

        PieChart pieChart = new PieChart(FXCollections.observableList(pieData));
        pieChart.setTitle("Розподіл: " + title);
        pieChart.setLegendVisible(true);
        
        return pieChart; // Повертаємо, не встановлюючи розмір жорстко
    }
}