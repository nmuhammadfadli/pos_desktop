/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Pembelian;

public class DetailPembelian {
    private Integer idDetailPembelian;
    private Integer hargaBeli;
    private Integer stok;
    private Integer subtotal;
    private Integer idSupplier;
    private String idPembelian; // header id
    private Integer idBarang;

    public DetailPembelian() {}

    public Integer getIdDetailPembelian() { return idDetailPembelian; }
    public void setIdDetailPembelian(Integer idDetailPembelian) { this.idDetailPembelian = idDetailPembelian; }

    public Integer getHargaBeli() { return hargaBeli; }
    public void setHargaBeli(Integer hargaBeli) { this.hargaBeli = hargaBeli; }

    public Integer getStok() { return stok; }
    public void setStok(Integer stok) { this.stok = stok; }

    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }

    public Integer getIdSupplier() { return idSupplier; }
    public void setIdSupplier(Integer idSupplier) { this.idSupplier = idSupplier; }

    public String getIdPembelian() { return idPembelian; }
    public void setIdPembelian(String idPembelian) { this.idPembelian = idPembelian; }

    public Integer getIdBarang() { return idBarang; }
    public void setIdBarang(Integer idBarang) { this.idBarang = idBarang; }
}
