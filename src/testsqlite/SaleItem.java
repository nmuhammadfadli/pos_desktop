/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;
import java.math.BigDecimal;
public class SaleItem {
    private int idDetailBarang;
    private int qty;
    private BigDecimal price;
    public SaleItem(int idDetailBarang, int qty, BigDecimal price) {
        this.idDetailBarang = idDetailBarang; this.qty = qty; this.price = price;
    }
    public int getIdDetailBarang() { return idDetailBarang; }
    public int getQty() { return qty; }
    public BigDecimal getPrice() { return price; }
}
