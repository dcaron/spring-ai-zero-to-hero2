/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.mcp.sample.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class MathTools {

  public MathTools() {}

  @Tool(description = "Adds two numbers. Example: sumNumbers(3, 5) returns 8.")
  public int sumNumbers(
      @ToolParam(description = "First integer addend, e.g. 3") int number1,
      @ToolParam(description = "Second integer addend, e.g. 5") int number2) {
    return number1 + number2;
  }

  @Tool(description = "Multiplies two numbers. Example: multiplyNumbers(4, 6) returns 24.")
  public int multiplyNumbers(
      @ToolParam(description = "First integer factor, e.g. 4") int number1,
      @ToolParam(description = "Second integer factor, e.g. 6") int number2) {
    return number1 * number2;
  }

  @Tool(description = "Divides two numbers. Example: divideNumbers(10, 4) returns 2.5.")
  public double divideNumbers(
      @ToolParam(description = "Dividend (number to divide), e.g. 10") double number1,
      @ToolParam(description = "Divisor (must be non-zero), e.g. 4") double number2) {
    return number1 / number2;
  }
}
