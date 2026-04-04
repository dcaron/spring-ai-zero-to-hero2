package com.example.chat_04;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 1: Chat")
@TracedEndpoint
@RestController()
@RequestMapping("/chat/04")
public class StructuredOutputConverterController {
  private final ChatClient chatClient;
  private final Logger logger = LoggerFactory.getLogger(StructuredOutputConverterController.class);

  public StructuredOutputConverterController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @Operation(summary = "List plays as raw text")
  @ApiResponse(
      responseCode = "200",
      description = "Plays as raw text",
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
        .user(
            u ->
                u.text(
                        """
                Provide a list of the plays written by {author}.
                Provide only the list no other commentary.
                """)
                    .param("author", topic))
        .call()
        .content();
  }

  @Operation(
      summary = "List plays as JSON array",
      description = "Structured output: response parsed to List<String>")
  @ApiResponse(
      responseCode = "200",
      description = "Plays as JSON array",
      content =
          @Content(
              examples =
                  @ExampleObject(value = "[\"Hamlet\", \"Macbeth\", \"Othello\", \"King Lear\"]")))
  @GetMapping("/plays/list")
  public List<String> getPlaysList(
      @Parameter(description = "Playwright author name", example = "Shakespeare")
          @RequestParam(value = "author", defaultValue = "Shakespeare")
          String topic) {

    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                Provide a list of the plays written by {author}.
                Provide only the list no other commentary.
                """)
                    .param("author", topic))
        .call()
        .entity(new ListOutputConverter(new DefaultConversionService()));
  }

  @Operation(
      summary = "List plays as JSON object",
      description = "Structured output: response parsed to Map<String, Object>")
  @ApiResponse(
      responseCode = "200",
      description = "Playwright info as JSON object",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "{\"author\": \"William Shakespeare\", \"birthYear\": 1564, \"plays\": [\"Hamlet\", \"Macbeth\"]}")))
  @GetMapping("/plays/map")
  public Map<String, Object> getPlaysMap(
      @Parameter(description = "Playwright author name", example = "Shakespeare")
          @RequestParam(value = "author", defaultValue = "Shakespeare")
          String topic) {

    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                Provide a JSON object about the playwright {author} with the following keys:
                - "author": the author's full name
                - "birthYear": year of birth as a number
                - "plays": an array of play titles (strings)
                Return ONLY valid JSON, no other text or commentary.
                """)
                    .param("author", topic))
        .call()
        .entity(new MapOutputConverter());
  }

  @Operation(
      summary = "List plays as typed objects",
      description = "Structured output: response parsed to Play[] Java records")
  @ApiResponse(
      responseCode = "200",
      description = "Plays as typed Java record array",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "[{\"author\": \"Shakespeare\", \"title\": \"Hamlet\", \"publicationYear\": 1603}]")))
  @GetMapping("/plays/object")
  public Play[] getPlaysObject(
      @Parameter(description = "Playwright author name", example = "Shakespeare")
          @RequestParam(value = "author", defaultValue = "Shakespeare")
          String topic) {

    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                Provide a list of the plays written by {author}.
                Provide only the list no other commentary.
                """)
                    .param("author", topic))
        .call()
        .entity(Play[].class);
  }
}

record Play(String author, String title, Integer publicationYear) {}
