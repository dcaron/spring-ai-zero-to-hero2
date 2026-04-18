package com.example.dashboard.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgenticDemoCatalogTest {

  AgenticDemoCatalog catalog = new AgenticDemoCatalog();

  @Test
  void catalogExposesExactlyTwoDemos() {
    List<AgenticDemo> demos = catalog.all();
    assertThat(demos).hasSize(2);
    assertThat(demos).extracting(AgenticDemo::id).containsExactly("01", "02");
  }

  @Test
  void demo01IsInnerMonologueOnPort8091() {
    AgenticDemo d = catalog.get("01");
    assertThat(d.port()).isEqualTo(8091);
    assertThat(d.basePath()).isEqualTo("/agents/inner-monologue");
    assertThat(d.supportsLogin()).isFalse();
    assertThat(d.traceKind()).isEqualTo(AgenticDemo.TraceKind.SINGLE);
  }

  @Test
  void demo02IsModelDirectedLoopOnPort8092WithLoginAndMultiStep() {
    AgenticDemo d = catalog.get("02");
    assertThat(d.port()).isEqualTo(8092);
    assertThat(d.basePath()).isEqualTo("/agents/model-directed-loop");
    assertThat(d.supportsLogin()).isTrue();
    assertThat(d.traceKind()).isEqualTo(AgenticDemo.TraceKind.MULTI_STEP);
  }

  @Test
  void unknownIdThrows() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> catalog.get("99"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
