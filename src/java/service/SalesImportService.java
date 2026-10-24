package service;

import dao.ChannelSaleDAO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import model.ChannelSale;
import service.adapter.ChannelAdapterFactory;
import service.adapter.ChannelImportAdapter;
import util.DBUtil;

public class SalesImportService {

    public static final int MAX_ROWS = 5000;
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final long MAX_AMOUNT = 50000000L;

    private static final String[] REQUIRED = {"order_ref", "sold_at", "goods_amount"};
    private static final DateTimeFormatter[] DATE_TIMES = {
        fmt("uuuu-MM-dd HH:mm:ss"), fmt("uuuu-MM-dd HH:mm"), fmt("uuuu-MM-dd'T'HH:mm:ss"),
        fmt("uuuu-MM-dd'T'HH:mm"), fmt("dd/MM/uuuu HH:mm:ss"), fmt("dd/MM/uuuu HH:mm")
    };
    private static final DateTimeFormatter[] DATES = {fmt("uuuu-MM-dd"), fmt("dd/MM/uuuu")};

    public static class Result {

        public int imported;
        public int duplicates;
        public List<String> errors = new ArrayList<>();
    }

    public static class Parsed {

        public List<ChannelSale> rows = new ArrayList<>();
        public int duplicates;
        public List<String> errors = new ArrayList<>();
    }

    static class Rec {

        final int line;
        final List<String> fields;

        Rec(int line, List<String> fields) {
            this.line = line;
            this.fields = fields;
        }
    }

    private final ChannelSaleDAO dao = new ChannelSaleDAO();

    public Result importCsv(InputStream in, String channel, int actorId) throws IOException, SQLException {
        Parsed parsed = parse(in, channel);
        Result result = new Result();
        result.errors = parsed.errors;
        if (!parsed.errors.isEmpty()) {
            return result;
        }
        result.duplicates = parsed.duplicates;
        try (Connection con = DBUtil.getConnection()) {
            try {
                con.setAutoCommit(false);
                for (ChannelSale s : parsed.rows) {
                    if (dao.existsByRef(con, channel, s.getExternalRef())) {
                        result.duplicates++;
                        continue;
                    }
                    s.setChannel(channel);
                    s.setCreatedBy(actorId);
                    dao.insert(con, s);
                    result.imported++;
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
        return result;
    }

    public static Parsed parse(InputStream in) throws IOException {
        return parse(in, null);
    }

    public static Parsed parse(InputStream in, String channel) throws IOException {
        Parsed out = new Parsed();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
            if (buf.size() > MAX_BYTES) {
                out.errors.add("The file is larger than 2 MB.");
                return out;
            }
        }
        String text = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == (char) 0xFEFF) {
            text = text.substring(1);
        }
        List<Rec> recs = new ArrayList<>();
        if (!readRecords(text, recs, out)) {
            return out;
        }
        if (recs.isEmpty()) {
            out.errors.add("The file is empty.");
            return out;
        }
        List<String> rawHeaders = recs.get(0).fields;
        List<String> normalizedHeaders = new ArrayList<>();
        for (String h : rawHeaders) {
            normalizedHeaders.add(normalize(h));
        }

        ChannelImportAdapter adapter;
        if (channel != null && !channel.trim().isEmpty()) {
            adapter = ChannelAdapterFactory.getAdapter(channel);
        } else {
            adapter = ChannelAdapterFactory.detectAdapter(normalizedHeaders);
        }

        Map<String, String> aliases = adapter.getColumnAliases();
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < normalizedHeaders.size(); i++) {
            String canon = aliases.get(normalizedHeaders.get(i));
            if (canon != null && !col.containsKey(canon)) {
                col.put(canon, i);
            }
        }
        for (String r : REQUIRED) {
            if (!col.containsKey(r)) {
                out.errors.add("Missing column: " + r);
            }
        }
        if (!out.errors.isEmpty()) {
            return out;
        }
        if (recs.size() == 1) {
            out.errors.add("The file has no data rows.");
            return out;
        }
        if (recs.size() - 1 > MAX_ROWS) {
            out.errors.add("The file has more than " + MAX_ROWS + " data rows.");
            return out;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < recs.size(); i++) {
            Rec rec = recs.get(i);
            List<String> problems = new ArrayList<>();
            ChannelSale s = buildRow(rec, col, problems, adapter);
            for (String p : problems) {
                out.errors.add("Line " + rec.line + ": " + p);
            }
            if (problems.isEmpty()) {
                if (seen.add(s.getExternalRef().toLowerCase(Locale.ROOT))) {
                    out.rows.add(s);
                } else {
                    out.duplicates++;
                }
            }
        }
        if (!out.errors.isEmpty()) {
            out.rows.clear();
            out.duplicates = 0;
        }
        return out;
    }

    private static ChannelSale buildRow(Rec rec, Map<String, Integer> col, List<String> problems, ChannelImportAdapter adapter) {
        ChannelSale s = new ChannelSale();
        String ref = cell(rec, col, "order_ref");
        if (ref.length() < 1 || ref.length() > 60) {
            problems.add("order_ref must be 1-60 characters.");
        }
        s.setExternalRef(ref);

        Timestamp at = parseDate(cell(rec, col, "sold_at"));
        if (at == null) {
            problems.add("sold_at must be a date like 2026-10-01 or 01/10/2026.");
        } else if (at.after(new Timestamp(System.currentTimeMillis()))) {
            problems.add("sold_at cannot be in the future.");
        } else {
            s.setSoldAt(at);
        }

        String status = adapter.parseStatus(cell(rec, col, "status"));
        if (status == null) {
            problems.add("status must be COMPLETED, CANCELLED or RETURNED.");
        }
        s.setStatus(status);

        Long goods = parseAmount(cell(rec, col, "goods_amount"));
        if (goods == null || goods <= 0) {
            problems.add("goods_amount must be a whole number from 1 to 50,000,000.");
        }
        Long fee = optional(rec, col, "platform_fee", problems);
        Long ship = optional(rec, col, "shipping_fee", problems);
        Long commission = optional(rec, col, "commission", problems);
        Long cost = optional(rec, col, "cost", problems);
        if (problems.isEmpty()) {
            s.setGoodsAmount(BigDecimal.valueOf(goods));
            s.setPlatformFee(BigDecimal.valueOf(fee));
            s.setShippingFee(BigDecimal.valueOf(ship));
            s.setAffiliateCommission(BigDecimal.valueOf(commission));
            s.setCostAmount(BigDecimal.valueOf(cost));
        }
        return s;
    }

    private static Long optional(Rec rec, Map<String, Integer> col, String name, List<String> problems) {
        String raw = cell(rec, col, name);
        if (raw.isEmpty()) {
            return 0L;
        }
        Long v = parseAmount(raw);
        if (v == null) {
            problems.add(name + " must be a whole number from 0 to 50,000,000, or empty.");
        }
        return v;
    }

    private static String cell(Rec rec, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= rec.fields.size()) {
            return "";
        }
        return rec.fields.get(i).trim();
    }

    static Long parseAmount(String raw) {
        String t = raw.trim();
        if (!t.matches("\\d{1,12}|\\d{1,3}(,\\d{3})+|\\d{1,3}(\\.\\d{3})+")) {
            return null;
        }
        String digits = t.replaceAll("[.,]", "");
        if (digits.length() > 12) {
            return null;
        }
        long v = Long.parseLong(digits);
        return v > MAX_AMOUNT ? null : v;
    }

    static Timestamp parseDate(String raw) {
        String t = raw.trim();
        for (DateTimeFormatter f : DATE_TIMES) {
            try {
                return Timestamp.valueOf(LocalDateTime.parse(t, f));
            } catch (DateTimeParseException e) {
                continue;
            }
        }
        for (DateTimeFormatter f : DATES) {
            try {
                return Timestamp.valueOf(LocalDate.parse(t, f).atStartOfDay());
            } catch (DateTimeParseException e) {
                continue;
            }
        }
        return null;
    }

    public static String normalize(String header) {
        String t = Normalizer.normalize(header, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT).trim();
        return t.replaceAll("[\\s_\\-]+", "_");
    }

    static boolean readRecords(String text, List<Rec> recs, Parsed out) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        int line = 1;
        int recLine = 1;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && text.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                    if (c == '\n') {
                        line++;
                    }
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < len && text.charAt(i + 1) == '\n') {
                    i++;
                }
                endRecord(recs, fields, cur, recLine);
                line++;
                recLine = line;
            } else {
                cur.append(c);
            }
        }
        if (inQuotes) {
            out.errors.add("Line " + recLine + ": a quoted value is not closed.");
            return false;
        }
        if (cur.length() > 0 || !fields.isEmpty()) {
            endRecord(recs, fields, cur, recLine);
        }
        return true;
    }

    private static void endRecord(List<Rec> recs, List<String> fields, StringBuilder cur, int recLine) {
        fields.add(cur.toString());
        cur.setLength(0);
        boolean blank = fields.size() == 1 && fields.get(0).trim().isEmpty();
        if (!blank) {
            recs.add(new Rec(recLine, new ArrayList<>(fields)));
        }
        fields.clear();
    }

    private static DateTimeFormatter fmt(String p) {
        return DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT);
    }
}
