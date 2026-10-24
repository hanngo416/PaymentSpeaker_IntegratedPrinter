package service.adapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShopeeImportAdapter implements ChannelImportAdapter {

    private final Map<String, String> aliases = new HashMap<>();

    public ShopeeImportAdapter() {
        alias("order_ref", "ma_don_hang", "order_id", "order_sn", "order_ref", "order_number");
        alias("sold_at", "ngay_dat_hang", "order_creation_date", "thoi_gian_tao_don", "order_date", "sold_at", "date", "created_at");
        alias("status", "trang_thai_don_hang", "trang_thai", "order_status", "status");
        alias("goods_amount", "tong_so_tien_nguoi_mua_thanh_toan", "product_subtotal", "tong_gia_ban_san_pham",
                "tong_tien_hang", "tong_tien", "total_amount", "total_payment", "amount", "goods_amount");
        alias("platform_fee", "phi_dich_vu", "phi_thanh_toan", "phi_co_dinh", "phi_san", "phi",
                "commission_fee", "service_fee", "transaction_fee", "platform_fee");
        alias("shipping_fee", "phi_van_chuyen_nguoi_ban_tra", "seller_shipping_fee", "phi_van_chuyen", "shipping_fee");
        alias("commission", "phi_tiep_thi_lien_ket", "shopee_affiliate_fee", "hoa_hong", "commission", "affiliate_commission");
        alias("cost", "gia_von", "cost", "cogs");
    }

    private void alias(String canon, String... names) {
        for (String n : names) {
            aliases.put(n, canon);
        }
    }

    @Override
    public String getChannel() {
        return "SHOPEE";
    }

    @Override
    public Map<String, String> getColumnAliases() {
        return aliases;
    }

    @Override
    public String parseStatus(String raw) {
        String t = service.SalesImportService.normalize(raw);
        switch (t) {
            case "":
            case "completed":
            case "delivered":
            case "done":
            case "hoan_thanh":
            case "da_giao":
                return "COMPLETED";
            case "cancelled":
            case "canceled":
            case "da_huy":
                return "CANCELLED";
            case "returned":
            case "refunded":
            case "tra_hang":
            case "hoan_tien":
                return "RETURNED";
            default:
                return null;
        }
    }
}
