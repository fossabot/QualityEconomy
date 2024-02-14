package com.imnotstable.qualityeconomy.commands;

import com.imnotstable.qualityeconomy.QualityEconomy;
import com.imnotstable.qualityeconomy.api.QualityEconomyAPI;
import com.imnotstable.qualityeconomy.configuration.Configuration;
import com.imnotstable.qualityeconomy.configuration.MessageType;
import com.imnotstable.qualityeconomy.configuration.Messages;
import com.imnotstable.qualityeconomy.util.CommandUtils;
import com.imnotstable.qualityeconomy.util.Number;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class WithdrawCommand implements Listener, Command {
  
  private final CommandAPICommand command = new CommandAPICommand("withdraw")
    .withArguments(new DoubleArgument("amount", Number.getMinimumValue()))
    .executesPlayer(this::withdraw);
  private final NamespacedKey amountKey = new NamespacedKey(QualityEconomy.getInstance(), "amount");
  private final NamespacedKey ownerKey = new NamespacedKey(QualityEconomy.getInstance(), "owner");
  private boolean isRegistered = false;
  
  public void register() {
    if (isRegistered || !Configuration.areBanknotesEnabled())
      return;
    command.register();
    isRegistered = true;
  }
  
  public void unregister() {
    if (!isRegistered)
      return;
    CommandAPI.unregister(command.getName(), true);
    isRegistered = true;
  }
  
  private void withdraw(Player sender, CommandArguments args) {
    double amount = Number.roundObj(args.get("amount"));
    if (CommandUtils.requirement(QualityEconomyAPI.hasBalance(sender.getUniqueId(), amount), MessageType.SELF_NOT_ENOUGH_MONEY, sender))
      return;
    QualityEconomyAPI.removeBalance(sender.getUniqueId(), amount);
    sender.getInventory().addItem(getBankNote(amount, sender));
    Messages.sendParsedMessage(sender, MessageType.WITHDRAW_MESSAGE,
      Number.formatCommas(amount)
    );
  }
  
  public ItemStack getBankNote(double amount, Player player) {
    ItemStack item = new ItemStack(Material.PAPER);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Messages.getParsedMessage(MessageType.WITHDRAW_BANKNOTE_DISPLAYNAME,
      Number.formatCommas(amount), player.getName()));
    meta.lore(Collections.singletonList(Messages.getParsedMessage(MessageType.WITHDRAW_BANKNOTE_LORE,
      Number.formatCommas(amount), player.getName())));
    meta.getPersistentDataContainer().set(amountKey, PersistentDataType.DOUBLE, amount);
    meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getName());
    item.setItemMeta(meta);
    return item;
  }
  
  @EventHandler
  public void on(PlayerInteractEvent event) {
    if (!Configuration.areBanknotesEnabled() || event.getItem() == null || !event.getItem().getType().equals(Material.PAPER) || !event.getAction().isRightClick())
      return;
    PersistentDataContainer persistentDataContainer = event.getItem().getItemMeta().getPersistentDataContainer();
    if (!persistentDataContainer.has(amountKey) || !persistentDataContainer.has(ownerKey))
      return;
    
    Player player = event.getPlayer();
    double amount = persistentDataContainer.get(amountKey, PersistentDataType.DOUBLE);
    QualityEconomyAPI.addBalance(player.getUniqueId(), amount);
    
    PlayerInventory inventory = player.getInventory();
    inventory.getItem(inventory.getHeldItemSlot()).subtract();
    
    Messages.sendParsedMessage(player, MessageType.WITHDRAW_CLAIM,
      Number.formatCommas(amount), persistentDataContainer.get(ownerKey, PersistentDataType.STRING));
  }
  
}
