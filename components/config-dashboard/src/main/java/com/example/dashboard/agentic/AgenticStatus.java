package com.example.dashboard.agentic;

public record AgenticStatus(
    boolean up, String provider, String model, int agentCount, String startCommand) {}
