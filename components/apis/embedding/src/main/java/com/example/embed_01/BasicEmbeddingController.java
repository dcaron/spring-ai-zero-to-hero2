package com.example.embed_01;

import com.example.tracing.TracedEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Stage 2: Embeddings")
@TracedEndpoint
@RestController
@RequestMapping("/embed/01")
public class BasicEmbeddingController {

  private final EmbeddingModel embeddingModel;

  @Value("${spring.ai.ollama.embedding.model:#{null}}")
  private String ollamaModel;

  @Value("${spring.ai.openai.embedding.options.model:#{null}}")
  private String openaiModel;

  @Value("${spring.ai.azure.openai.embedding.options.model:#{null}}")
  private String azureModel;

  public BasicEmbeddingController(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  @Operation(
      summary = "Generate embedding vector",
      description = "Converts text into a float[] embedding vector")
  @ApiResponse(
      responseCode = "200",
      description = "Embedding vector as float array",
      content = @Content(examples = @ExampleObject(value = "[0.1, -0.2, 0.05, ...]")))
  @GetMapping("text")
  public float[] getEmbedding(
      @Parameter(description = "Text to convert to embedding vector", example = "Hello World")
          @RequestParam(value = "text", defaultValue = "Hello World")
          String text) {
    return embeddingModel.embed(text);
  }

  @Operation(
      summary = "Get embedding dimensions",
      description = "Returns the embedding provider, model name, and vector dimensions")
  @ApiResponse(
      responseCode = "200",
      description = "Provider, model, and dimension info",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      value =
                          "{\"provider\": \"Ollama\", \"model\": \"nomic-embed-text\", \"dimension\": 768}")))
  @GetMapping("dimension")
  public Map<String, Object> getDimension() {
    String provider = embeddingModel.getClass().getSimpleName().replace("EmbeddingModel", "");
    String model = resolveModelName();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("provider", provider);
    result.put("model", model);
    result.put("dimension", embeddingModel.dimensions());
    return result;
  }

  private String resolveModelName() {
    if (ollamaModel != null) return ollamaModel;
    if (openaiModel != null) return openaiModel;
    if (azureModel != null) return azureModel;
    return "default";
  }
}
