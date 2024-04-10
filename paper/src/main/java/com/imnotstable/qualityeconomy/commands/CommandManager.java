package com.imnotstable.qualityeconomy.commands;

import java.util.Map;

public class CommandManager {
  
  private static final Map<String, BaseCommand> commands = Map.of(
    "balance", new BalanceCommand(),
    "balancetop", new BalanceTopCommand(),
    "custombalance", new CustomBalanceCommand(),
    "customeconomy", new CustomEconomyCommand(),
    "economy", new EconomyCommand(),
    "qualityeconomy", new MainCommand(),
    "pay", new PayCommand(),
    "request", new RequestCommand(),
    "withdraw", new WithdrawCommand()
  );
  
  public static void registerCommands() {
    commands.values().forEach(BaseCommand::register);
  }
  
  public static void unregisterCommands() {
    commands.values().forEach(BaseCommand::unregister);
  }
  
  public static void registerCommand(String command) {
    if (!commands.containsKey(command))
      throw new IllegalArgumentException("This command does not exist");
    commands.get(command).register();
  }
  
  public static void unregisterCommand(String command) {
    if (!commands.containsKey(command))
      throw new IllegalArgumentException("This command does not exist");
    commands.get(command).unregister();
  }
  
}
