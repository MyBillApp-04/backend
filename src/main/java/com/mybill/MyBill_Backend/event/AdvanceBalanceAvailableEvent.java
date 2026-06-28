package com.mybill.MyBill_Backend.event;

import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.User;
import org.springframework.context.ApplicationEvent;

public class AdvanceBalanceAvailableEvent extends ApplicationEvent {
    private final Client client;
    private final User user;
    private final double advanceAmount;

    public AdvanceBalanceAvailableEvent(Object source, Client client, User user, double advanceAmount) {
        super(source);
        this.client = client;
        this.user = user;
        this.advanceAmount = advanceAmount;
    }

    public Client getClient() {
        return client;
    }

    public User getUser() {
        return user;
    }

    public double getAdvanceAmount() {
        return advanceAmount;
    }
}
