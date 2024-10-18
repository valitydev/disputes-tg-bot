package dev.vality.disputes.tg.bot.provider.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponseParser {

    private final List<DictionaryEntry> providerResponseDictionary;
    private final List<DictionaryEntry> supportResponseDictionary;

    public boolean isApprovedBySupportResponse(String response) {
        return supportResponseDictionary.stream()
                .anyMatch(dictionaryEntry -> matchAnyPhrase(dictionaryEntry.getInclude(), response)
                        && !matchAnyPhrase(dictionaryEntry.getExclude(), response));
    }

    private boolean matchAnyPhrase(List<String> include, String response) {
        return include.stream().anyMatch(includeEntry -> includeEntry.equals(response));
    }
}
