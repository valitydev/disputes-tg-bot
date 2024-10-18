package dev.vality.disputes.tg.bot.provider.service;

import lombok.Data;

import java.util.List;

@Data
public class DictionaryEntry {

    public List<String> include;
    public List<String> exclude;
}
