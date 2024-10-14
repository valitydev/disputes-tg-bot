package dev.vality.disputes.tg.bot;

import org.springframework.modulith.core.ApplicationModules;

class TgBotApplicationTest {

    public static void main(String[] args) {
        ApplicationModules.of(TgBotApplication.class).verify();
    }

}