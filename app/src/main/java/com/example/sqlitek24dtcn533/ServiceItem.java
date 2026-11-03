package com.example.sqlitek24dtcn533;


public final class ServiceItem {
    public final long id, price;
    public final String code, name;
    public ServiceItem(long id, String code, String name, long price) {
        this.id = id; this.code = code; this.name = name; this.price = price;
    }
    @Override public String toString() { return code + " • " + name + " • " + InputRules.money(price); }
}
