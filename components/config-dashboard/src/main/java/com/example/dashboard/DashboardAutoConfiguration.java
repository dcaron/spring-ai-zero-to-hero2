package com.example.dashboard;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("ui")
@ComponentScan(basePackageClasses = DashboardAutoConfiguration.class)
public class DashboardAutoConfiguration {}
