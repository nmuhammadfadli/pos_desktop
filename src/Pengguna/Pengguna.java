/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Pengguna;

/**
 *
 * @author COMPUTER
 */
public class Pengguna {
    
    private String idPengguna;        // id_pengguna (varchar, example USR001)
    private String username;
    private String password;
    private String alamat;
    private String jabatan;
    private String namaLengkap;
    private Integer hakAkses;         // int
    private String email;
    private String notelpPengguna;

    public String getIdPengguna() { return idPengguna; }
    public void setIdPengguna(String idPengguna) { this.idPengguna = idPengguna; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public String getJabatan() { return jabatan; }
    public void setJabatan(String jabatan) { this.jabatan = jabatan; }

    public String getNamaLengkap() { return namaLengkap; }
    public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }

    public Integer getHakAkses() { return hakAkses; }
    public void setHakAkses(Integer hakAkses) { this.hakAkses = hakAkses; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNotelpPengguna() { return notelpPengguna; }
    public void setNotelpPengguna(String notelpPengguna) { this.notelpPengguna = notelpPengguna; }
}
