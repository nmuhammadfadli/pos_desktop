/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import java.math.BigDecimal;

public class Receivable {
    private int idReceivable;
    private long idTransaksi;
    private BigDecimal amountTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountOutstanding;
    private String createdAt;
    private String status; // OPEN / PARTIAL / PAID / CLOSED

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
}
