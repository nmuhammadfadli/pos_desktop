package Hutang;

import java.math.BigDecimal;

public class Receivable {
    private int idReceivable;
    private long idTransaksi;
    private BigDecimal amountTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountOutstanding;
    private String createdAt;
    private String status; // OPEN / PARTIAL / PAID / CLOSED

    // tambahan
    private Integer voucherId;    // bisa null
    private String voucherCode;   // kode voucher jika ada
    private String ownerName;     // nama guru pemilik voucher jika ada

    public int getIdReceivable() { return idReceivable; }
    public void setIdReceivable(int idReceivable) { this.idReceivable = idReceivable; }

    public long getIdTransaksi() { return idTransaksi; }
    public void setIdTransaksi(long idTransaksi) { this.idTransaksi = idTransaksi; }

    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getAmountOutstanding() { return amountOutstanding; }
    public void setAmountOutstanding(BigDecimal amountOutstanding) { this.amountOutstanding = amountOutstanding; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // getters/setters tambahan
    public Integer getVoucherId() { return voucherId; }
    public void setVoucherId(Integer voucherId) { this.voucherId = voucherId; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
