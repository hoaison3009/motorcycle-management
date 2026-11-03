package com.example.sqlitek24dtcn533;


import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ServiceEditActivity extends BaseActivity {
    private long id;
    private EditText code,name,price;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); id=getIntent().getLongExtra("id",0);
        screen(R.layout.activity_service_edit,id>0?"Thông tin dịch vụ":"Thêm dịch vụ");
        code=findViewById(R.id.editCode); name=findViewById(R.id.editName); price=findViewById(R.id.editPrice);
        if(id>0)execute(()-> {
            ServiceItem x=db.getService(id);
            if(x==null) {message("Dịch vụ không còn tồn tại");finish();return;}
            if(state==null) {code.setText(x.code);name.setText(x.name);price.setText(String.valueOf(x.price));}
        },"Không đọc được dịch vụ.");
        findViewById(R.id.btnSave).setOnClickListener(v->execute(()-> {
            db.saveService(new ServiceItem(id,value(code),value(name),InputRules.price(value(price))));saved();
        },"Mã dịch vụ đã tồn tại. Vui lòng dùng mã khác."));
        findViewById(R.id.btnDelete).setVisibility(id>0?View.VISIBLE:View.GONE);
        findViewById(R.id.btnDelete).setOnClickListener(v->confirm("Xóa dịch vụ?","Chỉ xóa được dịch vụ chưa có phiếu. Không thể hoàn tác.",()->execute(()-> {
            if(db.deleteService(id))saved();else message("Dịch vụ không còn tồn tại");
        },"Dịch vụ đang được dùng trong phiếu. Xóa các phiếu liên quan trước nếu thật sự cần xóa dịch vụ.")));
    }
}
