/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import java.math.BigDecimal;

public class TransactionItem {
    private int idDetailPenjualan;
    private int idDetailBarang;
    private int jumlahBarang;
    private BigDecimal hargaUnit;
    private BigDecimal subtotal;

    public TransactionItem() {}

    public int getIdDetailPenjualan() { return idDetailPenjualan; }
    public void setIdDetailPenjualan(int idDetailPenjualan) { this.idDetailPenjualan = idDetailPenjualan; }
    public int getIdDetailBarang() { return idDetailBarang; }
    public void setIdDetailBarang(int idDetailBarang) { this.idDetailBarang = idDetailBarang; }
    public int getJumlahBarang() { return jumlahBarang; }
    public void setJumlahBarang(int jumlahBarang) { this.jumlahBarang = jumlahBarang; }
    public BigDecimal getHargaUnit() { return hargaUnit; }
    public void setHargaUnit(BigDecimal hargaUnit) { this.hargaUnit = hargaUnit; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
