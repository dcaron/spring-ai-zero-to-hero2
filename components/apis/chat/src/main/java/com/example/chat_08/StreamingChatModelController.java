package com.example.chat_08;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "Stage 1: Chat")
@TracedEndpoint
@RestController
@RequestMapping("/chat/08")
public class StreamingChatModelController {
  private final StreamingChatModel chatModel;

  public StreamingChatModelController(StreamingChatModel chatModel) {
    this.chatModel = chatModel;
  }

  @Operation(
      summary = "Stream an essay",
      description =
          "Streaming: returns Flux<String> as server-sent events. The AI generates an essay token by token.")
  @ApiResponse(
      responseCode = "200",
      description = "Streamed essay tokens as server-sent events",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "Artificial intelligence is transforming society in profound ways...")))
  @GetMapping("/essay")
  public Flux<String> getJoke(
      @Parameter(description = "Topic for the essay", example = "Impact of AI on Society")
          @RequestParam(value = "topic", defaultValue = "Impact of AI on Society")
          String topic) {

    var promptTemplate = new PromptTemplate("Write an essay about {topic} ");
    promptTemplate.add("topic", topic);

    return chatModel.stream(promptTemplate.render());
  }
}
