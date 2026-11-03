package com.example.sqlitek24dtcn533;


import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private int mode=0;
    private long vehicleFilter=0;
    private RowAdapter adapter;
    private EditText search;
    private TextView summary;
    private Button action;
    private List<Vehicle> vehicles=new ArrayList<>();
    private List<ServiceItem> services=new ArrayList<>();
    private List<RepairOrder> orders=new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        screen(R.layout.activity_main,"Quản lý sửa chữa xe máy");
        findViewById(R.id.btnBack).setVisibility(View.GONE);
        search=findViewById(R.id.search);
        summary=findViewById(R.id.summary);
        action=findViewById(R.id.mainAction);
        adapter=new RowAdapter(this);
        ListView list=findViewById(R.id.list);
        list.setAdapter(adapter); list.setEmptyView(findViewById(R.id.empty));
        list.setOnItemClickListener((p,v,position,id)->openRow(position));
        if(state!=null) { mode=state.getInt("mode",0); vehicleFilter=state.getLong("vehicleFilter",0); }
        RadioGroup tabs=findViewById(R.id.tabs);
        tabs.check(mode==1?R.id.tabServices:mode==2?R.id.tabOrders:R.id.tabVehicles);
        tabs.setOnCheckedChangeListener((g,id)-> {
            mode=id==R.id.tabServices?1:id==R.id.tabOrders?2:0;
            if(mode!=2)vehicleFilter=0;
            search.setText(""); refresh();
        });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { refresh(); }
            public void afterTextChanged(Editable e) {}
        });
        action.setOnClickListener(v->startActivity(new Intent(this,mode==0?VehicleEditActivity.class:mode==1?ServiceEditActivity.class:AssignServiceActivity.class)));
        findViewById(R.id.btnReports).setOnClickListener(v->startActivity(new Intent(this,ReportActivity.class)));
        findViewById(R.id.clearFilter).setOnClickListener(v->{vehicleFilter=0;refresh();});
        findViewById(R.id.btnDemo).setOnClickListener(v->confirm("Tạo dữ liệu mẫu?",
                "Thêm 3 xe và 3 dịch vụ khi dữ liệu hiện tại hoàn toàn trống.",()->execute(()-> {
                    message(db.seedDemo()?"Đã tạo dữ liệu mẫu":"Chỉ tạo mẫu khi chưa có xe, dịch vụ hoặc phiếu."); refresh();
                },"Không tạo được dữ liệu mẫu.")));
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
    private void refresh() {
        if(adapter==null)return;
        execute(()-> {
            List<RowAdapter.Row> rows=new ArrayList<>();
            search.setVisibility(mode==2?View.GONE:View.VISIBLE);
            findViewById(R.id.clearFilter).setVisibility(mode==2 && vehicleFilter>0?View.VISIBLE:View.GONE);
            if(mode==0) {
                action.setText("+ Thêm xe"); search.setHint("Tìm mã xe, biển số hoặc hãng");
                vehicles=db.findVehicles(value(search),null,false);
                for(Vehicle x:vehicles)rows.add(new RowAdapter.Row(x.id,x.plate+" · "+x.brand,x.code+"  |  "+x.statusLabel()));
                summary.setText(vehicles.size()+" xe • Chạm để sửa, gán dịch vụ hoặc xem phiếu");
            } else if(mode==1) {
                action.setText("+ Thêm dịch vụ"); search.setHint("Tìm mã hoặc tên dịch vụ");
                services=db.findServices(value(search));
                for(ServiceItem x:services)rows.add(new RowAdapter.Row(x.id,x.name,x.code+"  |  "+InputRules.money(x.price)));
                summary.setText(services.size()+" dịch vụ • Chạm để sửa hoặc xóa");
            } else {
                action.setText("+ Gán xe vào dịch vụ"); orders=db.orders(vehicleFilter);
                long total=0; int pending=0;
                for(RepairOrder x:orders) {
                    total+=x.price; if(x.pending())pending++;
                    rows.add(new RowAdapter.Row(x.id,"#"+x.id+" · "+x.plate+" · "+x.serviceName,
                            x.statusLabel()+"  |  "+InputRules.money(x.price)+"\n"+RepairOrder.date(x.assignedAt)));
                }
                summary.setText((vehicleFilter>0?"Phiếu của xe đã chọn\n":"")+orders.size()+" phiếu · "+pending+" chưa xong\nTổng giá đã gán: "+InputRules.money(total));
            }
            adapter.setNotifyOnChange(false); adapter.clear(); adapter.addAll(rows); adapter.notifyDataSetChanged();
        },"Không đọc được danh sách.");
    }
    private void openRow(int position) {
        if(mode==0 && position<vehicles.size()) {
            Vehicle x=vehicles.get(position);
            new AlertDialog.Builder(this).setTitle(x.toString()).setItems(new String[]{"Xem / sửa xe","Gán dịch vụ","Xem phiếu của xe","Xóa xe"},(d,which)-> {
                if(which==0) startActivity(new Intent(this,VehicleEditActivity.class).putExtra("id",x.id));
                if(which==1) startActivity(new Intent(this,AssignServiceActivity.class).putExtra("vehicleId",x.id));
                if(which==2) { ((RadioGroup)findViewById(R.id.tabs)).check(R.id.tabOrders); vehicleFilter=x.id; refresh(); }
                if(which==3)confirm("Xóa xe "+x.code+"?","Chỉ xóa được xe chưa có phiếu dịch vụ. Thao tác không thể hoàn tác.",()->execute(()-> {
                    message(db.deleteVehicle(x.id)?"Đã xóa xe":"Xe không còn tồn tại"); refresh();
                },"Xe đang có phiếu. Hãy xem và xóa các phiếu liên quan trước nếu thật sự cần xóa xe."));
            }).show();
        } else if(mode==1 && position<services.size()) {
            startActivity(new Intent(this,ServiceEditActivity.class).putExtra("id",services.get(position).id));
        } else if(mode==2 && position<orders.size())showOrder(orders.get(position));
    }
    private void showOrder(RepairOrder x) {
        String detail=x.vehicleCode+" • "+x.plate+"\n"+x.serviceCode+" • "+x.serviceName+
                "\nGiá khi gán: "+InputRules.money(x.price)+"\nNgày gán: "+RepairOrder.date(x.assignedAt)+
                "\n"+x.statusLabel()+(x.completedAt>0?" · "+RepairOrder.date(x.completedAt):"")+"\nGhi chú: "+(x.note.isEmpty()?"Không có":x.note);
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("Phiếu #"+x.id).setMessage(detail).setNegativeButton("Đóng",null);
        if(x.pending())dialog.setPositiveButton("Hoàn thành",(d,w)->confirm("Hoàn thành phiếu?",
                "Chỉ đổi trạng thái phiếu. Hãy cập nhật tình trạng xe riêng sau khi kiểm tra thực tế.",()->execute(()-> {
                    message(db.complete(x.id)?"Đã hoàn thành phiếu":"Phiếu đã thay đổi hoặc không còn tồn tại"); refresh();
                },"Không cập nhật được phiếu.")));
        dialog.setNeutralButton("Xóa phiếu",(d,w)->confirm("Xóa phiếu #"+x.id+"?",
                "Phiếu và giá đã lưu sẽ bị xóa vĩnh viễn; xe và dịch vụ vẫn được giữ lại.",()->execute(()-> {
                    message(db.deleteOrder(x.id)?"Đã xóa phiếu":"Phiếu không còn tồn tại"); refresh();
                },"Không xóa được phiếu."))).show();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("mode",mode); out.putLong("vehicleFilter",vehicleFilter); super.onSaveInstanceState(out);
    }
}
