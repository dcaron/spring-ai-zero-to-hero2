package com.example.agent.cot;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 5: Agents")
@TracedEndpoint
@RestController
@RequestMapping("/cot/bio/")
public class ChainOfThoughtController {

  private final ChatClient chatClient;

  @Value("classpath:/info/Profile.pdf")
  private Resource profile;

  private ChainOfThoughtBioWriterAgent bioWriterAgent;

  @Autowired
  public ChainOfThoughtController(
      ChatModel chatModel,
      ChatClient.Builder builder,
      ChainOfThoughtBioWriterAgent bioWriterAgent) {
    this.chatClient = builder.build();
    this.bioWriterAgent = bioWriterAgent;
  }

  @Operation(
      summary = "Generate bio (single pass)",
      description = "Single-pass bio generation from Profile.pdf")
  @ApiResponse(
      responseCode = "200",
      description = "Generated biography with character and word count",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "Jane Doe is a seasoned software engineer...\n\n-------\n\nCharacters: 342 Words: 58")))
  @GetMapping("/oneshot")
  public String oneShot() {

    LinkedProfile profile = new LinkedProfile(this.profile);
    String bio =
        this.chatClient
            .prompt()
            .user(
                userSpec ->
                    userSpec
                        .text(
                            """
      Write a one paragraph professional biography suitable for conference presentation based on the content below

      {profile}
       """)
                        .param("profile", profile.getProfileAsString()))
            .call()
            .content();

    String result =
        bio
            + "\n\n-------\n\n"
            + "Characters: %s ".formatted(bio.length())
            + "Words: %s".formatted(bio.split("\\s+").length);

    return result;
  }

  @Operation(
      summary = "Generate bio (chain of thought)",
      description = "Multi-step: outline → draft → refine → polish. ~10s with Ollama.")
  @ApiResponse(
      responseCode = "200",
      description = "All intermediate steps and final biography separated by dividers",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "\n\n-------\n\nStep 1: Outline...\n\n-------\n\nFinal bio...\n\n-------\n\nCharacters: 342 Words: 58")))
  @GetMapping("/flow")
  public String agenticFlow() {
    LinkedProfile profile = new LinkedProfile(this.profile);
    List<String> stepResults = this.bioWriterAgent.writeBio(profile.getProfileAsString());
    String result = stepResults.stream().map(i -> "\n\n-------\n\n" + i).reduce("", String::concat);

    String bio = stepResults.get(stepResults.size() - 1);
    result +=
        "\n\n-------\n\n"
            + "Characters: %s ".formatted(bio.length())
            + "Words: %s".formatted(bio.split("\\s+").length);
    return result;
  }
}
