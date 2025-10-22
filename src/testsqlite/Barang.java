package testsqlite;

public class Barang {
    private int id;
    private String nama;
    private String idKategori;
    private String namaKategori; 

    public Barang() {}
    public Barang(int id, String nama, String idKategori) {
        this.id = id;
        this.nama = nama;
        this.idKategori = idKategori;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getIdKategori() { return idKategori; }
    public void setIdKategori(String idKategori) { this.idKategori = idKategori; }

    public String getNamaKategori() { return namaKategori; }
    public void setNamaKategori(String namaKategori) { this.namaKategori = namaKategori; }

    @Override
    public String toString() {
        return nama + " (" + idKategori + ")";
    }
}
