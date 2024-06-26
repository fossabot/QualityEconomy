package com.imnotstable.qualityeconomy.commands;

import com.imnotstable.qualityeconomy.QualityEconomy;
import com.imnotstable.qualityeconomy.api.QualityEconomyAPI;
import com.imnotstable.qualityeconomy.config.MessageType;
import com.imnotstable.qualityeconomy.config.Messages;
import com.imnotstable.qualityeconomy.economy.EconomicTransaction;
import com.imnotstable.qualityeconomy.economy.EconomicTransactionType;
import com.imnotstable.qualityeconomy.economy.EconomyPlayer;
import com.imnotstable.qualityeconomy.util.CommandUtils;
import com.imnotstable.qualityeconomy.util.Number;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestCommand extends BaseCommand {
  
  //<Requestee, <Requester, Amount>>
  @Getter
  private static ConcurrentMap<UUID, ConcurrentMap<UUID, Double>> requests = null;
  
  private final CommandTree command = new CommandTree("request")
    .withPermission("qualityeconomy.request")
    .then(new LiteralArgument("toggle")
      .executesPlayer(this::toggleRequests))
    .then(new MultiLiteralArgument("answer", "accept", "deny")
      .withRequirement(sender -> sender instanceof Player player && QualityEconomyAPI.hasRequest(player.getUniqueId()))
      .then(CommandUtils.TargetArgument(true)
        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
          if (info.sender() instanceof Player player && QualityEconomyAPI.hasRequest(player.getUniqueId()))
            return requests.get(player.getUniqueId()).keySet().stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).toArray(String[]::new);
          return new String[0];
        }))
        .executesPlayer(this::answerRequest)))
    .then(new LiteralArgument("send")
      .then(CommandUtils.TargetArgument(true)
        .then(CommandUtils.AmountArgument()
          .executesPlayer(this::request))));
  
  public void register() {
    if (!super.register(command, QualityEconomy.getQualityConfig().COMMANDS_REQUEST))
      return;
    requests = new ConcurrentHashMap<>();
  }
  
  public void unregister() {
    if (!super.unregister(command))
      return;
    requests = null;
  }
  
  private void toggleRequests(Player sender, CommandArguments args) {
    boolean toggle = !QualityEconomyAPI.isRequestable(sender.getUniqueId());
    QualityEconomyAPI.setRequestable(sender.getUniqueId(), toggle);
    if (toggle) Messages.sendParsedMessage(sender, MessageType.REQUEST_TOGGLE_ON);
    else Messages.sendParsedMessage(sender, MessageType.REQUEST_TOGGLE_OFF);
  }
  
  @SneakyThrows
  private void request(Player requester, CommandArguments args) {
    Player requestee = (Player) args.get("target");
    if (CommandUtils.requirement(QualityEconomyAPI.isRequestable(requestee.getUniqueId()), MessageType.NOT_ACCEPTING_REQUESTS, requester))
      return;
    double amount = (double) args.get("amount");
    if (CommandUtils.requirement(QualityEconomyAPI.hasBalance(requestee.getUniqueId(), amount), MessageType.OTHER_NOT_ENOUGH_MONEY, requester))
      return;
    if (CommandUtils.requirement(amount >= Number.getMinimumValue(), MessageType.INVALID_NUMBER, requester))
      return;
    EconomicTransaction.startNewTransaction(EconomicTransactionType.REQUEST, amount, EconomyPlayer.of(requester), EconomyPlayer.of(requestee)).execute();
  }
  
  private void answerRequest(Player requestee, CommandArguments args) {
    OfflinePlayer requester = (OfflinePlayer) args.get("target");
    double amount = Number.round(requests.get(requestee.getUniqueId()).get(requester.getUniqueId()));
    String answer = (String) args.get("answer");
    if (answer.equalsIgnoreCase("accept")) accept(requestee, requester, amount);
    else if (answer.equalsIgnoreCase("deny")) deny(requestee, requester, amount);
  }
  
  @SneakyThrows
  private void accept(Player requestee, OfflinePlayer requester, double amount) {
    if (CommandUtils.requirement(QualityEconomyAPI.hasBalance(requestee.getUniqueId(), amount), MessageType.SELF_NOT_ENOUGH_MONEY, requestee))
      return;
    EconomicTransaction.startNewTransaction(EconomicTransactionType.REQUEST_ACCEPT, amount, EconomyPlayer.of(requester), EconomyPlayer.of(requestee)).execute();
  }
  
  @SneakyThrows
  private void deny(Player requestee, OfflinePlayer requester, double amount) {
    EconomicTransaction.startNewTransaction(EconomicTransactionType.REQUEST_DENY, amount, EconomyPlayer.of(requester), EconomyPlayer.of(requestee)).execute();
  }
  
}
