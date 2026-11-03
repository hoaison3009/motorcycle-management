package com.example.sqlitek24dtcn533;


import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;

public final class InputRules {
    private InputRules() {}
    public static String text(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
    public static String normalize(String value) {
        return Normalizer.normalize(text(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ','d').replace('Đ','D')
                .toLowerCase(Locale.ROOT);
    }
    public static String code(String value) {
        String result = text(value).toUpperCase(Locale.ROOT);
        if (!result.matches("[A-Z0-9_-]{1,20}"))
            throw new IllegalArgumentException("Mã gồm 1–20 chữ A–Z, số, dấu - hoặc _.");
        return result;
    }
    public static String plate(String value) {
        String result = text(value).toUpperCase(Locale.ROOT);
        if (!result.matches("[A-Z0-9 .-]{5,20}") || !plateKey(result).matches("[A-Z0-9]{5,15}"))
            throw new IllegalArgumentException("Biển số gồm 5–15 chữ/số; có thể dùng dấu chấm, gạch ngang và khoảng trắng.");
        return result;
    }
    public static String plateKey(String value) {
        return text(value).toUpperCase(Locale.ROOT).replaceAll("[ .-]", "");
    }
    public static String required(String value, String label, int max) {
        String result = text(value);
        if (result.isEmpty() || result.length() > max)
            throw new IllegalArgumentException(label + " phải có từ 1 đến " + max + " ký tự.");
        return result;
    }
    public static long price(String value) {
        String result = text(value);
        if (!result.matches("[0-9]{1,10}"))
            throw new IllegalArgumentException("Giá phải là số nguyên VNĐ không âm, không có dấu phân cách.");
        long number = Long.parseLong(result);
        if (number > 1000000000L)
            throw new IllegalArgumentException("Giá tối đa là 1.000.000.000 VNĐ.");
        return number;
    }
    public static String money(long value) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(value) + " đ";
    }
}
