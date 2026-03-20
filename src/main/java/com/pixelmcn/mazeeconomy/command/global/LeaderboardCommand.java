package com.pixelmcn.mazeeconomy.command.global;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class LeaderboardCommand implements CommandExecutor {

    private final MazeEconomy plugin;

    public LeaderboardCommand(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!plugin.isGlobalEconomyActive()) {
            FormatUtil.sendConfigMessage(plugin, sender, "global.disabled");
            return true;
        }

        if (!sender.hasPermission("mazeeconomy.global.leaderboard")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            var db = plugin.getGlobalEconomyManager();

            List<Map.Entry<String, Double>> topCoins = db.getTopBalances(GlobalCurrencyType.MAZECOINS, 10);
            List<Map.Entry<String, Double>> topShards = db.getTopBalances(GlobalCurrencyType.SHARDS, 10);

            String coinName = plugin.getConfigManager().getMazecoinNamePlural();
            String shardName = plugin.getConfigManager().getShardNamePlural();
            int coinDec = plugin.getConfigManager().getMazecoinFormatDecimals();
            int shardDec = plugin.getConfigManager().getShardFormatDecimals();
            String coinSym = plugin.getConfigManager().getMazecoinSymbol();
            String shardSym = plugin.getConfigManager().getShardSymbol();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                FormatUtil.sendRaw(sender, "\n<gray><b>─── <#C9A55A>ɢʟᴏʙᴀʟ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ</#C9A55A> ───</b></gray>\n");

                FormatUtil.sendRaw(sender, "<#C9A55A><b>Top " + coinName + ":</b></#C9A55A>");
                sendTop(sender, topCoins, coinSym, coinDec);

                FormatUtil.sendRaw(sender, "\n<#6FA8DC><b>Top " + shardName + ":</b></#6FA8DC>");
                sendTop(sender, topShards, shardSym, shardDec);

                FormatUtil.sendRaw(sender, "");
            });
        });

        return true;
    }

    private void sendTop(CommandSender sender, List<Map.Entry<String, Double>> list, String sym, int dec) {
        if (list.isEmpty()) {
            FormatUtil.sendRaw(sender, "  <gray><i>No data available.</i></gray>");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            var entry = list.get(i);
            FormatUtil.sendRaw(sender, "  <gray>" + (i + 1) + ".</gray> <white>" + entry.getKey()
                    + "</white> <dark_gray>»</dark_gray> " + sym + FormatUtil.formatAmount(entry.getValue(), dec));
        }
    }
}
