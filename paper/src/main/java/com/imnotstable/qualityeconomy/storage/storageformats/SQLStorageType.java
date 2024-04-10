package com.imnotstable.qualityeconomy.storage.storageformats;

import com.imnotstable.qualityeconomy.QualityEconomy;
import com.imnotstable.qualityeconomy.storage.accounts.Account;
import com.imnotstable.qualityeconomy.util.debug.Logger;
import com.imnotstable.qualityeconomy.util.storage.EasySQL;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SQLStorageType extends EasySQL implements StorageType {
  
  public SQLStorageType(int databaseType) {
    super(databaseType);
  }
  
  @Override
  public CompletableFuture<Boolean> initStorageProcesses() {
    return CompletableFuture.supplyAsync(() -> {
      if (dataSource != null && !dataSource.isClosed()) {
        Logger.logError("Attempted to open datasource when datasource already exists");
        return false;
      }
      open();
      try (Connection connection = getConnection()) {
        createPlayerDataTable(connection);
        toggleCurrencyTable(connection);
        toggleColumns(connection);
        columns = getColumns(connection);
        generateStatements();
      } catch (SQLException exception) {
        Logger.logError("Error while initiating storage processes", exception);
        return false;
      }
      return true;
    });
  }
  
  @Override
  public void endStorageProcesses() {
    CompletableFuture.runAsync(() -> {
      if (dataSource == null) {
        Logger.logError("Attempted to close datasource when datasource doesn't exist");
        return;
      }
      if (dataSource.isClosed()) {
        Logger.logError("Attempted to close datasource when datasource is already closed");
        return;
      }
      close();
    });
  }
  
  @Override
  public void wipeDatabase() {
    CompletableFuture.runAsync(() -> {
      try (Connection connection = getConnection()) {
        dropPlayerDataTable(connection);
        if (QualityEconomy.getQualityConfig().CUSTOM_CURRENCIES)
          dropCurrencyTable(connection);
        endStorageProcesses();
        initStorageProcesses();
      } catch (SQLException exception) {
        Logger.logError("Failed to wipe database", exception);
      }
    });
  }
  
  @Override
  public void createAccount(@NotNull Account account) {
    CompletableFuture.runAsync(() -> {
      try (Connection connection = getConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(getInsertStatement())) {
        createAccountSetter(preparedStatement, account);
        int affectedRows = preparedStatement.executeUpdate();
        
        if (affectedRows == 0) {
          Logger.logError("Failed to create account (" + account.getUniqueId() + ")");
        }
      } catch (SQLException exception) {
        Logger.logError("Failed to create account (" + account.getUniqueId() + ")", exception);
      }
    });
  }
  
  @Override
  public void createAccounts(@NotNull Collection<Account> accounts) {
    CompletableFuture.runAsync(() -> {
      if (accounts.isEmpty())
        return;
      
      try (Connection connection = getConnection()) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(getInsertStatement())) {
          connection.setAutoCommit(false);
          
          for (Account account : accounts) {
            createAccountSetter(preparedStatement, account);
            preparedStatement.addBatch();
          }
          
          preparedStatement.executeBatch();
          connection.commit();
        } catch (SQLException exception) {
          Logger.logError("Failed to create accounts", exception);
          connection.rollback();
        }
      } catch (SQLException exception) {
        Logger.logError("Failed to rollback transaction", exception);
      }
    });
  }
  
  @Override
  public @NotNull CompletableFuture<Map<UUID, Account>> getAllAccounts() {
    return CompletableFuture.supplyAsync(() -> {
      Map<UUID, Account> accounts = new HashMap<>();
      
      try (Connection connection = getConnection();
           PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM PLAYERDATA")) {
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
          UUID uuid = UUID.fromString(resultSet.getString("UUID"));
          Account account = new Account(uuid)
            .setUsername(resultSet.getString("USERNAME"))
            .setBalance(resultSet.getDouble("BALANCE"));
          if (QualityEconomy.getQualityConfig().COMMANDS_PAY)
            account.setPayable(resultSet.getBoolean("PAYABLE"));
          if (QualityEconomy.getQualityConfig().COMMANDS_REQUEST)
            account.setRequestable(resultSet.getBoolean("REQUESTABLE"));
          if (QualityEconomy.getQualityConfig().CUSTOM_CURRENCIES) {
            Map<String, Double> customCurrencies = new HashMap<>();
            for (String currency : currencies) {
              customCurrencies.put(currency, resultSet.getDouble(currency));
            }
            account.setCustomBalances(customCurrencies);
          }
          accounts.put(uuid, account);
        }
      } catch (SQLException exception) {
        Logger.logError("Failed to get all accounts", exception);
      }
      return accounts;
    });
  }
  
  @Override
  public void saveAccounts(@NotNull Collection<Account> accounts) {
    CompletableFuture.runAsync(() -> {
      try (Connection connection = getConnection()) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(getUpdateStatement())) {
          connection.setAutoCommit(false);
          for (Account account : accounts) {
            if (!account.requiresUpdate())
              continue;
            account.update();
            preparedStatement.setString(1, account.getUsername());
            preparedStatement.setDouble(2, account.getBalance());
            if (QualityEconomy.getQualityConfig().COMMANDS_PAY)
              preparedStatement.setBoolean(columns.indexOf("PAYABLE"), account.isPayable());
            if (QualityEconomy.getQualityConfig().COMMANDS_REQUEST)
              preparedStatement.setBoolean(columns.indexOf("REQUESTABLE"), account.isRequestable());
            if (QualityEconomy.getQualityConfig().CUSTOM_CURRENCIES)
              for (String currency : currencies)
                preparedStatement.setDouble(columns.indexOf(currency), account.getCustomBalance(currency));
            preparedStatement.setString(columns.size(), account.getUniqueId().toString());
            preparedStatement.addBatch();
          }
          preparedStatement.executeBatch();
          connection.commit();
        } catch (SQLException exception) {
          Logger.logError("Failed to update accounts", exception);
          connection.rollback();
        }
      } catch (SQLException exception) {
        Logger.logError("Failed to rollback transaction", exception);
      }
    });
  }
  
  @Override
  public CompletableFuture<Boolean> addCurrency(@NotNull String currency) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection connection = getConnection()) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO CURRENCIES(CURRENCY) VALUES(?)")) {
          preparedStatement.setString(1, currency);
          preparedStatement.executeUpdate();
          addColumn(connection, currency, "FLOAT(53)", "0.0");
        } catch (SQLException exception) {
          Logger.logError("Failed to add currency to database (" + currency + ")", exception);
          connection.rollback();
          return false;
        }
      } catch (SQLException exception) {
        Logger.logError("Failed to retrieve connection to database or rollback", exception);
        return false;
      }
      super.currencies.add(currency);
      return true;
    });
  }
  
  @Override
  public CompletableFuture<Boolean> removeCurrency(@NotNull String currency) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection connection = getConnection()) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM CURRENCIES WHERE CURRENCY = ?")) {
          preparedStatement.setString(1, currency);
          preparedStatement.executeUpdate();
          dropColumn(connection, currency);
        } catch (SQLException exception) {
          Logger.logError("Failed to remove currency from database (" + currency + ")", exception);
          connection.rollback();
          return false;
        }
      } catch (SQLException exception) {
        Logger.logError("Failed to retrieve connection to database or rollback", exception);
        return false;
      }
      super.currencies.remove(currency);
      return true;
    });
  }
  
}
