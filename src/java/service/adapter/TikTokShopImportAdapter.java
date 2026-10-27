package service.adapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TikTokShopImportAdapter implements ChannelImportAdapter {

    private final Map<String, String> aliases = new HashMap<>();

    public TikTokShopImportAdapter() {
        alias("order_ref", "order_id", "order_number", "order_ref", "ma_don_hang");
        alias("sold_at", "created_time", "order_time", "paid_time", "sold_at", "order_date", "date", "created_at");
        alias("status", "order_status", "status", "trang_thai");
        alias("goods_amount", "sku_subtotal_after_discount", "product_amount", "total_customer_payment", "gmv",
                "goods_amount", "total_amount", "amount");
        alias("platform_fee", "payment_platform_fee", "platform_commission", "transaction_fee", "commission_fee", "platform_fee");
        alias("shipping_fee", "seller_shipping_fee", "original_shipping_fee", "shipping_fee");
        alias("commission", "creator_commission", "affiliate_commission", "commission");
        alias("cost", "cost_of_goods", "cost", "cogs");
        alias("buyer_paid_amount", "buyer_paid_amount", "buyer_paid");
        alias("seller_discount", "seller_discount", "seller_voucher");
        alias("platform_discount", "platform_discount", "tiktok_discount");
        alias("service_fee", "platform_commission", "commission_fee");
        alias("transaction_fee", "payment_platform_fee", "transaction_fee");
        alias("settlement_amount", "settlement_amount", "actual_settlement");
        alias("traffic_source", "traffic_type", "traffic_source", "source");
        alias("sku_code", "seller_sku", "sku_id", "sku");
        alias("product_name", "product_name");
        alias("variation_name", "variation");
        alias("quantity", "quantity", "qty");
    }

    private void alias(String canon, String... names) {
        for (String n : names) {
            aliases.put(n, canon);
        }
    }

    @Override
    public String getChannel() {
        return "TIKTOK";
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
            case "shipped":
            case "done":
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
