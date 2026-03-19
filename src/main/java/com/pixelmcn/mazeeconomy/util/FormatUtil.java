package com.pixelmcn.mazeeconomy.util;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class FormatUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private FormatUtil() {}

    // ── Formatting ────────────────────────────────────────────────────────────

    public static String formatLocal(MazeEconomy plugin, double amount) {
        int decimals = plugin.getConfigManager().getLocalFormatDecimals();
        String symbol = plugin.getConfigManager().getLocalCurrencySymbol();
        return symbol + formatAmount(amount, decimals);
    }

    public static String formatGlobal(MazeEconomy plugin, GlobalCurrencyType currency, double amount) {
        return switch (currency) {
            case MAZECOINS -> {
                String sym = plugin.getConfigManager().getMazecoinSymbol();
                int dec    = plugin.getConfigManager().getMazecoinFormatDecimals();
                yield sym + formatAmount(amount, dec);
            }
            case SHARDS -> {
                String sym = plugin.getConfigManager().getShardSymbol();
                int dec    = plugin.getConfigManager().getShardFormatDecimals();
                yield sym + formatAmount(amount, dec);
            }
        };
    }

    public static String formatAmount(double amount, int decimals) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimals));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(amount);
    }

    // ── Messaging ─────────────────────────────────────────────────────────────

    /**
     * Send a MiniMessage-formatted message with the plugin prefix prepended.
     */
    public static void send(MazeEconomy plugin, CommandSender sender, String miniMessageText,
                            TagResolver... resolvers) {
        String prefix = plugin.getConfigManager().getPrefix();
        Component msg = MM.deserialize(prefix + miniMessageText, resolvers);
        sender.sendMessage(msg);
    }

    /**
     * Send a raw MiniMessage message (no prefix).
     */
    public static void sendRaw(CommandSender sender, String miniMessageText, TagResolver... resolvers) {
        sender.sendMessage(MM.deserialize(miniMessageText, resolvers));
    }

    /**
     * Fetch a message from config, replace {key} style placeholders, then send with prefix.
     *
     * Config messages use {placeholder} syntax (e.g. {amount}, {player}).
     * We convert these to MiniMessage <placeholder> tags before parsing,
     * then resolve them with the provided TagResolvers.
     */
    public static void sendConfigMessage(MazeEconomy plugin, CommandSender sender,
                                         String path, TagResolver... resolvers) {
        String raw = plugin.getConfigManager().getMessage(path);
        // Convert {placeholder} → <placeholder> so MiniMessage can resolve them
        String converted = raw.replaceAll("\\{(\\w+)}", "<$1>");
        send(plugin, sender, converted, resolvers);
    }

    // ── Common TagResolvers ───────────────────────────────────────────────────

    public static TagResolver amount(double value, int decimals) {
        return Placeholder.unparsed("amount", formatAmount(value, decimals));
    }

    public static TagResolver symbol(String sym) {
        return Placeholder.unparsed("symbol", sym);
    }

    public static TagResolver currency(String name) {
        return Placeholder.unparsed("currency", name);
    }

    public static TagResolver player(String name) {
        return Placeholder.unparsed("player", name);
    }

    public static TagResolver rank(int r) {
        return Placeholder.unparsed("rank", String.valueOf(r));
    }

    public static TagResolver page(int p) {
        return Placeholder.unparsed("page", String.valueOf(p));
    }

    public static TagResolver maxPage(int m) {
        return Placeholder.unparsed("max", String.valueOf(m));
    }
}
