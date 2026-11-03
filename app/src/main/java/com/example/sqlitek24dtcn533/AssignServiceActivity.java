package com.example.sqlitek24dtcn533;


import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class AssignServiceActivity extends BaseActivity {
    private Spinner vehicle,service;
    private EditText note;
    private Button save;
    private List<Vehicle> vehicles=new ArrayList<>();
    private List<ServiceItem> services=new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);screen(R.layout.activity_assign,"Gán xe vào dịch vụ");

        vehicle=findViewById(R.id.vehicleSpinner);
        service=findViewById(R.id.serviceSpinner);
        note=findViewById(R.id.editNote);
        save=findViewById(R.id.btnSave);
        save.setEnabled(false);

        execute(()-> {
            vehicles = db.findVehicles("",null,false);
            services=db.findServices("");
            spinner(vehicle,vehicles);
            spinner(service,services);

            long selected = state == null ? getIntent().getLongExtra("vehicleId",0):state.getLong("vehicleId",0);
            long serviceId = state == null? 0 : state.getLong("serviceId",0);
            for(int i=0;i<vehicles.size();i++){
                if(vehicles.get(i).id == selected)vehicle.setSelection(i);
            }
            for(int i=0;i<services.size();i++){
                if(services.get(i).id==serviceId)service.setSelection(i);
            }
            if(vehicles.isEmpty() || services.isEmpty()) {
                ((TextView)findViewById(R.id.assignInfo)).setText("Cần có ít nhất một xe và một dịch vụ. Quay lại màn hình chính để thêm dữ liệu.");
            } else save.setEnabled(true);
        },"Không tải được xe hoặc dịch vụ.");
        service.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {updatePrice();}
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        save.setOnClickListener(v->execute(()-> {
            Vehicle x=(Vehicle)vehicle.getSelectedItem(); ServiceItem s=(ServiceItem)service.getSelectedItem();
            if(x==null||s==null) {message("Vui lòng thêm và chọn xe, dịch vụ trước.");return;}
            save.setEnabled(false);
            try {db.assign(x.id,s.id,value(note));saved();} finally {save.setEnabled(true);}
        },"Xe đã có phiếu chưa hoàn thành cho dịch vụ này. Hoàn thành hoặc xóa phiếu cũ trước khi gán lại."));
    }
    private void updatePrice() {
        ServiceItem s=(ServiceItem)service.getSelectedItem();
        ((TextView)findViewById(R.id.pricePreview)).setText(s==null?"Chưa có dịch vụ":"Giá khi gán: "+InputRules.money(s.price));
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        Vehicle x=(Vehicle)vehicle.getSelectedItem();ServiceItem s=(ServiceItem)service.getSelectedItem();
        if(x!=null)out.putLong("vehicleId",x.id);if(s!=null)out.putLong("serviceId",s.id);
        super.onSaveInstanceState(out);
    }
}
