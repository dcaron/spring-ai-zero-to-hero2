package com.example.agent.reflection;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 5: Agents")
@TracedEndpoint
@RestController
@RequestMapping("/reflection/bio/")
public class ReflectionAgentController {

  private final ChatClient chatClient;
  private final SelfReflectionAgent selfReflectionAgent;

  @Value("classpath:/info/Profile.pdf")
  private Resource profile;

  @Autowired
  public ReflectionAgentController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
    this.selfReflectionAgent = new SelfReflectionAgent(builder);
  }

  @Operation(
      summary = "Generate bio (single pass)",
      description = "Single-pass bio generation baseline")
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
      summary = "Generate bio (self-reflection)",
      description = "Writer + Critic loop with configurable iterations")
  @ApiResponse(
      responseCode = "200",
      description = "All iteration bios with character and word counts per iteration",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "\n------- Iteration 1 -------\nJane Doe is...\n\nCharacters: 300\nWords: 50\n-----------------------------\n...")))
  @GetMapping("/agent")
  public String agent() {
    LinkedProfile profile = new LinkedProfile(this.profile);
    List<String> bios = selfReflectionAgent.write(profile.getProfileAsString(), 3);

    String result = "";
    for (int i = 0; i < bios.size(); i++) {
      String bio = bios.get(i);
      result +=
          """

            ------- Iteration %d -------
            %s

            Characters: %d
            Words: %d
           -----------------------------
            """
              .formatted(i + 1, bio, bio.length(), bio.split("\\s+").length);
    }

    return result;
  }
}
