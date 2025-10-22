package testsqlite;

import java.math.BigDecimal;

public class DetailBarang {
    private int id;
    private String barcode;
    private int stok;
    private BigDecimal hargaJual;
    private String tanggalExp;
    private int idBarang;
       // bisa null jika belum diketahui
    private Integer idDetailPembelian;    // bisa null jika bukan berasal dari pembelian

    public DetailBarang() {}

    // getters/setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getStok() { return stok; }
    public void setStok(int stok) { this.stok = stok; }

    public BigDecimal getHargaJual() { return hargaJual; }
    public void setHargaJual(BigDecimal hargaJual) { this.hargaJual = hargaJual; }

    public String getTanggalExp() { return tanggalExp; }
    public void setTanggalExp(String tanggalExp) { this.tanggalExp = tanggalExp; }

    public int getIdBarang() { return idBarang; }
    public void setIdBarang(int idBarang) { this.idBarang = idBarang; }


    public Integer getIdDetailPembelian() { return idDetailPembelian; }
    public void setIdDetailPembelian(Integer idDetailPembelian) { this.idDetailPembelian = idDetailPembelian; }
}
