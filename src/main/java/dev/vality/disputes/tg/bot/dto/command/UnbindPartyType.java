package dev.vality.disputes.tg.bot.dto.command;

public enum UnbindPartyType {
    SPECIFIC_PARTY,
    ALL_PARTIES;

    public static UnbindPartyType fromString(String value) {
        if ("all".equalsIgnoreCase(value)) {
            return ALL_PARTIES;
        }
        return SPECIFIC_PARTY;
    }

    public boolean isAllParties() {
        return this == ALL_PARTIES;
    }
} 