package report.butt.mediamanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "mediamanager.bootstrap.username=test",
            "mediamanager.bootstrap.password=test",
            "mediamanager.validate-required-config=false"
        })
class MediamanagerApplicationTests {

    @Test
    void contextLoads() {}
}
