package com.example.chat_03;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 1: Chat")
@TracedEndpoint
@RestController()
@RequestMapping("/chat/03")
public class PromptTemplateController {
  private final ChatClient chatClient;

  public PromptTemplateController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @Operation(
      summary = "Generate a joke with template variables",
      description = "Prompt templates with {variables} for dynamic content")
  @ApiResponse(
      responseCode = "200",
      description = "Generated joke text",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "Why did Spring Boot go to therapy? Because it had too many dependency issues!")))
  @GetMapping("/joke")
  public String getJoke(
      @Parameter(description = "Topic for the joke", example = "cows")
          @RequestParam(value = "topic", defaultValue = "cows")
          String topic) {

    // Prompt template enables us to safely inject user input into the prompt
    // text in {} is replaced by the value of the variable with the same name.
    // PromptTemplate is a commonly used class with Spring AI
    return chatClient
        .prompt()
        .user(u -> u.text("Tell me a joke about {topic}").param("topic", topic))
        .call()
        .content();
  }

  @Operation(summary = "List plays by author", description = "Prompt template with author variable")
  @ApiResponse(
      responseCode = "200",
      description = "List of plays as text",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value = "Hamlet, Macbeth, Othello, King Lear, A Midsummer Night's Dream")))
  @GetMapping("/plays")
  public String getPlays(
      @Parameter(description = "Playwright author name", example = "Shakespeare")
          @RequestParam(value = "author", defaultValue = "Shakespeare")
          String topic) {

    return chatClient
        .prompt()
        .user(u -> u.text(new ClassPathResource("prompts/plays.st")).param("author", topic))
        .call()
        .content();
  }
}
