package com.pixelmcn.mazeeconomy.command.local;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final MazeEconomy plugin;

    public BalanceCommand(MazeEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("mazeeconomy.balance")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                FormatUtil.sendConfigMessage(plugin, sender, "player-only");
                return true;
            }
            sendBalance(sender, player.getUniqueId(), player.getName(), true);
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.getName() == null && !target.hasPlayedBefore()) {
                FormatUtil.sendConfigMessage(plugin, sender, "admin.player-not-found",
                        Placeholder.unparsed("player", args[0]));
                return true;
            }
            String name = target.getName() != null ? target.getName() : args[0];
            sendBalance(sender, target.getUniqueId(), name, false);
        }
        return true;
    }

    private void sendBalance(CommandSender sender, UUID uuid, String name, boolean isSelf) {
        var mgr    = plugin.getLocalEconomyManager();
        double w   = mgr.getWallet(uuid);
        double b   = mgr.getBank(uuid);
        double t   = w + b;
        String sym = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec    = plugin.getConfigManager().getLocalFormatDecimals();
        String cur = plugin.getConfigManager().getLocalCurrencyNamePlural();

        String msgKey = isSelf ? "local.balance-self" : "local.balance-other";
        FormatUtil.sendConfigMessage(plugin, sender, msgKey,
                FormatUtil.symbol(sym),
                FormatUtil.amount(w, dec),   // {wallet}
                FormatUtil.amount(b, dec),   // {bank}
                FormatUtil.amount(t, dec),   // {total}
                FormatUtil.currency(cur),
                FormatUtil.player(name),
                Placeholder.unparsed("wallet", FormatUtil.formatAmount(w, dec)),
                Placeholder.unparsed("bank",   FormatUtil.formatAmount(b, dec)),
                Placeholder.unparsed("total",  FormatUtil.formatAmount(t, dec)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
