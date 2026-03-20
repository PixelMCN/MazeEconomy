package com.pixelmcn.mazeeconomy.command.bank;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.gui.BankGui;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BankCommand implements CommandExecutor, TabCompleter {

    private final MazeEconomy plugin;
    private final BankGui bankGui;

    public BankCommand(MazeEconomy plugin) {
        this.plugin = plugin;
        this.bankGui = new BankGui(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (!plugin.getConfigManager().isLocalEconomyEnabled()) {
            FormatUtil.sendConfigMessage(plugin, sender, "local.feature-disabled");
            return true;
        }

        if (!(sender instanceof Player player)) {
            FormatUtil.sendConfigMessage(plugin, sender, "player-only");
            return true;
        }

        if (!player.hasPermission("mazeeconomy.bank")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        if (!plugin.getConfigManager().isBankEnabled()) {
            FormatUtil.sendRaw(sender, plugin.getConfigManager().getPrefix() +
                    "<#D97A7A>ᴛʜᴇ ʙᴀɴᴋ ɪs ᴅɪsᴀʙʟᴇᴅ ᴏɴ ᴛʜɪs sᴇʀᴠᴇʀ.</#D97A7A>");
            return true;
        }

        // /bank — open GUI
        if (args.length == 0) {
            bankGui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String symbol = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec = plugin.getConfigManager().getLocalFormatDecimals();

        switch (sub) {
            case "deposit", "dep" -> {
                if (args.length < 2) {
                    FormatUtil.sendRaw(sender, "<gray>Usage: /bank deposit <amount/all></gray>");
                    return true;
                }
                double amount = parseAmount(args[1], plugin.getLocalEconomyManager().getWallet(player.getUniqueId()));
                if (amount <= 0) {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.bank-invalid-amount");
                    return true;
                }
                EconomyResponse resp = plugin.getLocalEconomyManager().depositToBank(player.getUniqueId(),
                        player.getName(), amount);
                if (resp.isSuccess()) {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.bank-deposit-success",
                            FormatUtil.symbol(symbol), FormatUtil.amount(resp.getAmount(), dec));
                } else {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.bank-insufficient");
                }
            }
            case "withdraw", "with", "wd" -> {
                if (args.length < 2) {
                    FormatUtil.sendRaw(sender, "<gray>Usage: /bank withdraw <amount/all></gray>");
                    return true;
                }
                double amount = parseAmount(args[1], plugin.getLocalEconomyManager().getBank(player.getUniqueId()));
                if (amount <= 0) {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.bank-invalid-amount");
                    return true;
                }
                EconomyResponse resp = plugin.getLocalEconomyManager().withdrawFromBank(player.getUniqueId(),
                        player.getName(), amount);
                if (resp.isSuccess()) {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.bank-withdraw-success",
                            FormatUtil.symbol(symbol), FormatUtil.amount(resp.getAmount(), dec));
                } else {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.bank-insufficient");
                }
            }
            default -> bankGui.open(player);
        }
        return true;
    }

    private double parseAmount(String arg, double max) {
        if (arg.equalsIgnoreCase("all") || arg.equalsIgnoreCase("max"))
            return max;
        try {
            double v = Double.parseDouble(arg);
            return v > 0 ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l,
            @NotNull String[] args) {
        if (args.length == 1)
            return List.of("deposit", "withdraw").stream().filter(x -> x.startsWith(args[0].toLowerCase())).toList();
        if (args.length == 2)
            return List.of("100", "1000", "all");
        return List.of();
    }
}
