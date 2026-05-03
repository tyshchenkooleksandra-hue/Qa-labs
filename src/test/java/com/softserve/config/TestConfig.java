package com.softserve.config;

import com.softserve.service.impl.ScheduleCacheService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestConfiguration
public class TestConfig {
    @MockBean
    ScheduleCacheService cacheService;
}
