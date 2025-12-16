package dev.vality.disputes.tg.bot.service;

import dev.vality.disputes.tg.bot.config.model.ResponsePattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResponseParser {

    private final List<ResponsePattern> providerResponseDictionary;

    public Optional<ResponsePattern> findMatchingPattern(String response) {
        var lowerCaseResp = response.toLowerCase();
        return providerResponseDictionary.stream()
                .filter(responsePattern ->
                        lowerCaseResp.contains(responsePattern.getInclude().toLowerCase())
                                && !match(responsePattern.getExclude(), lowerCaseResp))
                .findFirst();
    }

    private boolean match(List<String> phrases, String response) {
        return phrases.stream().map(String::toLowerCase).anyMatch(response::contains);
    }
} 