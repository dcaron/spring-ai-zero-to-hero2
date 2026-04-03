package com.example.chat_07;

import com.example.tracing.TracedEndpoint;
import java.io.IOException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  @GetMapping("/explain")
  public String explain() throws IOException {

    var prompt = chatClient.prompt();

    // Use llava for Ollama multimodal — llama3.2 has no vision capability
    if (ollamaModel != null) {
      prompt = prompt.options(ChatOptions.builder().model("llava").build());
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
