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
        alias("goods_amount", "sku_subtotal_after_discount", "subtotal_after_seller_discounts", "total_revenue", "doanh_thu_chiu_thue",
                "product_amount", "goods_amount", "total_amount", "amount");
        alias("platform_fee", "total_fees", "phi_san", "platform_fee", "total_platform_fee");
        alias("service_fee", "tiktok_shop_commission_fee", "platform_commission", "commission_fee", "service_fee");
        alias("transaction_fee", "transaction_fee", "payment_platform_fee");
        alias("shipping_fee", "seller_shipping_fee", "original_shipping_fee", "shipping_fee");
        alias("commission", "creator_commission", "affiliate_commission", "commission");
        alias("cost", "cost_of_goods", "cost", "cogs");
        alias("buyer_paid_amount", "buyer_paid_amount", "buyer_paid", "total_customer_payment");
        alias("seller_discount", "seller_discounts", "seller_discount", "seller_voucher");
        alias("platform_discount", "platform_discount", "tiktok_discount", "tiktok_shop_discounts");
        alias("settlement_amount", "total_settlement_amount", "settlement_amount", "actual_settlement");
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
