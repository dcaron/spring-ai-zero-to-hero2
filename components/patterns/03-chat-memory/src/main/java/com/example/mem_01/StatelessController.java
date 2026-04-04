package com.example.mem_01;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 4: Patterns")
@TracedEndpoint
@RestController
@RequestMapping("/mem/01")
public class StatelessController {

  private final ChatClient chatClient;

  @Autowired
  public StatelessController(ChatModel chatModel, ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @Operation(
      summary = "Send message (stateless)",
      description = "Stateless chat: no memory between requests")
  @ApiResponse(
      responseCode = "200",
      description = "AI response to the message",
      content =
          @Content(
              examples = @ExampleObject(value = "Hello John! The capital of France is Paris.")))
  @GetMapping("/hello")
  public String query(
      @Parameter(
              description = "Message to send to the AI",
              example = "Hello my name is John, what is the capital of France?")
          @RequestParam(
              value = "message",
              defaultValue = "Hello my name is John, what is the capital of France?")
          String message) {

    return this.chatClient.prompt().user(message).call().content();
  }

  @Operation(
      summary = "Ask name (stateless)",
      description = "Asks 'What is my name?' — fails because no chat memory")
  @ApiResponse(
      responseCode = "200",
      description = "AI response — will not know the user's name",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "I'm sorry, I don't have access to your name. Could you please tell me?")))
  @GetMapping("/name")
  public String name() {
    return this.chatClient.prompt().user("What is my name?").call().content();
  }
}
