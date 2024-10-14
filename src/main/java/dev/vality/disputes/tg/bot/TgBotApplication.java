package dev.vality.disputes.tg.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ServletComponentScan
@SpringBootApplication
public class TgBotApplication extends SpringApplication {

    public static void main(String[] args) {
        SpringApplication.  run(TgBotApplication.class, args);
    }

}
