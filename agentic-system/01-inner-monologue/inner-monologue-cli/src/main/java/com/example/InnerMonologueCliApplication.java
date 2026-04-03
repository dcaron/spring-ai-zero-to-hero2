package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.core.command.annotation.EnableCommand;

@EnableCommand
@SpringBootApplication
public class InnerMonologueCliApplication {
  public static void main(String[] args) {
    SpringApplication.run(InnerMonologueCliApplication.class, args);
  }
}
