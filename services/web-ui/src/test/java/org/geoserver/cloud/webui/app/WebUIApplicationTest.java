package org.geoserver.cloud.webui.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test") // see bootstrap-test.yml
public class WebUIApplicationTest {

    @Test
    public void contextLoads() {}
}
