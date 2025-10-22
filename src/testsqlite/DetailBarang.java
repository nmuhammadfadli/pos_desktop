package testsqlite;

import java.math.BigDecimal;

/**
 * Model untuk row detail_barang (batch).
 */
public class DetailBarang {
    private int id;
    private String barcode;
    private int stok;
    private BigDecimal hargaJual;
    private String tanggalExp;
    private int idBarang;
    private String namaBarang;

    private Integer idSupplier;
    private Integer idDetailPembelian;
    
    private String namaSupplier;



    public DetailBarang() {}
    

    // getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getNamaBarang() { return namaBarang; }
public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }

public String getNamaSupplier() { return namaSupplier; }
public void setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }


    public int getStok() { return stok; }
    public void setStok(int stok) { this.stok = stok; }

    public BigDecimal getHargaJual() { return hargaJual; }
    public void setHargaJual(BigDecimal hargaJual) { this.hargaJual = hargaJual; }

    public String getTanggalExp() { return tanggalExp; }
    public void setTanggalExp(String tanggalExp) { this.tanggalExp = tanggalExp; }

    public int getIdBarang() { return idBarang; }
    public void setIdBarang(int idBarang) { this.idBarang = idBarang; }

    public Integer getIdSupplier() { return idSupplier; }
    public void setIdSupplier(Integer idSupplier) { this.idSupplier = idSupplier; }
    


    public Integer getIdDetailPembelian() { return idDetailPembelian; }
    public void setIdDetailPembelian(Integer idDetailPembelian) { this.idDetailPembelian = idDetailPembelian; }
}
