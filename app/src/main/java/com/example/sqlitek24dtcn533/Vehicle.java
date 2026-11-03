package com.example.sqlitek24dtcn533;


public final class Vehicle {
    public static final String NORMAL = "NORMAL";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String REPAIRING = "REPAIRING";
    public static final String[] STATUSES = {NORMAL, MAINTENANCE, REPAIRING};
    public static final String[] LABELS = {"Hoạt động tốt", "Cần bảo dưỡng", "Đang sửa chữa"};
    public final long id;
    public final String code, plate, brand, status;
    public Vehicle(long id, String code, String plate, String brand, String status) {
        this.id = id; this.code = code; this.plate = plate; this.brand = brand; this.status = status;
    }
    public String statusLabel() {
        for (int i=0; i<STATUSES.length; i++) if (STATUSES[i].equals(status)) return LABELS[i];
        return status;
    }
    @Override public String toString() { return code + " • " + plate + " • " + brand; }
}
