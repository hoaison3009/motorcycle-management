import com.example.sqlitek24dtcn533.*;
public class RulesSmoke {
    static int checks=0;
    static void eq(Object expected,Object actual){checks++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    static void reject(Runnable work){checks++;try{work.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Expected rejection");}
    public static void main(String[] args){
        eq("thay dau dong co",InputRules.normalize("  Thay dầu Động cơ  "));
        eq("XE001",InputRules.code(" xe001 "));
        eq("29B112345",InputRules.plateKey(InputRules.plate("29b1-123.45")));
        eq("29B112345",InputRules.plateKey("29B1 12345"));
        eq(0L,InputRules.price("0"));eq(120000L,InputRules.price("120000"));eq(1000000000L,InputRules.price("1000000000"));
        reject(()->InputRules.price("-1"));reject(()->InputRules.price("12.5"));reject(()->InputRules.price("1,000"));
        reject(()->InputRules.price("1000000001"));reject(()->InputRules.price("999999999999999999999999"));
        reject(()->InputRules.code(" "));reject(()->InputRules.code("XE 01"));reject(()->InputRules.code("';DROP TABLE vehicles;"));
        reject(()->InputRules.plate("---.."));reject(()->InputRules.required(" ","Hãng",40));
        eq("Cần bảo dưỡng",new Vehicle(1,"X1","29B112345","Honda",Vehicle.MAINTENANCE).statusLabel());
        eq("Hoạt động tốt",new Vehicle(1,"X1","29B112345","Honda",Vehicle.NORMAL).statusLabel());
        System.out.println(checks+" Java rules checks passed.");
    }
}
