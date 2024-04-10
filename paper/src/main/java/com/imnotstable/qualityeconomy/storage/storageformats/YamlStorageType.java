package com.imnotstable.qualityeconomy.storage.storageformats;

import com.imnotstable.qualityeconomy.QualityEconomy;
import com.imnotstable.qualityeconomy.storage.accounts.Account;
import com.imnotstable.qualityeconomy.util.debug.Logger;
import com.imnotstable.qualityeconomy.util.storage.EasyYaml;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class YamlStorageType extends EasyYaml implements StorageType {
  
  @Override
  public CompletableFuture<Boolean> initStorageProcesses() {
    return CompletableFuture.supplyAsync(() -> {
      if (yaml != null) return false;
      try {
        if (!file.exists()) {
          if (!file.createNewFile())
            return false;
          yaml = new YamlConfiguration();
        } else {
          yaml = YamlConfiguration.loadConfiguration(file);
        }
        toggleCustomCurrencies();
        save();
      } catch (IOException exception) {
        Logger.logError("Failed to initiate storage processes", exception);
        return false;
      }
      return true;
    });
  }
  
  @Override
  public void endStorageProcesses() {
    CompletableFuture.runAsync(() -> {
      if (yaml == null) return;
      if (file.exists())
        save();
      yaml = null;
    });
  }
  
  @Override
  public void wipeDatabase() {
    CompletableFuture.runAsync(() -> {
      file.delete();
      endStorageProcesses();
      initStorageProcesses();
    });
  }
  
  @Override
  public void createAccount(@NotNull Account account) {
    CompletableFuture.runAsync(() -> {
      setAccount(account);
      save();
    });
  }
  
  @Override
  public void createAccounts(@NotNull Collection<Account> accounts) {
    CompletableFuture.runAsync(() -> {
      accounts.forEach(this::setAccount);
      save();
    });
  }
  
  @Override
  public void saveAccounts(@NotNull Collection<Account> accounts) {
    CompletableFuture.runAsync(() -> {
      accounts.stream()
        .filter(Account::requiresUpdate)
        .forEach(account -> setAccount(account.update()));
      save();
    });
  }
  
  @Override
  public @NotNull CompletableFuture<Map<UUID, Account>> getAllAccounts() {
    return CompletableFuture.supplyAsync(() -> {
      Map<UUID, Account> accounts = new HashMap<>();
      for (String uuid : getAllUniqueIds()) {
        Account account = new Account(UUID.fromString(uuid));
        account.setUsername(yaml.getString(uuid + ".USERNAME"));
        account.setBalance(yaml.getDouble(uuid + ".BALANCE"));
        if (QualityEconomy.getQualityConfig().COMMANDS_PAY)
          account.setPayable(yaml.getBoolean(uuid + ".PAYABLE"));
        if (QualityEconomy.getQualityConfig().COMMANDS_REQUEST)
          account.setRequestable(yaml.getBoolean(uuid + ".REQUESTABLE"));
        if (QualityEconomy.getQualityConfig().CUSTOM_CURRENCIES)
          for (String currency : getCurrencies())
            account.setCustomBalance(currency, yaml.getDouble(uuid + "." + currency));
        accounts.put(account.getUniqueId(), account);
      }
      return accounts;
    });
  }
  
  @Override
  public CompletableFuture<Boolean> addCurrency(@NotNull String currency) {
    return CompletableFuture.supplyAsync(() -> {
      List<String> currencies = yaml.getStringList("custom-currencies");
      yaml.set("custom-currencies", currencies);
      for (String uuid : getAllUniqueIds())
        yaml.set(uuid + "." + currency, 0);
      save();
      super.currencies.add(currency);
      return true;
    });
  }
  
  @Override
  public CompletableFuture<Boolean> removeCurrency(@NotNull String currency) {
    return CompletableFuture.supplyAsync(() -> {
      List<String> currencies = yaml.getStringList("custom-currencies");
      yaml.set("custom-currencies", currencies);
      for (String uuid : getAllUniqueIds())
        yaml.set(uuid + "." + currency, null);
      save();
      super.currencies.remove(currency);
      return true;
    });
  }
  
}
