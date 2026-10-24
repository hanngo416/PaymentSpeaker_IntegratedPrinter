package service.adapter;

import java.util.List;
import java.util.Locale;

public class ChannelAdapterFactory {

    private static final ChannelImportAdapter SHOPEE = new ShopeeImportAdapter();
    private static final ChannelImportAdapter TIKTOK = new TikTokShopImportAdapter();
    private static final ChannelImportAdapter DEFAULT = new DefaultImportAdapter();

    public static ChannelImportAdapter getAdapter(String channel) {
        if (channel == null) {
            return DEFAULT;
        }
        String ch = channel.trim().toUpperCase(Locale.ROOT);
        if ("SHOPEE".equals(ch)) {
            return SHOPEE;
        }
        if ("TIKTOK".equals(ch) || "TIKTOK_SHOP".equals(ch) || "TIKTOKSHOP".equals(ch)) {
            return TIKTOK;
        }
        return DEFAULT;
    }

    public static ChannelImportAdapter detectAdapter(List<String> normalizedHeaders) {
        for (String h : normalizedHeaders) {
            if (h.contains("tiktok") || h.equals("sku_subtotal_after_discount") || h.equals("payment_platform_fee") || h.equals("creator_commission")) {
                return TIKTOK;
            }
            if (h.contains("shopee") || h.equals("tong_so_tien_nguoi_mua_thanh_toan") || h.equals("phi_dich_vu") || h.equals("phi_co_dinh")) {
                return SHOPEE;
            }
        }
        return DEFAULT;
    }
}
