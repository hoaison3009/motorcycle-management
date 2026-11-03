package com.example.sqlitek24dtcn533;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class RepairOrder {
    public final long id, vehicleId, serviceId, price, assignedAt, completedAt;
    public final String vehicleCode, plate, serviceCode, serviceName, status, note;
    public RepairOrder(
            long id,
            long vehicleId,
            long serviceId,
            long price,
            long assignedAt,
            long completedAt,
            String vehicleCode,
            String plate,
            String serviceCode,
            String serviceName,
            String status,
            String note
    ) {
        this.id=id; this.vehicleId=vehicleId;
        this.serviceId=serviceId; this.price=price;
        this.assignedAt=assignedAt;
        this.completedAt=completedAt;
        this.vehicleCode=vehicleCode;
        this.plate=plate;
        this.serviceCode=serviceCode;
        this.serviceName=serviceName;
        this.status=status; this.note=note;
    }
    public boolean pending() { return "PENDING".equals(status); }
    public String statusLabel() { return pending() ? "Chưa hoàn thành" : "Đã hoàn thành"; }
    public static String date(long time) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi","VN")).format(new Date(time));
    }
}
