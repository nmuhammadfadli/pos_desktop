package testsqlite;

import java.util.ArrayList;
import java.util.List;

public class Pembelian {
    private String idPembelian;
    private String tglPembelian;
    private String paymentMethod;        // NEW
    private Integer totalHarga;
    private List<DetailPembelian> details = new ArrayList<>();

    public Pembelian() {}

    public String getIdPembelian() { return idPembelian; }
    public void setIdPembelian(String idPembelian) { this.idPembelian = idPembelian; }

    public String getTglPembelian() { return tglPembelian; }
    public void setTglPembelian(String tglPembelian) { this.tglPembelian = tglPembelian; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getTotalHarga() { return totalHarga; }
    public void setTotalHarga(Integer totalHarga) { this.totalHarga = totalHarga; }

    public List<DetailPembelian> getDetails() { return details; }
    public void setDetails(List<DetailPembelian> details) { this.details = details; }
}
