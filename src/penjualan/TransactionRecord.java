package penjualan;

import java.math.BigDecimal;

/**
 * Model untuk header transaksi.
 */
public class TransactionRecord {
    private long idTransaksi;
    private String kodeTransaksi;
    private String tglTransaksi;
    private BigDecimal totalHarga;
    private BigDecimal totalBayar;
    private BigDecimal kembalian;
    private String paymentMethod;
    private Integer idVoucher;

    // baru: id_pengguna (TEXT) dan nama_kasir
    private String idPengguna;
    private String namaKasir;
    private String namaGuru;



    // ----------------- constructor lama (kompatibilitas) -----------------
//    public TransactionRecord(long idTransaksi, String kodeTransaksi, String tglTransaksi,
//                             BigDecimal totalHarga, BigDecimal totalBayar, BigDecimal kembalian,
//                             String paymentMethod, Integer idVoucher) {
//        this(idTransaksi, kodeTransaksi, tglTransaksi, totalHarga, totalBayar, kembalian, paymentMethod, idVoucher, null, null);
//    }

    // ----------------- constructor baru (lengkap) -----------------
public TransactionRecord(long id, String kode, String tgl, BigDecimal totalHarga,
                         BigDecimal totalBayar, BigDecimal kembalian, String paymentMethod,
                         Integer idVoucher, String idPengguna, String namaKasir, String namaGuru) {
    this.idTransaksi = id;
    this.kodeTransaksi = kode;
    this.tglTransaksi = tgl;
    this.totalHarga = totalHarga;
    this.totalBayar = totalBayar;
    this.kembalian = kembalian;
    this.paymentMethod = paymentMethod;
    this.idVoucher = idVoucher;
    this.idPengguna = idPengguna;
    this.namaKasir = namaKasir;
    this.namaGuru = namaGuru;
}

    // ----------------- getters & setters -----------------
    public long getIdTransaksi() { return idTransaksi; }
    public void setIdTransaksi(long idTransaksi) { this.idTransaksi = idTransaksi; }

    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }

    public String getTglTransaksi() { return tglTransaksi; }
    public void setTglTransaksi(String tglTransaksi) { this.tglTransaksi = tglTransaksi; }

    public BigDecimal getTotalHarga() { return totalHarga; }
    public void setTotalHarga(BigDecimal totalHarga) { this.totalHarga = totalHarga; }

    public BigDecimal getTotalBayar() { return totalBayar; }
    public void setTotalBayar(BigDecimal totalBayar) { this.totalBayar = totalBayar; }

    public BigDecimal getKembalian() { return kembalian; }
    public void setKembalian(BigDecimal kembalian) { this.kembalian = kembalian; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getIdVoucher() { return idVoucher; }
    public void setIdVoucher(Integer idVoucher) { this.idVoucher = idVoucher; }

    public String getIdPengguna() { return idPengguna; }
    public void setIdPengguna(String idPengguna) { this.idPengguna = idPengguna; }

    public String getNamaKasir() { return namaKasir; }
    public void setNamaKasir(String namaKasir) { this.namaKasir = namaKasir; }
    
    public String getNamaGuru() { return namaGuru; }
public void setNamaGuru(String namaGuru) { this.namaGuru = namaGuru; }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "idTransaksi=" + idTransaksi +
                ", kodeTransaksi='" + kodeTransaksi + '\'' +
                ", tglTransaksi='" + tglTransaksi + '\'' +
                ", totalHarga=" + totalHarga +
                ", totalBayar=" + totalBayar +
                ", kembalian=" + kembalian +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", idVoucher=" + idVoucher +
                ", idPengguna='" + idPengguna + '\'' +
                ", namaKasir='" + namaKasir + '\'' +
                '}';
    }
}
