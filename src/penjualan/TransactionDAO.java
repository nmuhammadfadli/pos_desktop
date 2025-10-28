package penjualan;

import Barang.DetailBarangDAO;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import testsqlite.DatabaseHelper;

/**
 * TransactionDAO: proses penjualan dan query riwayat transaksi
 * Kompatibel NetBeans 8.2 (tanpa text block)
 */
public class TransactionDAO {
    private DetailBarangDAO detailDao = new DetailBarangDAO();

    /**
     * Proses penjualan secara atomik
     */
    public void processSale(List<SaleItem> items, Integer voucherId, BigDecimal cashPaid, 
                            String kodeTrans, String idPengguna) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada item transaksi.");
        }
        if (cashPaid == null) cashPaid = BigDecimal.ZERO;

        // validasi awal
        for (SaleItem it : items) {
            if (it == null) throw new IllegalArgumentException("Item transaksi null.");
            if (it.getQty() <= 0) throw new IllegalArgumentException("Qty harus > 0 untuk idDetailBarang=" + it.getIdDetailBarang());
            if (it.getPrice() == null) throw new IllegalArgumentException("Harga null untuk idDetailBarang=" + it.getIdDetailBarang());
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
            } catch (Throwable ignore) {}

            conn.setAutoCommit(false);
            try {
                // Hitung total harga
                BigDecimal totalHarga = BigDecimal.ZERO;
                for (SaleItem it : items) {
                    totalHarga = totalHarga.add(it.getPrice().multiply(BigDecimal.valueOf(it.getQty())));
                }

                // ============ VOUCHER ============
                BigDecimal usedFromVoucher = BigDecimal.ZERO;
                BigDecimal voucherBalance = BigDecimal.ZERO;

                if (voucherId != null) {
                    String sel = "SELECT current_balance FROM kode_voucher WHERE id_voucher = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sel)) {
                        ps.setInt(1, voucherId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String s = rs.getString("current_balance");
                                voucherBalance = (s == null || s.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s);
                            } else {
                                throw new SQLException("Voucher dengan id " + voucherId + " tidak ditemukan.");
                            }
                        }
                    }

                    usedFromVoucher = voucherBalance.min(totalHarga);
                    if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newBal = voucherBalance.subtract(usedFromVoucher);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE kode_voucher SET current_balance = ? WHERE id_voucher = ?")) {
                            ps.setString(1, newBal.toPlainString());
                            ps.setInt(2, voucherId);
                            ps.executeUpdate();
                        }
                    }
                }

                // ============ HITUNG SISA, BAYAR, KEMBALIAN ============
                BigDecimal sisa = totalHarga.subtract(usedFromVoucher);
                BigDecimal totalBayar = usedFromVoucher.add(cashPaid);
                BigDecimal kembalian = BigDecimal.ZERO;
                if (cashPaid.compareTo(sisa) >= 0) {
                    kembalian = cashPaid.subtract(sisa);
                    sisa = BigDecimal.ZERO;
                } else {
                    sisa = sisa.subtract(cashPaid);
                }

                String paymentMethod = "CASH";
                if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0 && cashPaid.compareTo(BigDecimal.ZERO) == 0)
                    paymentMethod = "VOUCHER";
                else if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0 && cashPaid.compareTo(BigDecimal.ZERO) > 0)
                    paymentMethod = "MIX";
                else if (usedFromVoucher.compareTo(BigDecimal.ZERO) == 0 && cashPaid.compareTo(BigDecimal.ZERO) == 0)
                    paymentMethod = "CREDIT";

                // ============ INSERT transaksi_penjualan ============
                long idTrans;
                String insTrans = "INSERT INTO transaksi_penjualan " +
                        "(kode_transaksi, total_harga, total_bayar, kembalian, payment_method, id_voucher, id_pengguna) " +
                        "VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insTrans, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, kodeTrans);
                    ps.setString(2, totalHarga.toPlainString());
                    ps.setString(3, totalBayar.toPlainString());
                    ps.setString(4, kembalian.toPlainString());
                    ps.setString(5, paymentMethod);
                    if (voucherId != null) ps.setInt(6, voucherId);
                    else ps.setNull(6, Types.INTEGER);
                    if (idPengguna != null) ps.setString(7, idPengguna);
                    else ps.setNull(7, Types.VARCHAR);
                    ps.executeUpdate();

                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) idTrans = gk.getLong(1);
                        else throw new SQLException("Gagal mendapatkan id transaksi.");
                    }
                }

                // ============ INSERT detail_penjualan & update stok ============
                String insDetail = "INSERT INTO detail_penjualan " +
                        "(id_transaksi, id_detail_barang, jumlah_barang, harga_unit, subtotal) VALUES (?,?,?,?,?)";
                try (PreparedStatement psIns = conn.prepareStatement(insDetail)) {
                    for (SaleItem it : items) {
                        detailDao.decreaseStock(conn, it.getIdDetailBarang(), it.getQty());

                        BigDecimal subtotal = it.getPrice().multiply(BigDecimal.valueOf(it.getQty()));
                        psIns.setLong(1, idTrans);
                        psIns.setInt(2, it.getIdDetailBarang());
                        psIns.setInt(3, it.getQty());
                        psIns.setString(4, it.getPrice().toPlainString());
                        psIns.setString(5, subtotal.toPlainString());
                        psIns.executeUpdate();
                    }
                }

                // ============ record voucher_usage ============
                if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0 && voucherId != null) {
                    String insUsage = "INSERT INTO voucher_usage (id_voucher, id_transaksi, used_amount) VALUES (?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(insUsage)) {
                        ps.setInt(1, voucherId);
                        ps.setLong(2, idTrans);
                        ps.setString(3, usedFromVoucher.toPlainString());
                        ps.executeUpdate();
                    }
                }

                // ============ buat receivable (piutang) ============
                if (sisa.compareTo(BigDecimal.ZERO) > 0) {
                    String insReceivable = "INSERT INTO receivable " +
                            "(id_transaksi, amount_total, amount_paid, amount_outstanding, status) VALUES (?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(insReceivable)) {
                        ps.setLong(1, idTrans);
                        ps.setString(2, totalHarga.toPlainString());
                        ps.setString(3, totalBayar.toPlainString());
                        ps.setString(4, sisa.toPlainString());
                        ps.setString(5, "OPEN");
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (Exception ex) {
                try { conn.rollback(); } catch (Throwable t) {}
                throw ex;
            } finally {
                try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            }
        }
    }

    /**
     * Ambil semua transaksi (header)
     */
    public List<TransactionRecord> findAllTransactions() throws SQLException {
        List<TransactionRecord> list = new ArrayList<>();
        String sql = "SELECT " +
                "t.id_transaksi, t.kode_transaksi, t.tgl_transaksi, " +
                "t.total_harga, t.total_bayar, t.kembalian, t.payment_method, " +
                "t.id_voucher, t.id_pengguna, " +
                "p.nama_lengkap AS nama_kasir, g.nama_guru AS nama_guru " +
                "FROM transaksi_penjualan t " +
                "LEFT JOIN data_pengguna p ON t.id_pengguna = p.id_pengguna " +
                "LEFT JOIN kode_voucher v ON t.id_voucher = v.id_voucher " +
                "LEFT JOIN data_guru g ON v.id_guru = g.id_guru " +
                "ORDER BY t.tgl_transaksi DESC, t.id_transaksi DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TransactionRecord tr = new TransactionRecord(
                        rs.getLong("id_transaksi"),
                        rs.getString("kode_transaksi"),
                        rs.getString("tgl_transaksi"),
                        new BigDecimal(rs.getString("total_harga") == null ? "0" : rs.getString("total_harga")),
                        new BigDecimal(rs.getString("total_bayar") == null ? "0" : rs.getString("total_bayar")),
                        new BigDecimal(rs.getString("kembalian") == null ? "0" : rs.getString("kembalian")),
                        rs.getString("payment_method"),
                        rs.wasNull() ? null : rs.getInt("id_voucher"),
                        rs.getString("id_pengguna"),
                        rs.getString("nama_kasir"),
                        rs.getString("nama_guru")
                );
                list.add(tr);
            }
        }
        return list;
    }

    /**
     * Ambil detail item berdasarkan id_transaksi
     */
    public List<TransactionItem> findItemsByTransaction(long idTransaksi) throws SQLException {
        List<TransactionItem> items = new ArrayList<>();
        String sql = "SELECT d.id_detail_penjualan, d.id_detail_barang, b.nama, " +
                "d.jumlah_barang, d.harga_unit, d.subtotal " +
                "FROM detail_penjualan d " +
                "LEFT JOIN detail_barang db ON d.id_detail_barang = db.id_detail_barang " +
                "LEFT JOIN barang b ON db.id_barang = b.id " +
                "WHERE d.id_transaksi = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionItem it = new TransactionItem();
                    it.setIdDetailPenjualan(rs.getInt("id_detail_penjualan"));
                    it.setIdDetailBarang(rs.getInt("id_detail_barang"));
                    it.setNamaBarang(rs.getString("nama"));
                    it.setJumlahBarang(rs.getInt("jumlah_barang"));
                    String harga = rs.getString("harga_unit");
                    String sub = rs.getString("subtotal");
                    it.setHargaUnit(harga == null ? BigDecimal.ZERO : new BigDecimal(harga));
                    it.setSubtotal(sub == null ? BigDecimal.ZERO : new BigDecimal(sub));
                    items.add(it);
                }
            }
        }
        return items;
    }
}
