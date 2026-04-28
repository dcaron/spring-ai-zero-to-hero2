package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {
  @Value("${spring.ai.openai.api-key:notfound}")
  private String apiKey;

  @Value("${spring.ai.openai.base-url:notfound}")
  private String baseUrl;

  @Value("${spring.ai.openai.chat.options.deployment-name:notfound}")
  private String deploymentName;

  @GetMapping("/debug")
  public String getDebug() {
    return "You are running Azure OpenAI (Microsoft Foundry) Application with apiKey="
        + apiKey
        + ", base-url="
        + baseUrl
        + ", deployment-name="
        + deploymentName;
  }
}
