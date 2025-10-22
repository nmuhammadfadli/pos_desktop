package testsqlite;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk table 'receivable'
 * - nilai uang disimpan sebagai TEXT di DB (konversi ke BigDecimal di sini)
 */
public class ReceivableDAO {

    public List<Receivable> findAll() throws SQLException {
        List<Receivable> list = new ArrayList<>();
        String sql = "SELECT id_receivable, id_transaksi, amount_total, amount_paid, amount_outstanding, created_at, status FROM receivable ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(readFromResultSet(rs));
            }
        }
        return list;
    }

    public List<Receivable> findOpen() throws SQLException {
        List<Receivable> list = new ArrayList<>();
        String sql = "SELECT id_receivable, id_transaksi, amount_total, amount_paid, amount_outstanding, created_at, status FROM receivable WHERE status <> 'PAID' ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(readFromResultSet(rs));
        }
        return list;
    }

    public Receivable findById(int id) throws SQLException {
        String sql = "SELECT id_receivable, id_transaksi, amount_total, amount_paid, amount_outstanding, created_at, status FROM receivable WHERE id_receivable = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return readFromResultSet(rs);
            }
        }
        return null;
    }

    public Receivable findByTransaksi(long idTransaksi) throws SQLException {
        String sql = "SELECT id_receivable, id_transaksi, amount_total, amount_paid, amount_outstanding, created_at, status FROM receivable WHERE id_transaksi = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return readFromResultSet(rs);
            }
        }
        return null;
    }

    public int insert(Receivable r) throws SQLException {
        String sql = "INSERT INTO receivable (id_transaksi, amount_total, amount_paid, amount_outstanding, created_at, status) VALUES (?,?,?,?,datetime('now'),?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getIdTransaksi());
            ps.setString(2, r.getAmountTotal() == null ? "0" : r.getAmountTotal().toPlainString());
            ps.setString(3, r.getAmountPaid() == null ? "0" : r.getAmountPaid().toPlainString());
            ps.setString(4, r.getAmountOutstanding() == null ? "0" : r.getAmountOutstanding().toPlainString());
            ps.setString(5, r.getStatus() == null ? "OPEN" : r.getStatus());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM receivable WHERE id_receivable = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Apply payment to receivable (partial or full). This method is atomic.
     * - paymentAmount: jumlah yang dibayar sekarang
     * - jika amount_outstanding setelah dikurangi paymentAmount = 0 => status = PAID
     * - else status = PARTIAL (atau tetap OPEN)
     */
    public void applyPayment(int idReceivable, BigDecimal paymentAmount) throws SQLException {
        if (paymentAmount == null) paymentAmount = BigDecimal.ZERO;
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Jumlah pembayaran harus > 0");

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Receivable r = findById(idReceivable);
                if (r == null) throw new SQLException("Receivable tidak ditemukan: " + idReceivable);

                BigDecimal currentPaid = r.getAmountPaid() == null ? BigDecimal.ZERO : r.getAmountPaid();
                BigDecimal outstanding = r.getAmountOutstanding() == null ? BigDecimal.ZERO : r.getAmountOutstanding();

                BigDecimal newPaid = currentPaid.add(paymentAmount);
                BigDecimal newOutstanding = outstanding.subtract(paymentAmount);
                if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                    // tidak boleh negative outstanding => treat overpayment: set outstanding 0, adjust paid accordingly
                    newOutstanding = BigDecimal.ZERO;
                }

                String newStatus = (newOutstanding.compareTo(BigDecimal.ZERO) == 0) ? "PAID" : "PARTIAL";

                String upd = "UPDATE receivable SET amount_paid = ?, amount_outstanding = ?, status = ? WHERE id_receivable = ?";
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setString(1, newPaid.toPlainString());
                    ps.setString(2, newOutstanding.toPlainString());
                    ps.setString(3, newStatus);
                    ps.setInt(4, idReceivable);
                    ps.executeUpdate();
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

    private Receivable readFromResultSet(ResultSet rs) throws SQLException {
        Receivable r = new Receivable();
        r.setIdReceivable(rs.getInt("id_receivable"));
        r.setIdTransaksi(rs.getLong("id_transaksi"));
        String at = rs.getString("amount_total");
        String ap = rs.getString("amount_paid");
        String ao = rs.getString("amount_outstanding");
        r.setAmountTotal(at == null || at.trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(at));
        r.setAmountPaid(ap == null || ap.trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(ap));
        r.setAmountOutstanding(ao == null || ao.trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(ao));
        r.setCreatedAt(rs.getString("created_at"));
        r.setStatus(rs.getString("status"));
        return r;
    }
}
