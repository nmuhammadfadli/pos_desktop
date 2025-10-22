package testsqlite;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionDAO: processSale + query history (findAllTransactions, findItemsByTransaction)
 */
public class TransactionDAO {
    private DetailBarangDAO detailDao = new DetailBarangDAO();

    /**
     * Proses penjualan atomik (sudah dijelaskan sebelumnya)
     */
    public void processSale(List<SaleItem> items, Integer voucherId, BigDecimal cashPaid, String kodeTrans) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada item transaksi.");
        }
        if (cashPaid == null) cashPaid = BigDecimal.ZERO;

        // Validasi awal per item
        for (SaleItem it : items) {
            if (it == null) throw new IllegalArgumentException("Item transaksi null.");
            if (it.getQty() <= 0) throw new IllegalArgumentException("Qty harus > 0 untuk idDetailBarang=" + it.getIdDetailBarang());
            if (it.getPrice() == null) throw new IllegalArgumentException("Harga tidak boleh null untuk idDetailBarang=" + it.getIdDetailBarang());
            if (it.getPrice().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Harga harus >= 0 untuk idDetailBarang=" + it.getIdDetailBarang());
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            // pastikan FK diaktifkan (jaga-jaga)
            try (Statement s = conn.createStatement()) {
                try { s.execute("PRAGMA foreign_keys = ON"); } catch (Throwable ignore) {}
            } catch (Throwable ignore) {}

            conn.setAutoCommit(false);
            try {
                // hitung total harga
                BigDecimal totalHarga = BigDecimal.ZERO;
                for (SaleItem it : items) {
                    BigDecimal line = it.getPrice().multiply(BigDecimal.valueOf(it.getQty()));
                    totalHarga = totalHarga.add(line);
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
                    // gunakan voucher sebanyak min(voucherBalance, totalHarga)
                    usedFromVoucher = voucherBalance.min(totalHarga);
                    if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newBal = voucherBalance.subtract(usedFromVoucher);
                        try (PreparedStatement ps = conn.prepareStatement("UPDATE kode_voucher SET current_balance = ? WHERE id_voucher = ?")) {
                            ps.setString(1, newBal.toPlainString());
                            ps.setInt(2, voucherId);
                            ps.executeUpdate();
                        }
                    }
                }

                // ============ HITUNG SISA, BAYAR, KEMBALIAN ============
                BigDecimal sisa = totalHarga.subtract(usedFromVoucher); // tersisa untuk dibayar tunai / piutang
                BigDecimal totalBayar = usedFromVoucher.add(cashPaid); // total yg dibayar sekarang (voucher + cash)
                BigDecimal kembalian = BigDecimal.ZERO;
                if (cashPaid.compareTo(sisa) >= 0) {
                    // tunai cukup/lebih -> ada kembalian, sisa jadi 0
                    kembalian = cashPaid.subtract(sisa);
                    sisa = BigDecimal.ZERO;
                } else {
                    // tunai kurang -> sisa menjadi outstanding
                    sisa = sisa.subtract(cashPaid);
                }

                String paymentMethod = "CASH";
                if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0 && cashPaid.compareTo(BigDecimal.ZERO) == 0) paymentMethod = "VOUCHER";
                else if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0 && cashPaid.compareTo(BigDecimal.ZERO) > 0) paymentMethod = "MIX";
                else if (usedFromVoucher.compareTo(BigDecimal.ZERO) == 0 && cashPaid.compareTo(BigDecimal.ZERO) == 0) paymentMethod = "CREDIT";

                // ============ INSERT transaksi_penjualan ============
                long idTrans;
                String insTrans = "INSERT INTO transaksi_penjualan (kode_transaksi, total_harga, total_bayar, kembalian, payment_method, id_voucher) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insTrans, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, kodeTrans);
                    ps.setString(2, totalHarga.toPlainString());
                    ps.setString(3, totalBayar.toPlainString());
                    ps.setString(4, kembalian.toPlainString());
                    ps.setString(5, paymentMethod);
                    if (voucherId != null) ps.setInt(6, voucherId);
                    else ps.setNull(6, Types.INTEGER);
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) idTrans = gk.getLong(1);
                        else throw new SQLException("Gagal mendapatkan id transaksi (generated keys).");
                    }
                }

                // ============ INSERT detail_penjualan & UPDATE stok (atomic) ============
                String insDetail = "INSERT INTO detail_penjualan (id_transaksi, id_detail_barang, jumlah_barang, harga_unit, subtotal) VALUES (?,?,?,?,?)";
                try (PreparedStatement psIns = conn.prepareStatement(insDetail)) {
                    for (SaleItem it : items) {
                        // cek & kurangi stok via DAO dengan connection yg sama (akan throw SQLException jika stok tidak cukup)
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

                // ============ record voucher_usage jika ada penggunaan voucher ============
                if (usedFromVoucher.compareTo(BigDecimal.ZERO) > 0 && voucherId != null) {
                    String insUsage = "INSERT INTO voucher_usage (id_voucher, id_transaksi, used_amount) VALUES (?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(insUsage)) {
                        ps.setInt(1, voucherId);
                        ps.setLong(2, idTrans);
                        ps.setString(3, usedFromVoucher.toPlainString());
                        ps.executeUpdate();
                    }
                }

                // ============ buat receivable (piutang) jika sisa > 0 ============
                if (sisa.compareTo(BigDecimal.ZERO) > 0) {
                    String insReceivable = "INSERT INTO receivable (id_transaksi, amount_total, amount_paid, amount_outstanding, status) VALUES (?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(insReceivable)) {
                        ps.setLong(1, idTrans);
                        ps.setString(2, totalHarga.toPlainString());
                        ps.setString(3, totalBayar.toPlainString());
                        ps.setString(4, sisa.toPlainString());
                        ps.setString(5, "OPEN");
                        ps.executeUpdate();
                    }
                }

                // commit jika semua berhasil
                conn.commit();
            } catch (Exception ex) {
                // rollback jika error
                try { conn.rollback(); } catch (Throwable t) { /* ignore */ }
                throw ex;
            } finally {
                // kembalikan auto-commit (jika koneksi masih terbuka)
                try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            }
        }
    }

    /**
     * Ambil semua transaksi (header) dalam urutan paling baru dulu.
     */
    public List<TransactionRecord> findAllTransactions() throws SQLException {
        List<TransactionRecord> list = new ArrayList<>();
        String sql = "SELECT id_transaksi, kode_transaksi, tgl_transaksi, total_harga, total_bayar, kembalian, payment_method, id_voucher FROM transaksi_penjualan ORDER BY tgl_transaksi DESC, id_transaksi DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id_transaksi");
                String kode = rs.getString("kode_transaksi");
                String tgl = rs.getString("tgl_transaksi");
                String th = rs.getString("total_harga");
                String tb = rs.getString("total_bayar");
                String kemb = rs.getString("kembalian");
                String pm = rs.getString("payment_method");
                int idv = rs.getInt("id_voucher");
                Integer idVoucher = rs.wasNull() ? null : idv;

                TransactionRecord tr = new TransactionRecord(
                        id,
                        kode,
                        tgl,
                        th == null ? BigDecimal.ZERO : new BigDecimal(th),
                        tb == null ? BigDecimal.ZERO : new BigDecimal(tb),
                        kemb == null ? BigDecimal.ZERO : new BigDecimal(kemb),
                        pm,
                        idVoucher
                );
                list.add(tr);
            }
        }
        return list;
    }

    /**
     * Ambil baris-detail (item) untuk satu transaksi
     */
    public List<TransactionItem> findItemsByTransaction(long idTransaksi) throws SQLException {
        List<TransactionItem> items = new ArrayList<>();
        String sql = "SELECT id_detail_penjualan, id_detail_barang, jumlah_barang, harga_unit, subtotal FROM detail_penjualan WHERE id_transaksi = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionItem it = new TransactionItem();
                    it.setIdDetailPenjualan(rs.getInt("id_detail_penjualan"));
                    it.setIdDetailBarang(rs.getInt("id_detail_barang"));
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
