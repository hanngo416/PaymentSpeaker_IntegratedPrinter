package service.adapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DefaultImportAdapter implements ChannelImportAdapter {

    private final Map<String, String> aliases = new HashMap<>();

    public DefaultImportAdapter() {
        alias("order_ref", "order_ref", "order_id", "order_sn", "ma_don_hang", "order_number");
        alias("sold_at", "sold_at", "order_date", "date", "created_at", "ngay_dat_hang");
        alias("status", "status", "order_status", "trang_thai");
        alias("goods_amount", "goods_amount", "total_amount", "total_payment", "amount", "tong_tien");
        alias("platform_fee", "platform_fee", "commission_fee", "service_fee", "transaction_fee", "phi");
        alias("shipping_fee", "shipping_fee");
        alias("commission", "commission", "affiliate_commission");
        alias("cost", "cost", "cogs");
    }

    private void alias(String canon, String... names) {
        for (String n : names) {
            aliases.put(n, canon);
        }
    }

    @Override
    public String getChannel() {
        return "DEFAULT";
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
            case "done":
            case "delivered":
                return "COMPLETED";
            case "cancelled":
            case "canceled":
                return "CANCELLED";
            case "returned":
            case "refunded":
                return "RETURNED";
            default:
                return null;
        }
    }
}
