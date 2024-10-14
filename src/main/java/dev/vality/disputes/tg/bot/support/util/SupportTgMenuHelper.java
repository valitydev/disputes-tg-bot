package dev.vality.disputes.tg.bot.support.util;

import dev.vality.disputes.tg.bot.core.util.TelegramUtil;
import dev.vality.disputes.tg.bot.support.exception.TelegramFormatException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class SupportTgMenuHelper {

    public static String FLAG_DELIMITER = "_";

    public static void addBindButton(List<List<InlineKeyboardButton>> buttons, String context) {
        addButton(buttons, SupportMenuButton.BIND, context);
    }

    private static void addButton(List<List<InlineKeyboardButton>> buttons,
                                  SupportMenuButton buttonToAdd,
                                  String callbackData) {
        if (!TelegramUtil.isCallbackContextSizeValid(callbackData)) {
            throw new TelegramFormatException("Callback context size exceeds max permitted size!");
        }
        buttons.add(Collections.singletonList(InlineKeyboardButton.builder()
                .text(buttonToAdd.getText())
                .callbackData(callbackData)
                .build()));
    }

    public static void addFailButton(List<List<InlineKeyboardButton>> buttons, String context) {
        addButton(buttons, SupportMenuButton.FAIL, context);
    }

    public static SupportMenuButton getPressedButton(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        int flag = Integer.parseInt(data.substring(0, data.indexOf(FLAG_DELIMITER)));
        return Arrays.stream(SupportMenuButton.values())
                .filter(button -> button.getFlag() == flag)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    @Getter
    @RequiredArgsConstructor
    public enum SupportMenuButton {

        BIND("Привязать ID диспута", 0),
        FAIL("Перевести в Fail", 1);

        private final String text;
        private final int flag;

    }
}
