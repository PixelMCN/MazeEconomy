package com.pixelmcn.mazeeconomy.command.local;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.BalanceEntry;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BalanceTopCommand implements CommandExecutor {

    private final MazeEconomy plugin;

    public BalanceTopCommand(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("mazeeconomy.balancetop")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        int totalPages = plugin.getLocalEconomyManager().getTotalPages();
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        final int finalPage  = page;
        final int finalTotal = totalPages;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BalanceEntry> entries = plugin.getLocalEconomyManager().getTopBalances(finalPage);
            int perPage  = plugin.getConfigManager().getBaltopEntriesPerPage();
            int offset   = (finalPage - 1) * perPage;
            String symbol   = plugin.getConfigManager().getLocalCurrencySymbol();
            String currency = plugin.getConfigManager().getLocalCurrencyNamePlural();
            int decimals    = plugin.getConfigManager().getLocalFormatDecimals();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                FormatUtil.sendConfigMessage(plugin, sender, "local.baltop-header",
                        FormatUtil.page(finalPage),
                        FormatUtil.maxPage(finalTotal));

                if (entries.isEmpty()) {
                    FormatUtil.sendRaw(sender, "<gray>No entries found.</gray>");
                } else {
                    for (int i = 0; i < entries.size(); i++) {
                        BalanceEntry entry = entries.get(i);
                        FormatUtil.sendConfigMessage(plugin, sender, "local.baltop-entry",
                                FormatUtil.rank(offset + i + 1),
                                FormatUtil.player(entry.playerName()),
                                FormatUtil.symbol(symbol),
                                FormatUtil.amount(entry.balance(), decimals),
                                FormatUtil.currency(currency));
                    }
                }
                FormatUtil.sendConfigMessage(plugin, sender, "local.baltop-footer");
            });
        });

        return true;
    }
}
