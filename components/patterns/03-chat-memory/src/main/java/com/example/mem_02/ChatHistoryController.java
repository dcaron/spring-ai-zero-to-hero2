package com.example.mem_02;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 4: Patterns")
@TracedEndpoint
@RestController
@RequestMapping("/mem/02")
public class ChatHistoryController {

  private final ChatClient chatClient;
  private final PromptChatMemoryAdvisor promptChatMemoryAdvisor;

  @Autowired
  public ChatHistoryController(ChatClient.Builder builder) {
    this.chatClient = builder.build();

    var memory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
    this.promptChatMemoryAdvisor = PromptChatMemoryAdvisor.builder(memory).build();
  }

  @Operation(
      summary = "Send message (with memory)",
      description = "Chat with memory: MessageChatMemoryAdvisor stores history")
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

    return this.chatClient
        .prompt()
        .advisors(promptChatMemoryAdvisor)
        .user(message)
        .call()
        .content();
  }

  @Operation(
      summary = "Ask name (with memory)",
      description = "Asks 'What is my name?' — succeeds because memory retains context")
  @ApiResponse(
      responseCode = "200",
      description = "AI response — correctly recalls the user's name from memory",
      content =
          @Content(
              examples = @ExampleObject(value = "Your name is John, as you mentioned earlier.")))
  @GetMapping("/name")
  public String name() {
    return this.chatClient
        .prompt()
        .advisors(promptChatMemoryAdvisor)
        .user("What is my name?")
        .call()
        .content();
  }
}
