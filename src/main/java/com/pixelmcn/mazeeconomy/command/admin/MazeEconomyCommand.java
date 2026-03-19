package com.pixelmcn.mazeeconomy.command.admin;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MazeEconomyCommand implements CommandExecutor, TabCompleter {

    private final MazeEconomy plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MazeEconomyCommand(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("mazeeconomy.admin.manage")) {
                    sender.sendMessage(mm.deserialize("<#D97A7A>ɴᴏ ᴘᴇʀᴍɪssɪᴏɴ</#D97A7A>"));
                    return true;
                }
                plugin.getConfigManager().reload();
                sender.sendMessage(mm.deserialize("<#7FB77E>ᴄᴏɴғɪɢ ʀᴇʟᴏᴀᴅᴇᴅ</#7FB77E>"));
            }

            case "info" -> sendInfo(sender);

            default -> sendHelp(sender);
        }

        return true;
    }

    // ─────────────────────────────────────────────
    // HELP MENU
    // ─────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        var cfg = plugin.getConfigManager();

        boolean global = plugin.isGlobalEconomyActive();
        boolean bank = cfg.isBankEnabled();
        boolean admin = sender.hasPermission("mazeeconomy.admin.eco");

        String sym = cfg.getLocalCurrencySymbol();
        String cur = cfg.getLocalCurrencyNamePlural();

        header(sender, "Command Help");

        // LOCAL
        section(sender, "ʟᴏᴄᴀʟ ᴇᴄᴏɴᴏᴍʏ", "(" + cur + " — " + cfg.getServerId() + ")");
        cmd(sender, "/balance [player]", "ᴠɪᴇᴡ ʙᴀʟᴀɴᴄᴇ");
        cmd(sender, "/pay <player> <amount>", "sᴇɴᴅ " + sym);
        cmd(sender, "/baltop [page]", "ᴛᴏᴘ ᴘʟᴀʏᴇʀs");

        // BANK
        if (bank) {
            line(sender, "");
            section(sender, "ʙᴀɴᴋ", "(ɪɴᴛᴇʀᴇsᴛ: " + cfg.getBankInterestRate() + "%)");
            cmd(sender, "/bank", "ᴏᴘᴇɴ ɢᴜɪ");
            cmd(sender, "/bank deposit <amount/all>", "ᴅᴇᴘᴏsɪᴛ");
            cmd(sender, "/bank withdraw <amount/all>", "ᴡɪᴛʜᴅʀᴀᴡ");
        }

        line(sender, "");

        // GLOBAL
        if (global) {
            section(sender, "ɢʟᴏʙᴀʟ ᴇᴄᴏɴᴏᴍʏ", "(sʏɴᴄᴇᴅ)");
            cmd(sender, "/mazecoins [player]", "ᴠɪᴇᴡ ʙᴀʟᴀɴᴄᴇ");
            cmd(sender, "/shards [player]", "ᴠɪᴇᴡ ʙᴀʟᴀɴᴄᴇ");
        } else {
            line(sender, "<#D97A7A>ɢʟᴏʙᴀʟ ᴅɪsᴀʙʟᴇᴅ</#D97A7A>");
        }

        // ADMIN
        if (admin) {
            line(sender, "");
            section(sender, "ᴀᴅᴍɪɴ", "");

            cmd(sender, "/eco set local <player> <amount>", "sᴇᴛ ʙᴀʟᴀɴᴄᴇ");
            cmd(sender, "/eco add local <player> <amount>", "ᴀᴅᴅ ʙᴀʟᴀɴᴄᴇ");
            cmd(sender, "/eco remove local <player> <amount>", "ʀᴇᴍᴏᴠᴇ ʙᴀʟᴀɴᴄᴇ");

            if (global) {
                cmd(sender, "/eco set mazecoins <player> <amount>", "sᴇᴛ ᴍᴀᴢᴇᴄᴏɪɴs");
                cmd(sender, "/eco add mazecoins <player> <amount>", "ᴀᴅᴅ ᴍᴀᴢᴇᴄᴏɪɴs");
                cmd(sender, "/eco set shards <player> <amount>", "sᴇᴛ sʜᴀʀᴅs");
                cmd(sender, "/eco add shards <player> <amount>", "ᴀᴅᴅ sʜᴀʀᴅs");
            }

            cmd(sender, "/mazeeconomy reload", "ʀᴇʟᴏᴀᴅ ᴄᴏɴғɪɢ");
            cmd(sender, "/mazeeconomy info", "ᴘʟᴜɢɪɴ sᴛᴀᴛᴜs");
        }

        line(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ─────────────────────────────────────────────
    // INFO PANEL
    // ─────────────────────────────────────────────

    private void sendInfo(CommandSender sender) {
        var cfg = plugin.getConfigManager();

        header(sender, "Plugin Info");

        line(sender,
                "  <white>MazeEconomy</white> <dark_gray>v" + plugin.getDescription().getVersion() + "</dark_gray>");

        line(sender, "  <gray>sᴇʀᴠᴇʀ</gray> <dark_gray>»</dark_gray> <white>" + cfg.getServerId() + "</white>");
        line(sender, "  <gray>ᴄᴜʀʀᴇɴᴄʏ</gray> <dark_gray>»</dark_gray> <white>" +
                cfg.getLocalCurrencyNamePlural() + " (" + cfg.getLocalCurrencySymbol() + ")</white>");

        line(sender, "  <gray>ᴠᴀᴜʟᴛ</gray> <dark_gray>»</dark_gray> " +
                ok(plugin.getVaultHook() != null));

        line(sender, "  <gray>ʙᴀɴᴋ</gray> <dark_gray>»</dark_gray> " +
                ok(cfg.isBankEnabled()));

        line(sender, "  <gray>ᴅᴇᴀᴛʜ</gray> <dark_gray>»</dark_gray> " +
                ok(cfg.isDeathPenaltyEnabled()));

        line(sender, "  <gray>ᴘᴠᴘ</gray> <dark_gray>»</dark_gray> " +
                ok(cfg.isPvpPenaltyEnabled()));

        line(sender, "  <gray>ɢʟᴏʙᴀʟ</gray> <dark_gray>»</dark_gray> " +
                ok(plugin.isGlobalEconomyActive()));

        line(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private void line(CommandSender sender, String msg) {
        sender.sendMessage(mm.deserialize("<gray>" + msg + "</gray>"));
    }

    private void header(CommandSender sender, String title) {
        sender.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(mm.deserialize(
                "  <gray>[<#C9A55A><b>ᴍᴀᴢᴇᴇᴄᴏɴᴏᴍʏ</b></#C9A55A>]</gray> <dark_gray>»</dark_gray> <white>" + title
                        + "</white>"));
        sender.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void section(CommandSender sender, String title, String meta) {
        sender.sendMessage(mm.deserialize(
                "  <#C9A55A>" + title + "</#C9A55A> <dark_gray>" + meta + "</dark_gray>"));
    }

    private void cmd(CommandSender sender, String command, String description) {
        sender.sendMessage(mm.deserialize(
                "  <dark_gray>›</dark_gray> <#C9A55A>" + command + "</#C9A55A> <dark_gray>—</dark_gray> <gray>"
                        + description + "</gray>"));
    }

    private String ok(boolean state) {
        return state
                ? "<#7FB77E>✔</#7FB77E>"
                : "<#D97A7A>✘</#D97A7A>";
    }

    // ─────────────────────────────────────────────
    // TAB COMPLETE
    // ─────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {

        if (args.length == 1) {
            return List.of("help", "reload", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}