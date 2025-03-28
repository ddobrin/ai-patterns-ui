package ai.patterns.tools;

import static ai.patterns.utils.Ansi.blue;

import ai.patterns.base.AbstractBase;
import ai.patterns.data.TopicReport;
import dev.langchain4j.agent.tool.Tool;
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
    String buildCurrencyReport(String userMessage);
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

    return new TopicReport("Exchange Rate",
                          String.format("The exchange rate between currencies: 1 %s to 1 %s is set at %s. It has last been updated at %s",
                              fromCurrency,
                              toCurrency,
                              exchangeRates.get(toCurrency),
                              getLastUpdateTime()));
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
