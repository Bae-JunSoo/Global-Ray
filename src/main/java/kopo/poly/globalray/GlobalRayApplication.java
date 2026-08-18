package kopo.poly.globalray;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GlobalRayApplication {
    public static void main(String[] args) {

        SpringApplication.run(GlobalRayApplication.class, args);

    }
}