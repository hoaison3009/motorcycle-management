package com.example.sqlitek24dtcn533;


import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
    protected DatabaseHelper db;
    protected void screen(int layout,String title) {
        setContentView(layout);
        db = new DatabaseHelper(this);
        ((TextView)findViewById(R.id.toolbarTitle)).setText(title);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        View root = findViewById(R.id.root);
        int left = root.getPaddingLeft(),
            top = root.getPaddingTop(),
            right = root.getPaddingRight(),
            bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets) -> {
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
    protected void message(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    protected String value(EditText field) { return field.getText().toString().trim(); }
    protected void execute(Runnable operation,String constraintMessage) {
        try { operation.run(); }
        catch(SQLiteConstraintException e) { message(constraintMessage); }
        catch(IllegalArgumentException e) { message(e.getMessage()); }
        catch(SQLiteException e) {
            Log.e("MotorRepair", "Database operation failed",e);
            message("Không truy cập được dữ liệu. Kiểm tra dung lượng thiết bị và Logcat.");
        }
    }
    protected void confirm(String title,String detail,Runnable operation) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(detail)
            .setNegativeButton("Hủy",null).setPositiveButton("Đồng ý",(d,w)->operation.run()).show();
    }
    protected <T> void spinner(Spinner view,List<T> values) {
        ArrayAdapter<T> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        view.setAdapter(adapter);
    }
    protected void saved() { message("Đã lưu thành công"); setResult(RESULT_OK); finish(); }
    @Override protected void onDestroy() {
        if(db != null) db.close();
        super.onDestroy();
    }
}
