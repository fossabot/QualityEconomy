package com.imnotstable.qualityeconomy.commands;

import com.imnotstable.qualityeconomy.QualityEconomy;
import com.imnotstable.qualityeconomy.configuration.Configuration;
import com.imnotstable.qualityeconomy.configuration.MessageType;
import com.imnotstable.qualityeconomy.configuration.Messages;
import com.imnotstable.qualityeconomy.storage.accounts.Account;
import com.imnotstable.qualityeconomy.storage.accounts.AccountManager;
import com.imnotstable.qualityeconomy.util.Debug;
import com.imnotstable.qualityeconomy.util.Number;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Comparator;

public class BalanceTopCommand extends BaseCommand {
  
  public static Account[] orderedPlayerList;
  private String serverTotal = "0.0";
  private int maxPage;
  private final CommandTree command = new CommandTree("balancetop")
    .withAliases("baltop")
    .then(new LiteralArgument("update")
      .withPermission("qualityeconomy.balancetop.update")
      .executes((sender, args) -> {
        updateBalanceTop();
      }))
    .then(new IntegerArgument("page", 1)
      .setOptional(true)
      .executes(this::viewBalanceTop));
  private Integer taskID = null;
  
  public void register() {
    if (!super.register(command))
      return;
    if (Configuration.getBalancetopInterval() != 0)
      taskID = Bukkit.getScheduler().runTaskTimerAsynchronously(QualityEconomy.getInstance(), this::updateBalanceTop, 0L, Configuration.getBalancetopInterval()).getTaskId();
  }
  
  public void unregister() {
    if (!super.unregister(command))
      return;
    if (taskID != null) {
      Bukkit.getScheduler().cancelTask(taskID);
      taskID = null;
    }
  }
  
  private void viewBalanceTop(CommandSender sender, CommandArguments args) {
    int page = Math.min((int) args.getOrDefault("page", 1), maxPage);
    int startIndex = (page - 1) * 10;
    int endIndex = Math.min(startIndex + 10, orderedPlayerList.length);
    
    Messages.sendParsedMessage(sender, MessageType.BALANCETOP_TITLE,
      String.valueOf(maxPage), String.valueOf(page));
    Messages.sendParsedMessage(sender, MessageType.BALANCETOP_SERVER_TOTAL,
      serverTotal);
    
    if (maxPage != 0)
      for (int i = startIndex; i < endIndex; i++) {
        Account account = orderedPlayerList[i];
        Messages.sendParsedMessage(sender, MessageType.BALANCETOP_BALANCE_VIEW,
          Number.format(account.getBalance(), Number.FormatType.COMMAS), String.valueOf(i + 1), account.getUsername());
      }
    
    Messages.sendParsedMessage(sender, MessageType.BALANCETOP_NEXT_PAGE,
      args.fullInput().split(" ")[0].substring(1), String.valueOf(page + 1));
  }
  
  private void updateBalanceTop() {
    Debug.Timer timer = new Debug.Timer("updateBalanceTop()");
    
    Collection<Account> accounts = AccountManager.getAllAccounts();
    
    serverTotal = Number.format(accounts.stream()
      .mapToDouble(Account::getBalance)
      .sum(), Number.FormatType.COMMAS);
    orderedPlayerList = accounts.stream()
      .sorted(Comparator.comparingDouble(Account::getBalance).reversed())
      .toArray(Account[]::new);
    maxPage = (int) Math.ceil(orderedPlayerList.length / 10.0);
    
    timer.end();
  }
  
}
