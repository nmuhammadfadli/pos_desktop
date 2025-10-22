/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import java.math.BigDecimal;
public class Voucher {
    private int idVoucher;
    private String kode;
    private BigDecimal currentBalance;
    private String bulan;
    private int idGuru;
    private String namaGuru;

    public int getIdVoucher() { return idVoucher; }
    public void setIdVoucher(int idVoucher) { this.idVoucher = idVoucher; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    

    public int getIdGuru() { return idGuru; }
    public void setIdGuru(int idGuru) { this.idGuru = idGuru; }
    
    public String getBulan(){return bulan;}
    public void setBulan (String bulan){ this.bulan = bulan;}

    public String getNamaGuru() { return namaGuru; }
    public void setNamaGuru(String namaGuru) { this.namaGuru = namaGuru; }
}
