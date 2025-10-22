/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package testsqlite;

public class Guru {
    private int idGuru;
    private String namaGuru;
    private String notelpGuru;
    private String jabatan;
    

    public Guru() {}

    public Guru(int idGuru, String namaGuru,  String notelpGuru, String jabatan) {
        this.idGuru = idGuru;
        this.namaGuru = namaGuru;
        this.notelpGuru = notelpGuru;
        this.jabatan = jabatan;
    }

    // getters & setters
    public int getIdGuru() { return idGuru; }
    public void setIdGuru(int idGuru) { this.idGuru = idGuru; }

    public String getNamaGuru() { return namaGuru; }
    public void setNamaGuru(String namaGuru) { this.namaGuru = namaGuru; }


    public String getNotelpGuru() { return notelpGuru; }
    public void setNotelpGuru(String notelpGuru) { this.notelpGuru = notelpGuru; }

    public String getJabatan() { return jabatan; }
    public void setJabatan(String jabatan) { this.jabatan = jabatan; }
}
