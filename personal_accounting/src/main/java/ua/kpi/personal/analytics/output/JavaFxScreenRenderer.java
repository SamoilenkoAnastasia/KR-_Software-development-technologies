package ua.kpi.personal.analytics.output;

import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.TableCell;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportDataSet;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.Account;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.List;

public class JavaFxScreenRenderer implements OutputRenderer {
    
    private final TableView<ReportDataPoint> tableView;
    private final Label summaryLabel;
    private final AnchorPane chartContainer;

    public JavaFxScreenRenderer(TableView<ReportDataPoint> tableView, Label summaryLabel, AnchorPane chartContainer) {
        this.tableView = tableView;
        this.summaryLabel = summaryLabel;
        this.chartContainer = chartContainer;
    }

    @Override
    public void render(ReportDataSet dataSet) {
        
        summaryLabel.setText(dataSet.getSummaryText());
        
        updateTableView(dataSet);
        updateChart(dataSet.getDataPoints(), dataSet.getTitle());
        
        System.out.println("Відображення звіту '" + dataSet.getTitle() + "' на екрані завершено.");
    }
    
    @Override
    public void renderAllTransactionsTable(List<Transaction> transactions) {
        tableView.getColumns().clear();
        
        tableView.setItems(FXCollections.observableList((List<ReportDataPoint>) (List<?>) transactions));
        
        TableColumn<Transaction, LocalDateTime> dateColTx = new TableColumn<>("Дата");
        dateColTx.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateColTx.setCellFactory(column -> new TableCell<Transaction, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });
        
        TableColumn<Transaction, String> typeColTx = new TableColumn<>("Тип");
        typeColTx.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        TableColumn<Transaction, Category> categoryColTx = new TableColumn<>("Категорія");
        categoryColTx.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColTx.setCellFactory(column -> new TableCell<Transaction, Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        TableColumn<Transaction, Double> amountColTx = new TableColumn<>("Сума");
        amountColTx.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColTx.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    Transaction tx = (Transaction) getTableRow().getItem();
                    if (tx != null) {
                        if ("INCOME".equalsIgnoreCase(tx.getType())) {
                            setStyle("-fx-text-fill: green;");
                        } else if ("EXPENSE".equalsIgnoreCase(tx.getType())) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            }
        });
        
        TableColumn<Transaction, Account> accountColTx = new TableColumn<>("Рахунок");
        accountColTx.setCellValueFactory(new PropertyValueFactory<>("account"));
        accountColTx.setCellFactory(column -> new TableCell<Transaction, Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        
        tableView.getColumns().addAll(
            (TableColumn<ReportDataPoint, ?>) (TableColumn<?, ?>) dateColTx,
            (TableColumn<ReportDataPoint, ?>) (TableColumn<?, ?>) typeColTx,
            (TableColumn<ReportDataPoint, ?>) (TableColumn<?, ?>) categoryColTx,
            (TableColumn<ReportDataPoint, ?>) (TableColumn<?, ?>) amountColTx,
            (TableColumn<ReportDataPoint, ?>) (TableColumn<?, ?>) accountColTx
        );
    }
    
    @Override
    public void renderChart(List<? extends ReportDataPoint> chartData) {
        updateChart(chartData, "Співвідношення Дохід/Витрата");
    }

    private void updateTableView(ReportDataSet dataSet) {
        tableView.getColumns().clear();
        tableView.setItems(FXCollections.observableList(dataSet.getDataPoints()));
        
        TableColumn<ReportDataPoint, String> keyCol = new TableColumn<>(dataSet.getColumnHeaders()[0]);
        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
        
        TableColumn<ReportDataPoint, Double> valueCol = new TableColumn<>(dataSet.getColumnHeaders()[1]);
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setCellFactory(column -> new TableCell<ReportDataPoint, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        TableColumn<ReportDataPoint, Double> secondaryCol = new TableColumn<>(dataSet.getColumnHeaders()[2]);
        secondaryCol.setCellValueFactory(new PropertyValueFactory<>("secondaryValue"));
        secondaryCol.setCellFactory(column -> new TableCell<ReportDataPoint, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (dataSet.getTitle().contains("Категоріях")) {
                    setText(String.format("%.1f %%", item * 100));
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        tableView.getColumns().addAll(keyCol, valueCol, secondaryCol);
    }
    
    private void updateChart(List<? extends ReportDataPoint> dataPoints, String title) {
        chartContainer.getChildren().clear();

        if (dataPoints.size() > 0 && dataPoints.size() <= 10 && !title.contains("Динаміка")) {
            List<PieChart.Data> pieData = dataPoints.stream()
                .map(p -> new PieChart.Data(p.getKey(), p.getValue()))
                .collect(Collectors.toList());
            
            PieChart pieChart = new PieChart(FXCollections.observableList(pieData));
            pieChart.setTitle(title);
            pieChart.setLegendVisible(true);
            
            AnchorPane.setTopAnchor(pieChart, 0.0);
            AnchorPane.setBottomAnchor(pieChart, 0.0);
            AnchorPane.setLeftAnchor(pieChart, 0.0);
            AnchorPane.setRightAnchor(pieChart, 0.0);
            
            chartContainer.getChildren().add(pieChart);
        } else {
            chartContainer.getChildren().add(new Label("Немає даних для діаграми."));
        }
    }
}