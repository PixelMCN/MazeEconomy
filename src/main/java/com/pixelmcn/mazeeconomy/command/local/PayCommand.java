package com.pixelmcn.mazeeconomy.command.local;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final MazeEconomy plugin;

    public PayCommand(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (!plugin.getConfigManager().isLocalEconomyEnabled()) {
            FormatUtil.sendConfigMessage(plugin, sender, "local.feature-disabled");
            return true;
        }

        if (!(sender instanceof Player payer)) {
            FormatUtil.sendConfigMessage(plugin, sender, "player-only");
            return true;
        }

        if (!payer.hasPermission("mazeeconomy.pay")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            FormatUtil.sendRaw(sender, "<red>Usage: /pay <player> <amount></red>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            FormatUtil.sendConfigMessage(plugin, sender, "admin.player-not-found",
                    Placeholder.unparsed("player", args[0]));
            return true;
        }

        if (target.equals(payer)) {
            FormatUtil.sendConfigMessage(plugin, sender, "local.pay-self");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            FormatUtil.sendConfigMessage(plugin, sender, "local.pay-invalid-amount");
            return true;
        }

        String symbol = plugin.getConfigManager().getLocalCurrencySymbol();
        String currency = plugin.getConfigManager().getLocalCurrencyNamePlural();
        int decimals = plugin.getConfigManager().getLocalFormatDecimals();

        EconomyResponse resp = plugin.getLocalEconomyManager().pay(
                payer.getUniqueId(), payer.getName(),
                target.getUniqueId(), target.getName(),
                amount);

        if (resp.isSuccess()) {
            FormatUtil.sendConfigMessage(plugin, payer, "local.pay-success",
                    FormatUtil.symbol(symbol),
                    FormatUtil.amount(amount, decimals),
                    FormatUtil.currency(currency),
                    FormatUtil.player(target.getName()));

            FormatUtil.sendConfigMessage(plugin, target, "local.pay-received",
                    FormatUtil.symbol(symbol),
                    FormatUtil.amount(amount, decimals),
                    FormatUtil.currency(currency),
                    FormatUtil.player(payer.getName()));

        } else if (resp.getResult() == EconomyResponse.Result.INSUFFICIENT_FUNDS) {
            FormatUtil.sendConfigMessage(plugin, payer, "local.pay-insufficient",
                    FormatUtil.symbol(symbol),
                    FormatUtil.amount(resp.getNewBalance(), decimals),
                    FormatUtil.currency(currency));
        } else {
            FormatUtil.sendConfigMessage(plugin, payer, "local.pay-invalid-amount");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
