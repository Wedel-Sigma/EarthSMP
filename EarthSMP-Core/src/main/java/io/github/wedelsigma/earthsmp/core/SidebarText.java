package io.github.wedelsigma.earthsmp.core;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Pure HUD formatting, independently unit-testable without a running Minecraft server. */
final class SidebarText {
    private SidebarText() {}

    static String homeLabel(int day, boolean inNation, int used) {
        if (!inNation || day == 0) return "-";
        if (day <= 2) return "∞";
        return Math.max(0, 4 - used) + "/4";
    }

    static String nationLabel(NationDatabase.Membership member) {
        if (member == null) return "Brak panstwa";
        String name = member.nationName();
        int characters = name.codePointCount(0, name.length());
        return characters > 19 ? name.substring(0, name.offsetByCodePoints(0, 18)) + "…" : name;
    }

    static List<Component> lines(int day, NationDatabase.Membership member,
                                  NationDatabase.NationRank rank, int online,
                                  boolean war, int used) {
        boolean inNation = member != null;
        int members = rank == null ? 0 : rank.members();
        int cores = rank == null ? 0 : rank.placedCores();
        return List.of(
                Component.text("Dzien: " + (day == 0 ? "nie rozpoczeto" : day), NamedTextColor.AQUA),
                Component.text("Panstwo:", NamedTextColor.GRAY),
                Component.text(nationLabel(member), inNation ? NamedTextColor.GOLD : NamedTextColor.YELLOW),
                Component.text("Rdzenie: " + cores, NamedTextColor.WHITE),
                Component.text("Online: " + (inNation ? online + "/" + members : "-"), NamedTextColor.WHITE),
                Component.text("Status: " + (!inNation ? "-" : war ? "Wojna" : "Pokoj"),
                        war ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("/home: " + homeLabel(day, inNation, used), NamedTextColor.AQUA),
                Component.text("Reset: 06:00 PL", NamedTextColor.DARK_GRAY)
        );
    }
}
