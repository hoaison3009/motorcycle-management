package com.example.sqlitek24dtcn533;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends BaseActivity {
    private Spinner brand;
    private CheckBox onlyMaintenance;
    private RowAdapter adapter;
    private String selectedBrand="";
    private boolean updating=false;
    private List<Vehicle> vehicles=new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);screen(R.layout.activity_report,"Báo cáo và truy vấn xe");

        brand=findViewById(R.id.brandFilter);
        onlyMaintenance=findViewById(R.id.onlyMaintenance);
        adapter=new RowAdapter(this);

        ListView list=findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.empty));

        if(state!=null) {
            selectedBrand = state.getString("brand","");
            onlyMaintenance.setChecked(state.getBoolean("maintenance",false));
        }

        brand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                if(updating) return;
                selectedBrand = position == 0 ? "" : InputRules.normalize(parent.getItemAtPosition(position).toString());
                refresh();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        onlyMaintenance.setOnCheckedChangeListener((b,checked)->refresh());

        findViewById(R.id.resetFilters).setOnClickListener(v->{selectedBrand="";brand.setSelection(0);onlyMaintenance.setChecked(false);refresh();});
        list.setOnItemClickListener((p,v,position,id)-> {
            if(position<vehicles.size()){
                startActivity(new Intent(this,VehicleEditActivity.class).putExtra("id",vehicles.get(position).id));
            }
        });
    }
    @Override protected void onResume() {
        super.onResume();
        execute(()-> {
            updating=true;
            try {
                List<String> brands = new ArrayList<>();
                brands.add("Tất cả hãng");
                brands.addAll(db.brands());
                spinner(brand,brands);int selection=0;

                for(int i=1;i<brands.size();i++){
                    if(InputRules.normalize(brands.get(i)).equals(selectedBrand)){
                        selection=i;
                    }
                }
                if(selection == 0)selectedBrand="";
                brand.setSelection(selection);
            } finally {
                updating=false;
            }
            refresh();
        },"Không tải được hãng xe.");
    }
    private void refresh() {
        if(adapter==null||updating)return;
        execute(()-> {
            vehicles=db.findVehicles("",selectedBrand,onlyMaintenance.isChecked());

            List<RowAdapter.Row> rows=new ArrayList<>();
            for(Vehicle x:vehicles){
                rows.add(new RowAdapter.Row(x.id,x.plate+" · "+x.brand,x.code+"  |  "+x.statusLabel()));
            }

            adapter.setNotifyOnChange(false);
            adapter.clear();adapter.addAll(rows);
            adapter.notifyDataSetChanged();
            ((TextView)findViewById(R.id.summary)).setText("Kết quả: "+vehicles.size()+" xe"+(onlyMaintenance.isChecked()?" cần bảo dưỡng":""));
        },"Không truy vấn được dữ liệu.");
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("brand",selectedBrand);out.putBoolean("maintenance",onlyMaintenance.isChecked());super.onSaveInstanceState(out);
    }
}
