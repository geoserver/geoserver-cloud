package org.geoserver.cloud.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = GeoServerServletConfig.class)
public class ContextSmokeTest {

    @Test
    public void contextLoads() {}
}
