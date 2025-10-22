package testsqlite;

import java.math.BigDecimal;

public class TransactionRecord {
    private long idTransaksi;
    private String kodeTransaksi;
    private String tglTransaksi;
    private BigDecimal totalHarga;
    private BigDecimal totalBayar;
    private BigDecimal kembalian;
    private String paymentMethod;
    private Integer idVoucher; 
    public TransactionRecord() {}

    public TransactionRecord(long idTransaksi, String kodeTransaksi, String tglTransaksi,
                             BigDecimal totalHarga, BigDecimal totalBayar, BigDecimal kembalian,
                             String paymentMethod, Integer idVoucher) {
        this.idTransaksi = idTransaksi;
        this.kodeTransaksi = kodeTransaksi;
        this.tglTransaksi = tglTransaksi;
        this.totalHarga = totalHarga;
        this.totalBayar = totalBayar;
        this.kembalian = kembalian;
        this.paymentMethod = paymentMethod;
        this.idVoucher = idVoucher;
    }

    public long getIdTransaksi() { return idTransaksi; }
    public void setIdTransaksi(long idTransaksi) { this.idTransaksi = idTransaksi; }
    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }
    public String getTglTransaksi() { return tglTransaksi; }
    public void setTglTransaksi(String tglTransaksi) { this.tglTransaksi = tglTransaksi; }
    public BigDecimal getTotalHarga() { return totalHarga; }
    public void setTotalHarga(BigDecimal totalHarga) { this.totalHarga = totalHarga; }
    public BigDecimal getTotalBayar() { return totalBayar; }
    public void setTotalBayar(BigDecimal totalBayar) { this.totalBayar = totalBayar; }
    public BigDecimal getKembalian() { return kembalian; }
    public void setKembalian(BigDecimal kembalian) { this.kembalian = kembalian; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Integer getIdVoucher() { return idVoucher; }
    public void setIdVoucher(Integer idVoucher) { this.idVoucher = idVoucher; }
}
