package controller.sales;

import controller.auth.LoginServlet;
import dao.AffiliateDAO;
import dao.ChannelSaleDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Affiliate;
import model.ChannelSale;
import model.Page;
import model.User;
import model.view.ChannelProfit;
import model.view.DayProfit;
import service.AuditService;
import service.ProfitService;
import util.SqlErrors;
import static util.Params.trim;

@WebServlet(name = "SalesServlet", urlPatterns = {"/sales"})
public class SalesServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/sales/sales.jsp";
    private static final int PAGE_SIZE = 10;
    private static final int MAX_DAYS = 366;
    private static final long MAX_AMOUNT = 50000000L;
    private static final Map<String, String> MESSAGES = new HashMap<>();
    private static final Map<String, String> CHANNEL_NAMES = new LinkedHashMap<>();

    static {
        MESSAGES.put("added", "Sale added.");
        MESSAGES.put("addedwarn", "Sale added. Its costs and fees are higher than the goods amount, so this sale makes a loss.");
        MESSAGES.put("status", "Sale status changed.");
        MESSAGES.put("bad", "Invalid request.");
        CHANNEL_NAMES.put("STORE", "Store");
        CHANNEL_NAMES.put("SHOPEE", "Shopee");
        CHANNEL_NAMES.put("TIKTOK", "TikTok Shop");
    }

    private final ProfitService profitService = new ProfitService();
    private final ChannelSaleDAO saleDAO = new ChannelSaleDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        show(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        try {
            if ("add".equals(action)) {
                add(request, response, actor);
            } else if ("status".equals(action)) {
                changeStatus(request, response, actor);
            } else {
                response.sendRedirect(request.getContextPath() + "/sales?msg=bad");
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot update the sales book", e);
        }
    }

    private void changeStatus(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, IOException {
        Integer id = parseId(request.getParameter("id"));
        String status = trim(request.getParameter("status"));
        String back = backQuery(request);
        if (id == null || !validStatus(status)) {
            response.sendRedirect(request.getContextPath() + "/sales?msg=bad" + back);
            return;
        }
        String msg = "bad";
        if (saleDAO.setStatus(id, status)) {
            AuditService.log(actor, "SALES_STATUS", "CHANNEL_SALE", id, "status", status);
            msg = "status";
        }
        response.sendRedirect(request.getContextPath() + "/sales?msg=" + msg + back);
    }

    private void add(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Map<String, String> f = new HashMap<>();
        for (String k : new String[]{"channel", "ref", "soldOn", "status", "goods", "platformFee", "shippingFee",
            "commission", "cost", "note", "affiliate"}) {
            f.put(k, trim(request.getParameter(k)));
        }
        ChannelSale s = new ChannelSale();

        String channel = f.get("channel");
        if (!validChannel(channel)) {
            errors.put("channel", "Choose Shopee or TikTok Shop.");
        }
        s.setChannel(channel);

        String ref = f.get("ref");
        if (ref.length() < 1 || ref.length() > 60) {
            errors.put("ref", "1-60 characters.");
        }
        s.setExternalRef(ref);

        LocalDate soldOn = parseDate(f.get("soldOn"));
        if (soldOn == null) {
            errors.put("soldOn", "Choose the sold date.");
        } else if (soldOn.isAfter(LocalDate.now())) {
            errors.put("soldOn", "The sold date cannot be in the future.");
        } else {
            s.setSoldAt(soldAt(soldOn));
        }

        String status = f.get("status");
        if (!validStatus(status)) {
            errors.put("status", "Choose a status.");
        }
        s.setStatus(status);

        Long goods = amount(f.get("goods"));
        if (goods == null || goods <= 0) {
            errors.put("goods", "Enter a whole number from 1 to 50,000,000.");
        }
        Long fee = optionalAmount(f.get("platformFee"));
        if (fee == null) {
            errors.put("platformFee", "Enter a whole number from 0 to 50,000,000, or leave empty.");
        }
        Long ship = optionalAmount(f.get("shippingFee"));
        if (ship == null) {
            errors.put("shippingFee", "Enter a whole number from 0 to 50,000,000, or leave empty.");
        }
        Long commission = optionalAmount(f.get("commission"));
        if (commission == null) {
            errors.put("commission", "Enter a whole number from 0 to 50,000,000, or leave empty.");
        }
        Affiliate affiliate = null;
        String rawAffiliate = f.get("affiliate");
        if (!rawAffiliate.isEmpty()) {
            Integer affiliateId = parseId(rawAffiliate);
            affiliate = affiliateId == null ? null : affiliateDAO.find(affiliateId);
            if (affiliate == null || !affiliate.isActive()) {
                affiliate = null;
                errors.put("affiliate", "Choose an active affiliate or None.");
            }
        }
        Long cost = optionalAmount(f.get("cost"));
        if (cost == null) {
            errors.put("cost", "Enter a whole number from 0 to 50,000,000, or leave empty.");
        }

        String note = f.get("note");
        if (note.length() > 200) {
            errors.put("note", "At most 200 characters.");
        }
        s.setNote(note.isEmpty() ? null : note);

        if (errors.isEmpty() && saleDAO.existsByRef(channel, ref)) {
            errors.put("ref", "This order reference already exists for that channel.");
        }

        if (errors.isEmpty()) {
            s.setGoodsAmount(BigDecimal.valueOf(goods));
            s.setPlatformFee(BigDecimal.valueOf(fee));
            s.setShippingFee(BigDecimal.valueOf(ship));
            if (affiliate != null) {
                s.setAffiliateId(affiliate.getAffiliateId());
                if (f.get("commission").isEmpty()) {
                    commission = commissionFor(goods, affiliate.getRate());
                }
            }
            s.setAffiliateCommission(BigDecimal.valueOf(commission));
            s.setCostAmount(BigDecimal.valueOf(cost));
            s.setCreatedBy(actor.getUserId());
            try {
                saleDAO.insert(s);
                AuditService.log(actor, "SALES_ADD", "CHANNEL_SALE", channel + ":" + ref,
                        "goods", String.valueOf(goods), "status", status);
                String msg = fee + ship + commission + cost > goods ? "addedwarn" : "added";
                response.sendRedirect(request.getContextPath() + "/sales?msg=" + msg);
                return;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
                errors.put("ref", "This order reference already exists for that channel.");
            }
        }
        show(request, response, errors, f);
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Map<String, String> errors,
            Map<String, String> form) throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        LocalDate from = parseDate(request.getParameter("from"));
        LocalDate to = parseDate(request.getParameter("to"));
        String rangeError = null;
        boolean given = !trim(request.getParameter("from")).isEmpty() || !trim(request.getParameter("to")).isEmpty();
        if (given && (from == null || to == null)) {
            rangeError = "Enter both dates as yyyy-mm-dd. Showing the last 7 days.";
        } else if (from != null && to != null && from.isAfter(to)) {
            rangeError = "The start date is after the end date. Showing the last 7 days.";
        } else if (from != null && to != null && ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            rangeError = "The range is longer than 366 days. Showing the last 7 days.";
        }
        if (!given || rangeError != null) {
            from = today.minusDays(6);
            to = today;
        }
        String channel = form != null ? "" : trim(request.getParameter("channel"));
        if (!"STORE".equals(channel) && !validChannel(channel)) {
            channel = "";
        }
        String channelOrNull = channel.isEmpty() ? null : channel;
        Timestamp fromTs = Timestamp.valueOf(from.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        try {
            List<DayProfit> days = profitService.byDay(fromTs, toTs, channelOrNull);
            List<ChannelProfit> found = profitService.byChannel(fromTs, toTs);
            List<ChannelProfit> cards = new ArrayList<>();
            for (String name : CHANNEL_NAMES.keySet()) {
                ChannelProfit c = null;
                for (ChannelProfit p : found) {
                    if (name.equals(p.getChannel())) {
                        c = p;
                    }
                }
                if (c == null) {
                    c = new ChannelProfit();
                    c.setChannel(name);
                }
                cards.add(c);
            }

            LocalDate day = parseDate(request.getParameter("day"));
            DayProfit selected = null;
            if (day != null) {
                for (DayProfit d : days) {
                    if (d.getDay().equals(day)) {
                        selected = d;
                    }
                }
            }
            if (selected != null && !"STORE".equals(channel)) {
                Timestamp dayFrom = Timestamp.valueOf(day.atStartOfDay());
                Timestamp dayTo = Timestamp.valueOf(day.plusDays(1).atStartOfDay());
                Page<ChannelSale> sales = saleDAO.search(dayFrom, dayTo, channelOrNull,
                        clampPage(request.getParameter("page")), PAGE_SIZE);
                request.setAttribute("sales", sales);
            }

            request.setAttribute("days", days);
            request.setAttribute("cards", cards);
            request.setAttribute("selected", selected);
            request.setAttribute("nextStep", nextStep(found));
        } catch (SQLException e) {
            throw new ServletException("Cannot load the sales book", e);
        }
        try {
            List<Affiliate> active = new ArrayList<>();
            for (Affiliate a : affiliateDAO.findAll()) {
                if (a.isActive()) {
                    active.add(a);
                }
            }
            request.setAttribute("affiliates", active);
        } catch (SQLException e) {
            throw new ServletException("Cannot load affiliates", e);
        }
        request.setAttribute("channelNames", CHANNEL_NAMES);
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("channel", channel);
        request.setAttribute("today", today.toString());
        request.setAttribute("rangeError", rangeError);
        request.setAttribute("errors", errors == null ? new HashMap<String, String>() : errors);
        request.setAttribute("f", form == null ? new HashMap<String, String>() : form);
        request.setAttribute("selectedAffiliate", form == null ? null : parseId(form.get("affiliate")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private String backQuery(HttpServletRequest request) {
        LocalDate from = parseDate(request.getParameter("from"));
        LocalDate to = parseDate(request.getParameter("to"));
        if (from == null || to == null) {
            return "";
        }
        String channel = trim(request.getParameter("channel"));
        if (!"STORE".equals(channel) && !validChannel(channel)) {
            channel = "";
        }
        LocalDate day = parseDate(request.getParameter("day"));
        return "&from=" + from + "&to=" + to + "&channel=" + channel + (day == null ? "" : "&day=" + day);
    }

    static String nextStep(List<ChannelProfit> found) {
        List<String> losing = new ArrayList<>();
        boolean anyOnline = false;
        for (ChannelProfit c : found) {
            if (!ProfitService.STORE.equals(c.getChannel())) {
                anyOnline = true;
            }
            if (c.getProfit().signum() < 0) {
                losing.add(CHANNEL_NAMES.containsKey(c.getChannel()) ? CHANNEL_NAMES.get(c.getChannel()) : c.getChannel());
            }
        }
        if (!losing.isEmpty()) {
            return "Check " + String.join(" and ", losing) + ". It lost money in this period. Look at its fees, shipping and costs.";
        }
        if (!anyOnline) {
            return "Add your first online order below.";
        }
        return "Every channel made a profit in this period. Keep adding your online orders so the numbers stay true.";
    }

    static int clampPage(String raw) {
        try {
            int p = Integer.parseInt(trim(raw));
            return Math.max(1, Math.min(p, 100000));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    static boolean validChannel(String c) {
        return "SHOPEE".equals(c) || "TIKTOK".equals(c);
    }

    static boolean validStatus(String s) {
        return "COMPLETED".equals(s) || "CANCELLED".equals(s) || "RETURNED".equals(s);
    }

    static Integer parseId(String raw) {
        try {
            return Integer.valueOf(trim(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(trim(raw));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static Long amount(String raw) {
        String digits = trim(raw).replaceAll("[.,\\s]", "");
        if (!digits.matches("\\d{1,9}")) {
            return null;
        }
        long v = Long.parseLong(digits);
        return v > MAX_AMOUNT ? null : v;
    }

    static long commissionFor(long goods, BigDecimal rate) {
        return BigDecimal.valueOf(goods).multiply(rate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();
    }

    static Long optionalAmount(String raw) {
        return trim(raw).isEmpty() ? Long.valueOf(0) : amount(raw);
    }

    private static Timestamp soldAt(LocalDate d) {
        if (d.equals(LocalDate.now())) {
            return Timestamp.valueOf(LocalDateTime.now());
        }
        return Timestamp.valueOf(d.atStartOfDay());
    }
}
