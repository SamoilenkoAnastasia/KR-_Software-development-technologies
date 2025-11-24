package ua.kpi.personal.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ua.kpi.personal.model.ExchangeRate;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ExchangeRateService {

   
    private static final String API_URL = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchangenew?json";

    private final HttpClient httpClient = HttpClient.newBuilder()
                                        .executor(Executors.newSingleThreadExecutor())
                                        .build();
    private final Gson gson = new Gson();

    public Map<String, Double> getRates() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Type listType = new TypeToken<List<ExchangeRate>>(){}.getType();
                List<ExchangeRate> rates = gson.fromJson(response.body(), listType);

                return rates.stream()
                        .filter(r -> "USD".equals(r.getCc()) || "EUR".equals(r.getCc()))
                        .collect(Collectors.toMap(ExchangeRate::getCc, ExchangeRate::getRate));
            } else {
                System.err.println("Помилка API НБУ. Код статусу: " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Помилка підключення до API НБУ: " + e.getMessage());
        }
        return Collections.emptyMap(); 
    }
}