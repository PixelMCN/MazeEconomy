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

public class WithdrawGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final MazeEconomy plugin;

    public WithdrawGui(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        LanguageManager lang = plugin.getLanguageManager();
        GuiManager guiCfg = plugin.getGuiManager();

        Component title = MM.deserialize(
                lang.getString("gui.withdraw-menu.title", "<gray>[<#D97A7A><b>ᴡɪᴛʜᴅʀᴀᴡ</b></#D97A7A>]</gray>"));
        Material bgMat = guiCfg.getMaterial("general.background", Material.GRAY_STAINED_GLASS_PANE);

        Gui gui = Gui.gui()
                .title(title)
                .rows(3)
                .disableAllInteractions()
                .create();

        ItemStack pane = makeItem(bgMat, Component.empty(), List.of());
        gui.getFiller().fill(new GuiItem(pane));

        double bank = plugin.getLocalEconomyManager().getBank(player.getUniqueId());
        String sym = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec = plugin.getConfigManager().getLocalFormatDecimals();
        String fBank = FormatUtil.formatAmount(bank, dec);

        // ── Navigation (Slot 18) ─────────────────────────────
        Material arrowMat = guiCfg.getMaterial("general.back-button", Material.ARROW);
        String arrowName = lang.getString("gui.back-arrow.name", "<!italic><red><b>« ʙᴀᴄᴋ</b></red>");
        gui.setItem(18, new GuiItem(
                makeItem(arrowMat, MM.deserialize(arrowName), List.of()),
                e -> new BankGui(plugin).open(player)));

        // ── Info (Slot 4) ──────────────────────────
        Material infoMat = guiCfg.getMaterial("withdraw-menu.bank-info", Material.PAPER);
        String infoName = lang.getString("gui.withdraw-menu.bank-info.name", "");
        List<String> infoLore = lang.getStringList("gui.withdraw-menu.bank-info.lore");

        gui.setItem(4, new GuiItem(
                makeItem(infoMat, parse(infoName, fBank, "", sym), parseLore(infoLore, fBank, "", sym))));

        // ── Amounts (11 to 15) ─────────────────────
        gui.setItem(11, makeAmountSlot(player, 100, "withdraw-menu.amount-100", Material.RED_DYE, fBank, sym, dec, lang,
                guiCfg));
        gui.setItem(12, makeAmountSlot(player, 500, "withdraw-menu.amount-500", Material.REDSTONE, fBank, sym, dec,
                lang, guiCfg));
        gui.setItem(13, makeAmountSlot(player, 1000, "withdraw-menu.amount-1000", Material.REDSTONE_BLOCK, fBank, sym,
                dec, lang, guiCfg));

        // ALL button
        Material allMat = guiCfg.getMaterial("withdraw-menu.amount-all", Material.NETHER_STAR);
        String allName = lang.getString("gui.withdraw-menu.amount.name", "");
        List<String> allLore = lang.getStringList("gui.withdraw-menu.amount.lore-all");
        gui.setItem(14, new GuiItem(
                makeItem(allMat, parse(allName, fBank, fBank, sym), parseLore(allLore, fBank, fBank, sym)),
                e -> handleWithdraw(player, bank)));

        // CUSTOM button
        Material custMat = guiCfg.getMaterial("withdraw-menu.amount-custom", Material.WRITABLE_BOOK);
        String custName = lang.getString("gui.withdraw-menu.custom.name", "");
        List<String> custLore = lang.getStringList("gui.withdraw-menu.custom.lore");
        gui.setItem(15, new GuiItem(
                makeItem(custMat, parse(custName, fBank, "", sym), parseLore(custLore, fBank, "", sym)),
                e -> promptCustomAmount(player)));

        gui.open(player);
    }

    private GuiItem makeAmountSlot(Player player, double amount, String path, Material def, String fBank, String sym,
            int dec, LanguageManager lang, GuiManager guiCfg) {
        Material mat = guiCfg.getMaterial(path, def);
        String name = lang.getString("gui.withdraw-menu.amount.name", "");
        List<String> lore = lang.getStringList("gui.withdraw-menu.amount.lore-fixed");
        String fAmt = FormatUtil.formatAmount(amount, dec);

        ItemStack item = makeItem(mat, parse(name, fBank, fAmt, sym), parseLore(lore, fBank, fAmt, sym));
        return new GuiItem(item, e -> handleWithdraw(player, amount));
    }

    private void handleWithdraw(Player player, double amount) {
        if (amount <= 0) {
            FormatUtil.sendConfigMessage(plugin, player, "local.bank-insufficient");
            return;
        }

        EconomyResponse resp = plugin.getLocalEconomyManager()
                .withdrawFromBank(player.getUniqueId(), player.getName(), amount);

        String sym = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec = plugin.getConfigManager().getLocalFormatDecimals();

        if (resp.isSuccess()) {
            FormatUtil.sendConfigMessage(plugin, player, "local.bank-withdraw-success",
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
                        String raw = plugin.getLanguageManager().getMessage("local.prompt-withdraw");
                        String prefix = plugin.getLanguageManager().getPrefix();
                        return LegacyComponentSerializer.legacySection().serialize(MM.deserialize(prefix + raw));
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        try {
                            double amount = Double.parseDouble(input);
                            plugin.getServer().getScheduler().runTask(plugin, () -> handleWithdraw(player, amount));
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

    private Component parse(String text, String bank, String amount, String symbol) {
        if (text == null || text.isEmpty())
            return Component.empty();
        String parsed = text.replace("{bank}", bank).replace("{amount}", amount).replace("{symbol}", symbol);
        return MM.deserialize(parsed);
    }

    private List<Component> parseLore(List<String> list, String bank, String amount, String symbol) {
        return list.stream().map(s -> parse(s, bank, amount, symbol)).toList();
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
