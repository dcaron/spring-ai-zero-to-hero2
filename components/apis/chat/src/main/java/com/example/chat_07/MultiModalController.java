package com.example.chat_07;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 1: Chat")
@TracedEndpoint
@RestController
@RequestMapping("/chat/07")
public class MultiModalController {
  private final ChatClient chatClient;

  @Value("classpath:/multimodal.test.png")
  private Resource image;

  @Value("${spring.ai.ollama.chat.model:#{null}}")
  private String ollamaModel;

  public MultiModalController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @Operation(
      summary = "Describe an image",
      description =
          "Multimodal: sends an image + text prompt, AI returns a description. Auto-switches to llava model on Ollama.")
  @ApiResponse(
      responseCode = "200",
      description = "AI-generated description of the image",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "The image shows a wire fruit basket containing bananas and red apples, placed on what appears to be a kitchen counter.")))
  @GetMapping("/explain")
  public String explain() throws IOException {

    var prompt = chatClient.prompt();

    // Use llava for Ollama multimodal — llama3.2 has no vision capability
    if (ollamaModel != null) {
      // Spring AI 2.0.0-M6: options() takes a ChatOptions.Builder, not a built ChatOptions
      prompt = prompt.options(ChatOptions.builder().model("llava"));
    }

    return prompt
        .user(
            u ->
                u.text("Explain what do you see in this picture?")
                    .media(MimeTypeUtils.IMAGE_PNG, image))
        .call()
        .content();
  }
}
