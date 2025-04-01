/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.patterns.tools;

import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.Models.MODEL_GEMMA3_4B;

import ai.patterns.base.AbstractBase;
import ai.patterns.data.TopicReport;
import ai.patterns.utils.ChatUtils;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class CurrencyManagerTool extends AbstractBase {
  // Maps countries to their currency codes
  private final Map<String, String> countryCurrencyMap = new HashMap<>();

  // Maps currency codes to their exchange rates (relative to USD)
  private final Map<String, Double> exchangeRates = new HashMap<>();

  // API endpoints
  private static final String COUNTRIES_API_URL = "https://restcountries.com/v3.1/all?fields=name,currencies";
  private static final String EXCHANGE_RATES_API_URL = "https://open.er-api.com/v6/latest/USD";

  interface CurrencyManagerAssistant {
    @SystemMessage("""
            You are a Currency Archive assistant. Your task is to:
            1. Get the currency for a given country, by country name
            2. Ensure the content is valid UTF-8 text
            3. Return the cleaned content as a properly escaped JSON string
            4. If you encounter any issues with the file content, return a clear error message
            """)
    String findCurrency(String cityArticle);

    @SystemMessage("""
            You are a Currency Archive assistant. Your task is to:
            1. Fetch exchange rates from one currency to another from a currency exchange service 
            2. Ensure the content is valid UTF-8 text
            3. Return the cleaned content as a properly escaped JSON string
            4. If you encounter any issues with the file content, return a clear error message
            """)
    String getExchangeRates(String fromCurrency, String toCurrency);

    @SystemMessage(fromResource = "templates/currency-manager-system.txt")
    Result<String> buildCurrencyReport(String userMessage);
  }

  @Tool("Get the currency for a country, by country name")
  TopicReport getCurrencyByCountry(String country) throws Exception {
    System.out.println(blue(">>> Invoking `getCurrencyByCountry` tool with country: ") + country);

    if(countryCurrencyMap.isEmpty()){
      long start = System.currentTimeMillis();
      fetchCountryCurrencyData();
      System.out.println("Fetch currencies from online service(ms): " + (System.currentTimeMillis() - start));
    }

    return new TopicReport(country, String.format("The currency for country %s is %s", country, countryCurrencyMap.get(country)));
  }

  @Tool("Get the exchange rate from one currency to another currency")
  TopicReport getExchangeRate(String fromCurrency, String toCurrency)throws Exception {
    System.out.println(blue(String.format(">>> Invoking `getExchangeRate` tool from currency: %s to currency %s",
                          fromCurrency, toCurrency)));

    if(exchangeRates.isEmpty()){
      long start = System.currentTimeMillis();
      fetchExchangeRates();
      System.out.println("Fetch exchange rates from online service(ms): " + (System.currentTimeMillis() - start));
    }

    CurrencyManagerAssistant currencyAssistant = AiServices.builder(CurrencyManagerAssistant.class)
        .chatLanguageModel(getChatLanguageModelOllama(ChatUtils.getDefaultChatOptions(MODEL_GEMMA3_4B)))
        .build();

    StringBuilder userMessage = new StringBuilder(String.format("What is the exchange rate from %s to %s?", fromCurrency, toCurrency));
    userMessage.append(String.format("Use this helping information: The exchange rate between currencies: %s to %s is set at %s. It has last been updated at %s",
            fromCurrency,
            toCurrency,
            exchangeRates.get(toCurrency),
            getLastUpdateTime()));
    Result<String> reportResult = currencyAssistant.buildCurrencyReport(userMessage.toString());

    return new TopicReport("Exchange rate", reportResult.content());
  }

  // Utility methods
  /**
   * Fetches country and currency data from the REST Countries API.
   *
   * @throws IOException If there is an issue with the API request
   */
  private void fetchCountryCurrencyData() throws IOException, JSONException {
    String jsonResponse = makeApiRequest(COUNTRIES_API_URL);
    JSONArray countries = new JSONArray(jsonResponse);

    for (int i = 0; i < countries.length(); i++) {
      JSONObject country = countries.getJSONObject(i);
      String countryName = country.getJSONObject("name").getString("common");

      // Extract the first currency code from the currencies object
      JSONObject currencies = country.optJSONObject("currencies");
      if ((currencies != null) && (currencies.length() > 0)) {
        String currencyCode = currencies.keys().next().toString();
        countryCurrencyMap.put(countryName, currencyCode);
      }
    }
  }

  /**
   * Fetches current exchange rates from the Exchange Rates API.
   *
   * @throws IOException If there is an issue with the API request
   */
  private void fetchExchangeRates() throws IOException, JSONException {
    String jsonResponse = makeApiRequest(EXCHANGE_RATES_API_URL);

    JSONObject responseObj = new JSONObject(jsonResponse);

    if (responseObj.has("rates")) {
      JSONObject rates = responseObj.getJSONObject("rates");
      Iterator<String> keys = rates.keys();
      while (keys.hasNext()) {
        String currencyCode = keys.next();
        double rate = rates.getDouble(currencyCode);
        exchangeRates.put(currencyCode, rate);
      }
    }

  }

  /**
   * Returns the last update time of the exchange rates.
   *
   * @return A string representing when the rates were last updated
   */
  public String getLastUpdateTime() throws IOException, JSONException {
    String jsonResponse = makeApiRequest(EXCHANGE_RATES_API_URL);
    JSONObject responseObj = new JSONObject(jsonResponse);

    if (responseObj.has("time_last_update_utc")) {
      return responseObj.getString("time_last_update_utc");
    }

    return "Unknown";
  }

  /**
   * Makes an HTTP GET request to the specified URL and returns the response.
   *
   * @param apiUrl The URL to make the request to
   * @return The response body as a string
   * @throws IOException If there is an issue with the request
   */
  private String makeApiRequest(String apiUrl) throws IOException {
    URL url = new URL(apiUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      throw new IOException("API request failed with response code: " + responseCode);
    }

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;

    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();

    return response.toString();
  }
}
