/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

public class Supplier {
    private Integer idSupplier;
    private String namaSupplier;
    private String alamatSupplier;
    private String notelpSupplier;

    public Supplier() {}

    public Supplier(Integer idSupplier, String namaSupplier, String alamatSupplier, String notelpSupplier) {
        this.idSupplier = idSupplier;
        this.namaSupplier = namaSupplier;
        this.alamatSupplier = alamatSupplier;
        this.notelpSupplier = notelpSupplier;
    }

    public Integer getIdSupplier() { return idSupplier; }
    public void setIdSupplier(Integer idSupplier) { this.idSupplier = idSupplier; }

    public String getNamaSupplier() { return namaSupplier; }
    public void setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }

    public String getAlamatSupplier() { return alamatSupplier; }
    public void setAlamatSupplier(String alamatSupplier) { this.alamatSupplier = alamatSupplier; }

    public String getNotelpSupplier() { return notelpSupplier; }
    public void setNotelpSupplier(String notelpSupplier) { this.notelpSupplier = notelpSupplier; }

    @Override
    public String toString() {
        return namaSupplier + (idSupplier == null ? "" : " (id:" + idSupplier + ")");
    }
}
