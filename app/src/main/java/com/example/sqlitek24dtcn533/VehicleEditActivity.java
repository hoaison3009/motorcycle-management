package com.example.sqlitek24dtcn533;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import java.util.Arrays;

public class VehicleEditActivity extends BaseActivity {
    private long id;
    private EditText code,plate;
    private AutoCompleteTextView brand;
    private Spinner status;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); id=getIntent().getLongExtra("id",0);
        screen(R.layout.activity_vehicle_edit,id>0?"Thông tin xe":"Thêm xe");
        code=findViewById(R.id.editCode); plate=findViewById(R.id.editPlate);
        brand=findViewById(R.id.editBrand); status=findViewById(R.id.status);
        brand.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,
                new String[]{"Honda","Yamaha","Suzuki","Piaggio","SYM","VinFast","Kymco"}));
        spinner(status,Arrays.asList(Vehicle.LABELS));
        if(id>0) execute(()-> {
            Vehicle x=db.getVehicle(id);
            if(x==null) {message("Xe không còn tồn tại");finish();return;}
            if(state==null) {
                code.setText(x.code); plate.setText(x.plate); brand.setText(x.brand);
                status.setSelection(Arrays.asList(Vehicle.STATUSES).indexOf(x.status));
            }
        },"Không đọc được xe.");
        findViewById(R.id.btnSave).setOnClickListener(v->execute(()-> {
            db.saveVehicle(new Vehicle(id,value(code),value(plate),value(brand),Vehicle.STATUSES[status.getSelectedItemPosition()])); saved();
        },"Mã xe hoặc biển số đã tồn tại. Biển số được so sánh không phân biệt dấu chấm, gạch ngang và khoảng trắng."));
        findViewById(R.id.btnAssign).setVisibility(id>0?View.VISIBLE:View.GONE);
        findViewById(R.id.btnAssign).setOnClickListener(v->startActivity(new Intent(this,AssignServiceActivity.class).putExtra("vehicleId",id)));
        findViewById(R.id.btnDelete).setVisibility(id>0?View.VISIBLE:View.GONE);
        findViewById(R.id.btnDelete).setOnClickListener(v->confirm("Xóa xe?","Chỉ xóa được xe chưa có phiếu. Không thể hoàn tác.",()->execute(()-> {
            if(db.deleteVehicle(id))saved();else message("Xe không còn tồn tại");
        },"Xe đang có phiếu. Xóa các phiếu liên quan ở màn hình Phiếu trước nếu cần xóa xe.")));
    }
}
