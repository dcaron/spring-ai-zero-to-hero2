package com.example.stuff_01;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stage 4: Patterns")
@TracedEndpoint
@RestController
@RequestMapping("/stuffit/01")
public class StuffController {

  private final ChatModel chatModel;

  @Value("classpath:/docs/wikipedia-curling.md")
  private Resource docsToStuffResource;

  @Value("classpath:/prompts/qa-prompt.st")
  private Resource qaPromptResource;

  @Autowired
  public StuffController(ChatModel chatModel) {
    this.chatModel = chatModel;
  }

  @Operation(
      summary = "Query with context stuffing",
      description = "Stuff-the-prompt: injects context data into the prompt")
  @ApiResponse(
      responseCode = "200",
      description = "AI-generated answer using the stuffed context",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "The mixed doubles gold medal in curling at the 2022 Winter Olympics was won by...")))
  @GetMapping("/query")
  public String query(
      @Parameter(
              description = "Question to answer using the stuffed context",
              example =
                  "Which athletes won the mixed doubles gold medal in curling at the 2022 Winter Olympics?")
          @RequestParam(
              value = "message",
              defaultValue =
                  "Which athletes won the mixed doubles gold medal in curling at the 2022 Winter Olympics?'")
          String message) {

    PromptTemplate promptTemplate = new PromptTemplate(qaPromptResource);
    Map<String, Object> map = new HashMap<>();
    map.put("question", message);
    map.put("context", docsToStuffResource);

    Prompt prompt = promptTemplate.create(map);
    return chatModel.call(prompt).getResult().getOutput().getText();
  }
}
