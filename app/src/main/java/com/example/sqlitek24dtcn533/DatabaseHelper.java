package com.example.sqlitek24dtcn533;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "motorcycle_repair.db";
    public static final int DATABASE_VERSION = 1;
    public DatabaseHelper(Context context) { this(context, DATABASE_NAME); }

    public DatabaseHelper(Context context, String databaseName) {
        super(context.getApplicationContext(), databaseName, null, DATABASE_VERSION);
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code))>0)," +
            "plate TEXT NOT NULL, plate_key TEXT NOT NULL UNIQUE CHECK(length(plate_key)>0)," +
            "brand TEXT NOT NULL CHECK(length(trim(brand))>0),brand_key TEXT NOT NULL," +
            "status TEXT NOT NULL CHECK(status IN ('NORMAL','MAINTENANCE','REPAIRING'))," +
            "search_key TEXT NOT NULL)"
        );
        db.execSQL(
            "CREATE TABLE services (id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(code))>0)," +
            "name TEXT NOT NULL CHECK(length(trim(name))>0)," +
            "price INTEGER NOT NULL CHECK(price BETWEEN 0 AND 1000000000),search_key TEXT NOT NULL)"
        );
        db.execSQL(
            "CREATE TABLE repair_orders (id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT," +
            "service_id INTEGER NOT NULL REFERENCES services(id) ON DELETE RESTRICT," +
            "service_code TEXT NOT NULL,service_name TEXT NOT NULL," +
            "price INTEGER NOT NULL CHECK(price BETWEEN 0 AND 1000000000)," +
            "assigned_at INTEGER NOT NULL,completed_at INTEGER," +
            "status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','COMPLETED'))," +
            "note TEXT NOT NULL DEFAULT ''," +
            "CHECK((status='PENDING' AND completed_at IS NULL) OR " +
            "(status='COMPLETED' AND completed_at IS NOT NULL)))"
        );
        db.execSQL("CREATE INDEX idx_vehicle_brand_status ON vehicles(brand_key,status)");
        db.execSQL("CREATE INDEX idx_vehicle_status ON vehicles(status)");
        db.execSQL("CREATE INDEX idx_order_vehicle ON repair_orders(vehicle_id)");
        db.execSQL("CREATE INDEX idx_order_service ON repair_orders(service_id)");
        db.execSQL("CREATE UNIQUE INDEX idx_pending_pair ON repair_orders(vehicle_id,service_id) WHERE status='PENDING'");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Chưa có migration từ " + oldVersion + " đến " + newVersion);
    }
    public long saveVehicle(Vehicle vehicle) {
        String code = InputRules.code(vehicle.code);
        String plate = InputRules.plate(vehicle.plate);
        String brand = InputRules.required(vehicle.brand,"Hãng xe",40);
        boolean valid = false;
        for (String status : Vehicle.STATUSES) if (status.equals(vehicle.status)) valid = true;
        if (!valid) throw new IllegalArgumentException("Chọn tình trạng xe hợp lệ.");
        ContentValues values = new ContentValues();
        values.put("code",code);
        values.put("plate",plate);
        values.put("plate_key",InputRules.plateKey(plate));
        values.put("brand",brand);
        values.put("brand_key",InputRules.normalize(brand));
        values.put("status",vehicle.status);
        values.put("search_key",InputRules.normalize(code+" "+plate+" "+InputRules.plateKey(plate)+" "+brand));
        return save("vehicles",vehicle.id,values);
    }
    public long saveService(ServiceItem service) {
        String code = InputRules.code(service.code);
        String name = InputRules.required(service.name,"Tên dịch vụ",100);
        long price = InputRules.price(String.valueOf(service.price));
        ContentValues values = new ContentValues();
        values.put("code",code);
        values.put("name",name);
        values.put("price",price);
        values.put("search_key",InputRules.normalize(code+" "+name));
        return save("services",service.id,values);
    }
    private long save(String table,long id,ContentValues values) {
        if (id==0) return getWritableDatabase().insertOrThrow(table,null,values);
        if (getWritableDatabase().update(table,values,"id=?",new String[]{String.valueOf(id)})!=1)
            throw new IllegalArgumentException("Bản ghi không còn tồn tại. Hãy quay lại danh sách.");
        return id;
    }
    public Vehicle getVehicle(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM vehicles WHERE id=?",new String[]{String.valueOf(id)})
        ) {
            return c.moveToFirst() ? readVehicle(c) : null;
        }
    }
    public ServiceItem getService(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM services WHERE id=?",new String[]{String.valueOf(id)})
        ) {
            return c.moveToFirst() ? readService(c) : null;
        }
    }
    public List<Vehicle> findVehicles(String keyword,String brandKey,boolean maintenanceOnly) {
        String sql = "SELECT * FROM vehicles WHERE instr(search_key,?)>0";
        List<String> args = new ArrayList<>();
        args.add(InputRules.normalize(keyword));
        if (brandKey != null && !brandKey.isEmpty()) {
            sql += " AND brand_key=?";
            args.add(brandKey);
        }
        if (maintenanceOnly) {
            sql += " AND status=?";
            args.add(Vehicle.MAINTENANCE);
        }
        sql += " ORDER BY id DESC";
        List<Vehicle> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql,args.toArray(new String[0]))) {
            while(c.moveToNext()) rows.add(readVehicle(c));
        }
        return rows;
    }
    public List<String> brands() {
        List<String> rows=new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
            "SELECT MIN(brand) FROM vehicles GROUP BY brand_key ORDER BY brand_key",null)
        ) {
            while(c.moveToNext()) rows.add(c.getString(0));
        }
        return rows;
    }
    public List<ServiceItem> findServices(String keyword) {
        List<ServiceItem> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM services WHERE instr(search_key,?)>0 ORDER BY id DESC",new String[]{InputRules.normalize(keyword)})
        ) {
            while(c.moveToNext()) rows.add(readService(c));
        }
        return rows;
    }

    public long assign(long vehicleId,long serviceId,String note) {
        String cleanNote = InputRules.text(note);
        if (cleanNote.length()>500) throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (getVehicle(vehicleId) == null) throw new IllegalArgumentException("Xe không còn tồn tại.");
            ServiceItem service = getService(serviceId);
            if (service == null) throw new IllegalArgumentException("Dịch vụ không còn tồn tại.");
            ContentValues values = new ContentValues();
            values.put("vehicle_id",vehicleId);
            values.put("service_id",serviceId);
            values.put("service_code",service.code);
            values.put("service_name",service.name);
            values.put("price",service.price);
            values.put("assigned_at",System.currentTimeMillis());
            values.put("status","PENDING");
            values.put("note",cleanNote);
            long id = db.insertOrThrow("repair_orders",null,values);
            db.setTransactionSuccessful(); return id;
        } finally { db.endTransaction(); }
    }
    public List<RepairOrder> orders(long vehicleId) {
        String sql = "SELECT r.*,v.code AS vehicle_code,v.plate FROM repair_orders r JOIN vehicles v ON v.id=r.vehicle_id";
        String[] args = null;
        if (vehicleId > 0) {
            sql += " WHERE r.vehicle_id=?";
            args = new String[]{String.valueOf(vehicleId)};
        }
        sql += " ORDER BY r.id DESC";
        List<RepairOrder> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql,args)) {
            while(c.moveToNext()) {
                rows.add(
                    new RepairOrder(
                        number(c,"id"),
                        number(c,"vehicle_id"),
                        number(c,"service_id"),
                        number(c,"price"),
                        number(c,"assigned_at"),
                        number(c,"completed_at"),
                        text(c,"vehicle_code"),
                        text(c,"plate"),
                        text(c,"service_code"),
                        text(c,"service_name"),
                        text(c,"status"),
                        text(c,"note")));
            }
        }
        return rows;
    }
    public boolean complete(long id) {
        ContentValues values = new ContentValues();
        values.put("status","COMPLETED");
        values.put("completed_at",System.currentTimeMillis());
        return getWritableDatabase().update("repair_orders",values,"id=? AND status='PENDING'",new String[]{String.valueOf(id)}) == 1;
    }
    public boolean deleteOrder(long id) { return delete("repair_orders",id); }
    public boolean deleteVehicle(long id) { return delete("vehicles",id); }
    public boolean deleteService(long id) { return delete("services",id); }
    private boolean delete(String table,long id) {
        return getWritableDatabase().delete(table,"id=?",new String[]{String.valueOf(id)})==1;
    }
    private static long number(Cursor c,String name) { return c.getLong(c.getColumnIndexOrThrow(name)); }
    private static String text(Cursor c,String name) { return c.getString(c.getColumnIndexOrThrow(name)); }
    private Vehicle readVehicle(Cursor c) {
        return new Vehicle(
            number(c,"id"),
            text(c,"code"),
            text(c,"plate"),
            text(c,"brand"),
            text(c,"status")
        );
    }
    private ServiceItem readService(Cursor c) {
        return new ServiceItem(
            number(c,"id"),
            text(c,"code"),
            text(c,"name"),
            number(c,"price")
        );
    }
    public boolean seedDemo() {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            try(Cursor c=db.rawQuery(
                "SELECT (SELECT COUNT(*) FROM vehicles)+(SELECT COUNT(*) FROM services)+(SELECT COUNT(*) FROM repair_orders)",null)
            ) {
                if(c.moveToFirst() && c.getLong(0)>0) return false;
            }
            saveVehicle(new Vehicle(0,"XE001","29B1-123.45","Honda",Vehicle.MAINTENANCE));
            saveVehicle(new Vehicle(0,"XE002","30F1-678.90","Yamaha",Vehicle.NORMAL));
            saveVehicle(new Vehicle(0,"XE003","29C1-246.80","Honda",Vehicle.REPAIRING));
            saveService(new ServiceItem(0,"DV001","Thay dầu động cơ",120000));
            saveService(new ServiceItem(0,"DV002","Vệ sinh lọc gió",50000));
            saveService(new ServiceItem(0,"DV003","Kiểm tra phanh",80000));
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
}
