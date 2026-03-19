package com.pixelmcn.mazeeconomy.gui;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.config.GuiManager;
import com.pixelmcn.mazeeconomy.config.LanguageManager;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class DepositGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final MazeEconomy plugin;

    public DepositGui(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        LanguageManager lang = plugin.getLanguageManager();
        GuiManager guiCfg = plugin.getGuiManager();

        Component title = MM.deserialize(
                lang.getString("gui.deposit-menu.title", "<gray>[<#7FB77E><b>ᴅᴇᴘᴏsɪᴛ</b></#7FB77E>]</gray>"));
        Material bgMat = guiCfg.getMaterial("general.background", Material.GRAY_STAINED_GLASS_PANE);

        Gui gui = Gui.gui()
                .title(title)
                .rows(3)
                .disableAllInteractions()
                .create();

        ItemStack pane = makeItem(bgMat, Component.empty(), List.of());
        gui.getFiller().fill(new GuiItem(pane));

        double wallet = plugin.getLocalEconomyManager().getWallet(player.getUniqueId());
        String sym = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec = plugin.getConfigManager().getLocalFormatDecimals();
        String fWallet = FormatUtil.formatAmount(wallet, dec);

        // ── Navigation (Slot 18) ─────────────────────────────
        Material arrowMat = guiCfg.getMaterial("general.back-button", Material.ARROW);
        String arrowName = lang.getString("gui.back-arrow.name", "<!italic><red><b>« ʙᴀᴄᴋ</b></red>");
        gui.setItem(18, new GuiItem(
                makeItem(arrowMat, MM.deserialize(arrowName), List.of()),
                e -> new BankGui(plugin).open(player)));

        // ── Info (Slot 4) ──────────────────────────
        Material infoMat = guiCfg.getMaterial("deposit-menu.wallet-info", Material.PAPER);
        String infoName = lang.getString("gui.deposit-menu.wallet-info.name", "");
        List<String> infoLore = lang.getStringList("gui.deposit-menu.wallet-info.lore");

        gui.setItem(4, new GuiItem(
                makeItem(infoMat, parse(infoName, fWallet, "", sym), parseLore(infoLore, fWallet, "", sym))));

        // ── Amounts (11 to 15) ─────────────────────
        gui.setItem(11, makeAmountSlot(player, 100, "deposit-menu.amount-100", Material.LIME_DYE, fWallet, sym, dec,
                lang, guiCfg));
        gui.setItem(12, makeAmountSlot(player, 500, "deposit-menu.amount-500", Material.EMERALD, fWallet, sym, dec,
                lang, guiCfg));
        gui.setItem(13, makeAmountSlot(player, 1000, "deposit-menu.amount-1000", Material.EMERALD_BLOCK, fWallet, sym,
                dec, lang, guiCfg));

        // ALL button
        Material allMat = guiCfg.getMaterial("deposit-menu.amount-all", Material.NETHER_STAR);
        String allName = lang.getString("gui.deposit-menu.amount.name", "");
        List<String> allLore = lang.getStringList("gui.deposit-menu.amount.lore-all");
        gui.setItem(14, new GuiItem(
                makeItem(allMat, parse(allName, fWallet, fWallet, sym), parseLore(allLore, fWallet, fWallet, sym)),
                e -> handleDeposit(player, wallet)));

        // CUSTOM button
        Material custMat = guiCfg.getMaterial("deposit-menu.amount-custom", Material.WRITABLE_BOOK);
        String custName = lang.getString("gui.deposit-menu.custom.name", "");
        List<String> custLore = lang.getStringList("gui.deposit-menu.custom.lore");
        gui.setItem(15, new GuiItem(
                makeItem(custMat, parse(custName, fWallet, "", sym), parseLore(custLore, fWallet, "", sym)),
                e -> promptCustomAmount(player)));

        gui.open(player);
    }

    private GuiItem makeAmountSlot(Player player, double amount, String path, Material def, String fWallet, String sym,
            int dec, LanguageManager lang, GuiManager guiCfg) {
        Material mat = guiCfg.getMaterial(path, def);
        String name = lang.getString("gui.deposit-menu.amount.name", "");
        List<String> lore = lang.getStringList("gui.deposit-menu.amount.lore-fixed");
        String fAmt = FormatUtil.formatAmount(amount, dec);

        ItemStack item = makeItem(mat, parse(name, fWallet, fAmt, sym), parseLore(lore, fWallet, fAmt, sym));
        return new GuiItem(item, e -> handleDeposit(player, amount));
    }

    private void handleDeposit(Player player, double amount) {
        if (amount <= 0) {
            FormatUtil.sendConfigMessage(plugin, player, "local.bank-insufficient");
            return;
        }

        EconomyResponse resp = plugin.getLocalEconomyManager()
                .depositToBank(player.getUniqueId(), player.getName(), amount);

        String sym = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec = plugin.getConfigManager().getLocalFormatDecimals();

        if (resp.isSuccess()) {
            FormatUtil.sendConfigMessage(plugin, player, "local.bank-deposit-success",
                    FormatUtil.symbol(sym),
                    FormatUtil.amount(resp.getAmount(), dec));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
        } else {
            FormatUtil.sendConfigMessage(plugin, player, "local.bank-insufficient");
        }
    }

    private void promptCustomAmount(Player player) {
        player.closeInventory();
        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true).withLocalEcho(false).withEscapeSequence("cancel").withTimeout(30)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        String raw = plugin.getLanguageManager().getMessage("local.prompt-deposit");
                        String prefix = plugin.getLanguageManager().getPrefix();
                        return LegacyComponentSerializer.legacySection().serialize(MM.deserialize(prefix + raw));
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        try {
                            double amount = Double.parseDouble(input);
                            plugin.getServer().getScheduler().runTask(plugin, () -> handleDeposit(player, amount));
                            return Prompt.END_OF_CONVERSATION;
                        } catch (NumberFormatException ex) {
                            FormatUtil.sendConfigMessage(plugin, player, "local.prompt-invalid");
                            return this;
                        }
                    }
                })
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        FormatUtil.sendConfigMessage(plugin, player, "local.prompt-cancelled");
                    }
                });
        factory.buildConversation(player).begin();
    }

    private Component parse(String text, String wallet, String amount, String symbol) {
        if (text == null || text.isEmpty())
            return Component.empty();
        String parsed = text.replace("{wallet}", wallet).replace("{amount}", amount).replace("{symbol}", symbol);
        return MM.deserialize(parsed);
    }

    private List<Component> parseLore(List<String> list, String wallet, String amount, String symbol) {
        return list.stream().map(s -> parse(s, wallet, amount, symbol)).toList();
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
