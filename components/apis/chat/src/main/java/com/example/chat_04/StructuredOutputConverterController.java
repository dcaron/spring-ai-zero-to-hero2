package com.example.chat_04;

import com.example.tracing.TracedEndpoint;
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

@TracedEndpoint
@RestController()
@RequestMapping("/chat/04")
public class StructuredOutputConverterController {
  private final ChatClient chatClient;
  private final Logger logger = LoggerFactory.getLogger(StructuredOutputConverterController.class);

  public StructuredOutputConverterController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @GetMapping("/plays")
  public String getPlays(
      @RequestParam(value = "author", defaultValue = "Shakespeare") String topic) {

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

  @GetMapping("/plays/list")
  public List<String> getPlaysList(
      @RequestParam(value = "author", defaultValue = "Shakespeare") String topic) {

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

  @GetMapping("/plays/map")
  public Map<String, Object> getPlaysMap(
      @RequestParam(value = "author", defaultValue = "Shakespeare") String topic) {

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

  @GetMapping("/plays/object")
  public Play[] getPlaysObject(
      @RequestParam(value = "author", defaultValue = "Shakespeare") String topic) {

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
