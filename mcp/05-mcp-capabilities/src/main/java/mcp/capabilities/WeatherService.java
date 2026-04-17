/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mcp.capabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WeatherService {

  private static final String BASE_URL = "https://api.weather.gov";

  private final RestClient restClient;

  public WeatherService() {

    this.restClient =
        RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Accept", "application/geo+json")
            .defaultHeader("User-Agent", "WeatherApiClient/1.0 (your@email.com)")
            .build();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Points(@JsonProperty("properties") Props properties) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Props(@JsonProperty("forecast") String forecast) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Forecast(@JsonProperty("properties") Props properties) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Props(@JsonProperty("periods") List<Period> periods) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Period(
        @JsonProperty("number") Integer number,
        @JsonProperty("name") String name,
        @JsonProperty("startTime") String startTime,
        @JsonProperty("endTime") String endTime,
        @JsonProperty("isDaytime") Boolean isDayTime,
        @JsonProperty("temperature") Integer temperature,
        @JsonProperty("temperatureUnit") String temperatureUnit,
        @JsonProperty("temperatureTrend") String temperatureTrend,
        @JsonProperty("probabilityOfPrecipitation") Map probabilityOfPrecipitation,
        @JsonProperty("windSpeed") String windSpeed,
        @JsonProperty("windDirection") String windDirection,
        @JsonProperty("icon") String icon,
        @JsonProperty("shortForecast") String shortForecast,
        @JsonProperty("detailedForecast") String detailedForecast) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Alert(@JsonProperty("features") List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(@JsonProperty("properties") Properties properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
        @JsonProperty("event") String event,
        @JsonProperty("areaDesc") String areaDesc,
        @JsonProperty("severity") String severity,
        @JsonProperty("description") String description,
        @JsonProperty("instruction") String instruction) {}
  }

  /**
   * Get forecast for a specific latitude/longitude
   *
   * @param latitude Latitude
   * @param longitude Longitude
   * @return The forecast for the given location
   * @throws RestClientException if the request fails
   */
  @Tool(
      description =
          "Get the current weather forecast for a US latitude/longitude (api.weather.gov — US"
              + " coverage only). Examples: Seattle 47.6062/-122.3321, New York 40.7128/-74.0060,"
              + " Miami 25.7617/-80.1918.")
  public String getWeatherForecastByLocation(
      @ToolParam(description = "US latitude, e.g. 47.6062 (Seattle) or 40.7128 (New York)")
          double latitude,
      @ToolParam(description = "US longitude, e.g. -122.3321 (Seattle) or -74.0060 (New York)")
          double longitude) {

    var points =
        restClient
            .get()
            .uri("/points/{latitude},{longitude}", latitude, longitude)
            .retrieve()
            .body(Points.class);

    var forecast =
        restClient.get().uri(points.properties().forecast()).retrieve().body(Forecast.class);

    String forecastText =
        forecast.properties().periods().stream()
            .map(
                p -> {
                  return String.format(
                      """
					%s:
					Temperature: %s %s
					Wind: %s %s
					Forecast: %s
					""",
                      p.name(),
                      p.temperature(),
                      p.temperatureUnit(),
                      p.windSpeed(),
                      p.windDirection(),
                      p.detailedForecast());
                })
            .collect(Collectors.joining());

    return forecastText;
  }

  /**
   * Get alerts for a specific area
   *
   * @param state Area code. Two-letter US state code (e.g. CA, NY)
   * @return Human readable alert information
   * @throws RestClientException if the request fails
   */
  @Tool(
      description =
          "Get active weather alerts for a US state. Input MUST be a two-letter US state or"
              + " territory code. Examples: CA (California), NY (New York), TX (Texas), FL"
              + " (Florida), WA (Washington), HI (Hawaii), AK (Alaska), PR (Puerto Rico).")
  public String getAlerts(
      @ToolParam(
              description =
                  "Two-letter US state or territory code. Valid values include AL, AK, AZ, AR, CA,"
                      + " CO, CT, DE, FL, GA, HI, ID, IL, IN, IA, KS, KY, LA, ME, MD, MA, MI, MN,"
                      + " MS, MO, MT, NE, NV, NH, NJ, NM, NY, NC, ND, OH, OK, OR, PA, RI, SC, SD,"
                      + " TN, TX, UT, VT, VA, WA, WV, WI, WY, DC, PR.")
          String state) {
    Alert alert =
        restClient.get().uri("/alerts/active/area/{state}", state).retrieve().body(Alert.class);

    return alert.features().stream()
        .map(
            f ->
                String.format(
                    """
					Event: %s
					Area: %s
					Severity: %s
					Description: %s
					Instructions: %s
					""",
                    f.properties().event(),
                    f.properties.areaDesc(),
                    f.properties.severity(),
                    f.properties.description(),
                    f.properties.instruction()))
        .collect(Collectors.joining("\n"));
  }

  public static void main(String[] args) {
    WeatherService client = new WeatherService();
    System.out.println(client.getWeatherForecastByLocation(47.6062, -122.3321));
    System.out.println(client.getAlerts("NY"));
  }
}
