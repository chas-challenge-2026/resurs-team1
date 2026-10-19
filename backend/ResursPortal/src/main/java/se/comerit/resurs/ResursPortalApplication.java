package se.comerit.resurs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@ConfigurationPropertiesScan
@Controller
public class ResursPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResursPortalApplication.class, args);
    }

    @GetMapping(value = "/**/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
