package com.pixelmcn.mazeeconomy.command.admin;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class EcoCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = List.of("set", "add", "remove");
    private static final List<String> CURRENCIES = List.of("local", "mazecoins", "shards");

    private final MazeEconomy plugin;

    public EcoCommand(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("mazeeconomy.admin.eco")) {
            FormatUtil.sendConfigMessage(plugin, sender, "no-permission");
            return true;
        }

        // /eco <set/add/remove> <local/mazecoins/shards> <player> <amount>
        if (args.length < 4) {
            sender.sendMessage("/eco <set/add/remove> <local/mazecoins/shards> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String currencyArg = args[1].toLowerCase(Locale.ROOT);
        String playerArg = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
            if (amount < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            FormatUtil.sendConfigMessage(plugin, sender, "admin.invalid-amount");
            return true;
        }

        if (!ACTIONS.contains(action)) {
            sender.sendMessage("/eco <set/add/remove> <local/mazecoins/shards> <player> <amount>");
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerArg);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            FormatUtil.sendConfigMessage(plugin, sender, "admin.player-not-found",
                    Placeholder.unparsed("player", playerArg));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : playerArg;

        switch (currencyArg) {
            case "local" -> {
                if (!plugin.getConfigManager().isLocalEconomyEnabled()) {
                    FormatUtil.sendConfigMessage(plugin, sender, "local.feature-disabled");
                    return true;
                }
                handleLocal(sender, action, target, targetName, amount);
            }
            case "mazecoins", "shards" -> {
                if (!plugin.isGlobalEconomyActive()) {
                    FormatUtil.sendConfigMessage(plugin, sender, "global.disabled");
                    return true;
                }
                GlobalCurrencyType type = currencyArg.equals("mazecoins")
                        ? GlobalCurrencyType.MAZECOINS
                        : GlobalCurrencyType.SHARDS;
                handleGlobal(sender, action, target, targetName, type, amount);
            }
            default -> sender.sendMessage("/eco <set/add/remove> <local/mazecoins/shards> <player> <amount>");
        }

        return true;
    }

    private void handleLocal(CommandSender sender, String action, OfflinePlayer target,
            String targetName, double amount) {
        var manager = plugin.getLocalEconomyManager();
        String symbol = plugin.getConfigManager().getLocalCurrencySymbol();
        String currency = plugin.getConfigManager().getLocalCurrencyNamePlural();
        int decimals = plugin.getConfigManager().getLocalFormatDecimals();

        EconomyResponse resp = switch (action) {
            case "set" -> manager.setBalance(target.getUniqueId(), targetName, amount);
            case "add" -> manager.deposit(target.getUniqueId(), targetName, amount);
            case "remove" -> manager.withdraw(target.getUniqueId(), targetName, amount);
            default -> EconomyResponse.failure("Unknown action");
        };

        sendResult(sender, action, resp, targetName, symbol, currency, amount, decimals);
    }

    private void handleGlobal(CommandSender sender, String action, OfflinePlayer target,
            String targetName, GlobalCurrencyType currency, double amount) {
        var manager = plugin.getGlobalEconomyManager();

        String symbol = switch (currency) {
            case MAZECOINS -> plugin.getConfigManager().getMazecoinSymbol();
            case SHARDS -> plugin.getConfigManager().getShardSymbol();
        };
        String currencyName = switch (currency) {
            case MAZECOINS -> plugin.getConfigManager().getMazecoinNamePlural();
            case SHARDS -> plugin.getConfigManager().getShardNamePlural();
        };
        int decimals = switch (currency) {
            case MAZECOINS -> plugin.getConfigManager().getMazecoinFormatDecimals();
            case SHARDS -> plugin.getConfigManager().getShardFormatDecimals();
        };

        EconomyResponse resp = switch (action) {
            case "set" -> manager.setBalance(target.getUniqueId(), targetName, currency, amount);
            case "add" -> manager.deposit(target.getUniqueId(), targetName, currency, amount);
            case "remove" -> manager.withdraw(target.getUniqueId(), targetName, currency, amount);
            default -> EconomyResponse.failure("Unknown action");
        };

        sendResult(sender, action, resp, targetName, symbol, currencyName, amount, decimals);
    }

    private void sendResult(CommandSender sender, String action, EconomyResponse resp,
            String targetName, String symbol, String currency, double amount, int decimals) {
        if (resp.isSuccess()) {
            String msgKey = switch (action) {
                case "set" -> "admin.set-success";
                case "add" -> "admin.add-success";
                case "remove" -> "admin.remove-success";
                default -> "admin.set-success";
            };
            FormatUtil.sendConfigMessage(plugin, sender, msgKey,
                    FormatUtil.player(targetName),
                    FormatUtil.amount(amount, decimals),
                    FormatUtil.symbol(symbol),
                    FormatUtil.currency(currency));
        } else if (resp.getResult() == EconomyResponse.Result.INSUFFICIENT_FUNDS) {
            FormatUtil.sendConfigMessage(plugin, sender, "admin.remove-insufficient",
                    FormatUtil.player(targetName),
                    FormatUtil.currency(currency));
        } else {
            FormatUtil.sendConfigMessage(plugin, sender, "admin.invalid-amount");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> ACTIONS.stream()
                    .filter(a -> a.startsWith(args[0].toLowerCase()))
                    .toList();
            case 2 -> CURRENCIES.stream()
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .toList();
            case 3 -> Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
            default -> List.of();
        };
    }
}
