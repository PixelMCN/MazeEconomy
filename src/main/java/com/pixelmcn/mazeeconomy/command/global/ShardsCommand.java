package com.pixelmcn.mazeeconomy.command.global;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
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

public class ShardsCommand implements CommandExecutor, TabCompleter {

    private final MazeEconomy plugin;

    public ShardsCommand(MazeEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Global economy disabled guard — checked first, before everything else
        if (!plugin.isGlobalEconomyActive()) {
            FormatUtil.sendConfigMessage(plugin, sender, "global.disabled");
            return true;
        }

        if (!sender.hasPermission("mazeeconomy.global.shards")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        UUID uuid; String name; boolean isSelf;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) { FormatUtil.sendConfigMessage(plugin, sender, "player-only"); return true; }
            uuid = player.getUniqueId(); name = player.getName(); isSelf = true;
        } else {
            @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.getName() == null && !target.hasPlayedBefore()) {
                FormatUtil.sendConfigMessage(plugin, sender, "admin.player-not-found", Placeholder.unparsed("player", args[0]));
                return true;
            }
            uuid = target.getUniqueId(); name = target.getName() != null ? target.getName() : args[0];
            isSelf = sender instanceof Player p && p.getUniqueId().equals(uuid);
        }

        double balance = plugin.getGlobalEconomyManager().getBalance(uuid, GlobalCurrencyType.SHARDS);
        FormatUtil.sendConfigMessage(plugin, sender, isSelf ? "global.balance-shards-self" : "global.balance-shards-other",
                FormatUtil.symbol(plugin.getConfigManager().getShardSymbol()),
                FormatUtil.amount(balance, plugin.getConfigManager().getShardFormatDecimals()),
                FormatUtil.player(name));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
