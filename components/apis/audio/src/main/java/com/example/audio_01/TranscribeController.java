package com.example.audio_01;

import com.example.tracing.TracedEndpoint;
// Spring AI 2.0.0-M6: response-format enum moved from spring-ai's internal API
// to the official openai-java SDK (com.openai.models.audio.AudioResponseFormat).
import com.openai.models.audio.AudioResponseFormat;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

@TracedEndpoint
@RestController
@RequestMapping("/audio/01")
public class TranscribeController {

  private final OpenAiAudioTranscriptionModel transcriptionModel;

  @Value("classpath:/data/The_Astronomer_Vermeer.ogg")
  private Resource audioResource;

  public TranscribeController(OpenAiAudioTranscriptionModel transcriptionModel) {
    this.transcriptionModel = transcriptionModel;
  }

  @GetMapping("text")
  public String transcribe() {

    var transcriptionOptions =
        OpenAiAudioTranscriptionOptions.builder()
            .responseFormat(AudioResponseFormat.TEXT)
            .temperature(0f)
            .build();

    AudioTranscriptionPrompt transcriptionRequest =
        new AudioTranscriptionPrompt(audioResource, transcriptionOptions);
    AudioTranscriptionResponse response = this.transcriptionModel.call(transcriptionRequest);

    return response.getResult().getOutput();
  }
}
