package model;

public class Attribution {

    private final Affiliate affiliate;
    private final Campaign campaign;
    private final long eligibleAmount;
    private final long commission;
    private final String rateText;
    private final String snapshot;

    public Attribution(Affiliate affiliate, Campaign campaign, long eligibleAmount, long commission, String rateText, String snapshot) {
        this.affiliate = affiliate;
        this.campaign = campaign;
        this.eligibleAmount = eligibleAmount;
        this.commission = commission;
        this.rateText = rateText;
        this.snapshot = snapshot;
    }

    public Affiliate getAffiliate() {
        return affiliate;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public long getEligibleAmount() {
        return eligibleAmount;
    }

    public long getCommission() {
        return commission;
    }

    public String getRateText() {
        return rateText;
    }

    public String getSnapshot() {
        return snapshot;
    }
}
