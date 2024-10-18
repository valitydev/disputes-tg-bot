package dev.vality.disputes.tg.bot.common.config;

import dev.vality.disputes.tg.bot.common.config.properties.BotProperties;
import dev.vality.disputes.tg.bot.common.dao.ChatDao;
import dev.vality.disputes.tg.bot.common.handler.CommonHandler;
import dev.vality.disputes.tg.bot.common.service.DisputesBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;

@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public DisputesBot disputesBot(TelegramBotsApi telegramBotsApi, BotProperties botProperties,
                                   List<CommonHandler> handlers, ChatDao chatDao) throws TelegramApiException {
        var bot = new DisputesBot(botProperties, handlers, chatDao);
        bot.clearWebhook();
        telegramBotsApi.registerBot(bot);
        return bot;
    }
}
