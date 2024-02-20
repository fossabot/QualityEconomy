package com.imnotstable.qualityeconomy.economy.events;

import com.imnotstable.qualityeconomy.economy.EconomicTransaction;
import com.imnotstable.qualityeconomy.economy.EconomicTransactionType;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CustomBalanceSetEvent extends EconomyEvent {
  
  @Getter
  private static final HandlerList handlerList = new HandlerList();
  
  public CustomBalanceSetEvent(EconomicTransaction transaction) {
    super(transaction, EconomicTransactionType.CUSTOM_BALANCE_SET);
  }
  
  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
  
}
