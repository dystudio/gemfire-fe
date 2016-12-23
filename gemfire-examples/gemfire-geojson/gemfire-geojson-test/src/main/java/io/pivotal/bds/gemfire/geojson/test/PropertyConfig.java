package io.pivotal.bds.gemfire.geojson.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertyConfig {

    @Value("${locators:localhost[10334]}")
    public String locators;
    
}
