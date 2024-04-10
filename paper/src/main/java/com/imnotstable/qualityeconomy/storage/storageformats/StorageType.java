package com.imnotstable.qualityeconomy.storage.storageformats;

import com.imnotstable.qualityeconomy.storage.accounts.Account;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageType {
  
  CompletableFuture<Boolean> initStorageProcesses();
  
  void endStorageProcesses();
  
  void wipeDatabase();
  
  void createAccount(@NotNull Account account);
  
  void createAccounts(@NotNull Collection<Account> accounts);
  
  void saveAccounts(@NotNull Collection<Account> accounts);
  
  @NotNull
  CompletableFuture<Map<UUID, Account>> getAllAccounts();
  
  @NotNull
  Set<String> getCurrencies();
  
  boolean addCurrency(@NotNull String currency);
  
  boolean removeCurrency(@NotNull String currency);
  
}
