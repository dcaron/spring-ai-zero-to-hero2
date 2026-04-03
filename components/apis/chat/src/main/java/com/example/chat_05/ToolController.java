package com.example.chat_05;

import com.example.chat_05.tool.annotations.TimeTools;
import com.example.chat_05.tool.return_direct.Restaurant;
import com.example.chat_05.tool.return_direct.RestaurantSearch;
import com.example.tracing.TracedEndpoint;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@TracedEndpoint
@RestController
@RequestMapping("/chat/05")
class ToolController {
  private final ChatClient chatClient;

  public ToolController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @GetMapping("/time")
  public String time(@RequestParam(value = "city", defaultValue = "Toronto") String city) {

    return chatClient
        .prompt()
        .tools(new TimeTools())
        .user(
            u ->
                u.text(
                        """
                What is the current time in {city}?
                """)
                    .param("city", city))
        .call()
        .content();
  }

  @GetMapping("/dayOfWeek")
  public String tomorrow(@RequestParam(value = "city", defaultValue = "Toronto") String city) {

    return chatClient
        .prompt()
        .tools(new TimeTools())
        .user(
            u ->
                u.text(
                        """
                What day of the week is tomorrow in {city}?
                """)
                    .param("city", city))
        .call()
        .content();
  }

  @GetMapping("/weather")
  public String getWeather(@RequestParam(value = "city", defaultValue = "Toronto") String city) {

    return chatClient
        .prompt()
        .toolNames("weatherFunction")
        .user(
            u ->
                u.text(
                        """
                What is the current weather in {city}?
                """)
                    .param("city", city))
        .call()
        .content();
  }

  @GetMapping("/pack")
  public String getClothingRecommendation(
      @RequestParam(value = "city", defaultValue = "Toronto") String city) {

    return chatClient
        .prompt()
        .toolNames("weatherFunction")
        .user(
            u ->
                u.text(
                        """
                I am traveling to {city} what kind of clothes should I pack?
                """)
                    .param("city", city))
        .call()
        .content();
  }

  @GetMapping("/search")
  public List<Restaurant> search(
      @RequestParam(
              value = "query",
              defaultValue = "find me an italian restaurant for lunch for 4 people today")
          String query) {

    // RestaurantSearch uses returnDirect=true, so the tool result is returned directly
    // We use content() and parse it, since entity() conflicts with returnDirect in Spring AI 2.0
    String content =
        this.chatClient
            .prompt()
            .tools(new RestaurantSearch(), new TimeTools())
            .system(
                "Today is "
                    + java.time.LocalDate.now()
                    + ". Default time is 12:00. Default star rating is 3. "
                    + "Always call the search tool with all required parameters.")
            .user(query)
            .call()
            .content();

    try {
      return java.util.Arrays.asList(
          new tools.jackson.databind.json.JsonMapper().readValue(content, Restaurant[].class));
    } catch (Exception e) {
      // If the LLM returned prose instead of JSON, wrap it in a single result
      return java.util.List.of(new Restaurant("Search Result", "various", 0, content));
    }
  }
}
