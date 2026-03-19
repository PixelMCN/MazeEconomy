package com.pixelmcn.mazeeconomy.gui;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.config.GuiManager;
import com.pixelmcn.mazeeconomy.config.LanguageManager;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BankGui {

        private static final MiniMessage MM = MiniMessage.miniMessage();
        private final MazeEconomy plugin;

        public BankGui(MazeEconomy plugin) {
                this.plugin = plugin;
        }

        public void open(Player player) {
                LanguageManager lang = plugin.getLanguageManager();
                GuiManager guiCfg = plugin.getGuiManager();

                Component title = MM.deserialize(lang.getString("gui.bank-hub.title",
                                "<gray>[<#C9A55A><b>ʙᴀɴᴋ ʜᴜʙ</b></#C9A55A>]</gray>"));
                Material bgMat = guiCfg.getMaterial("general.background", Material.GRAY_STAINED_GLASS_PANE);

                Gui gui = Gui.gui()
                                .title(title)
                                .rows(3)
                                .disableAllInteractions()
                                .create();

                ItemStack pane = makeItem(bgMat, Component.empty(), List.of());
                gui.getFiller().fill(new GuiItem(pane));

                populateContent(gui, player);
                gui.open(player);
        }

        private void populateContent(Gui gui, Player player) {
                var cfg = plugin.getConfigManager();
                var mgr = plugin.getLocalEconomyManager();
                LanguageManager lang = plugin.getLanguageManager();
                GuiManager guiCfg = plugin.getGuiManager();

                String sym = cfg.getLocalCurrencySymbol();
                int dec = cfg.getLocalFormatDecimals();

                double wallet = mgr.getWallet(player.getUniqueId());
                double bank = mgr.getBank(player.getUniqueId());
                double total = wallet + bank;

                String fWallet = FormatUtil.formatAmount(wallet, dec);
                String fBank = FormatUtil.formatAmount(bank, dec);
                String fTotal = FormatUtil.formatAmount(total, dec);

                // ── Center: Total Wealth (Slot 13) ──────────────────────
                Material accMat = guiCfg.getMaterial("bank-hub.accounts", Material.PAPER);
                String accName = lang.getString("gui.bank-hub.accounts.name", "");
                List<String> accLore = lang.getStringList("gui.bank-hub.accounts.lore");

                gui.setItem(13, new GuiItem(makeItem(accMat,
                                parse(accName, fWallet, fBank, fTotal, sym, "", ""),
                                parseLore(accLore, fWallet, fBank, fTotal, sym, "", ""))));

                // ── Left: Deposit Menu (Slot 11) ────────────────────────
                Material depMat = guiCfg.getMaterial("bank-hub.deposit", Material.LIME_DYE);
                String depName = lang.getString("gui.bank-hub.deposit.name", "");
                List<String> depLore = lang.getStringList("gui.bank-hub.deposit.lore");

                gui.setItem(11, new GuiItem(
                                makeItem(depMat, parse(depName, fWallet, fBank, fTotal, sym, "", ""),
                                                parseLore(depLore, fWallet, fBank, fTotal, sym, "", "")),
                                e -> new DepositGui(plugin).open(player)));

                // ── Right: Withdraw Menu (Slot 15) ──────────────────────
                Material withMat = guiCfg.getMaterial("bank-hub.withdraw", Material.RED_DYE);
                String withName = lang.getString("gui.bank-hub.withdraw.name", "");
                List<String> withLore = lang.getStringList("gui.bank-hub.withdraw.lore");

                gui.setItem(15, new GuiItem(
                                makeItem(withMat, parse(withName, fWallet, fBank, fTotal, sym, "", ""),
                                                parseLore(withLore, fWallet, fBank, fTotal, sym, "", "")),
                                e -> new WithdrawGui(plugin).open(player)));

                // ── Bottom Center: Info (Slot 22) ───────────────────────
                if (cfg.isBankInterestEnabled()) {
                        String rate = String.valueOf(cfg.getBankInterestRate());
                        String interval = cfg.getBankInterestInterval();

                        Material infoMat = guiCfg.getMaterial("bank-hub.info", Material.BOOK);
                        String infoName = lang.getString("gui.bank-hub.info.name", "");
                        List<String> infoLore = lang.getStringList("gui.bank-hub.info.lore");

                        gui.setItem(22, new GuiItem(
                                        makeItem(infoMat, parse(infoName, fWallet, fBank, fTotal, sym, rate, interval),
                                                        parseLore(infoLore, fWallet, fBank, fTotal, sym, rate,
                                                                        interval))));
                }
        }

        private Component parse(String text, String wallet, String bank, String total, String symbol, String rate,
                        String interval) {
                if (text == null || text.isEmpty())
                        return Component.empty();
                String parsed = text
                                .replace("{wallet}", wallet)
                                .replace("{bank}", bank)
                                .replace("{total}", total)
                                .replace("{symbol}", symbol)
                                .replace("{rate}", rate)
                                .replace("{interval}", interval);
                return MM.deserialize(parsed);
        }

        private List<Component> parseLore(List<String> list, String wallet, String bank, String total, String symbol,
                        String rate, String interval) {
                return list.stream().map(s -> parse(s, wallet, bank, total, symbol, rate, interval)).toList();
        }

        private ItemStack makeItem(Material material, Component name, List<Component> lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.displayName(name);
                        if (!lore.isEmpty())
                                meta.lore(lore);
                        item.setItemMeta(meta);
                }
                return item;
        }
}