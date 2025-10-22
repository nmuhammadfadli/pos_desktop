package testsqlite;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {

    public List<Voucher> findAll() throws SQLException {
        List<Voucher> list = new ArrayList<>();
        // ambil id_guru dan nama_guru (left join) supaya bisa menampilkan nama guru
        String sql = "SELECT a.id_voucher, a.kode, a.id_guru, b.nama_guru, a.current_balance " +
                     "FROM kode_voucher a LEFT JOIN data_guru b ON a.id_guru = b.id_guru " +
                     "ORDER BY a.id_voucher";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToVoucher(rs));
            }
        }
        return list;
    }

    public Voucher findById(int id) throws SQLException {
        String sql = "SELECT a.id_voucher, a.kode, a.id_guru, b.nama_guru, a.current_balance " +
                     "FROM kode_voucher a LEFT JOIN data_guru b ON a.id_guru = b.id_guru " +
                     "WHERE a.id_voucher = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToVoucher(rs);
            }
        }
        return null;
    }

    /**
     * Insert voucher. Mengembalikan id generated (atau -1 jika gagal)
     * Asumsi: Voucher.getIdGuru() mengembalikan primitive int (selalu ada, 0 = tidak ada).
     */
    public int insert(Voucher v) throws SQLException {
        String sql = "INSERT INTO kode_voucher (kode, id_guru, current_balance) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getKode());
            ps.setInt(2, v.getIdGuru()); // primitive int
            ps.setString(3, v.getCurrentBalance() == null ? BigDecimal.ZERO.toPlainString() : v.getCurrentBalance().toPlainString());
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Update seluruh field voucher (kode, id_guru, current_balance)
     */
    public void update(Voucher v) throws SQLException {
        String sql = "UPDATE kode_voucher SET kode = ?, id_guru = ?, current_balance = ? WHERE id_voucher = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getKode());
            ps.setInt(2, v.getIdGuru());
            ps.setString(3, v.getCurrentBalance() == null ? BigDecimal.ZERO.toPlainString() : v.getCurrentBalance().toPlainString());
            ps.setInt(4, v.getIdVoucher());
            ps.executeUpdate();
        }
    }

    public void updateBalance(int idVoucher, BigDecimal newBal) throws SQLException {
        String sql = "UPDATE kode_voucher SET current_balance = ? WHERE id_voucher = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newBal == null ? BigDecimal.ZERO.toPlainString() : newBal.toPlainString());
            ps.setInt(2, idVoucher);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM kode_voucher WHERE id_voucher = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---------------- helper ----------------
    private Voucher mapRowToVoucher(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setIdVoucher(rs.getInt("id_voucher"));
        v.setKode(rs.getString("kode"));

        // ambil id_guru (jika NULL di DB, getInt mengembalikan 0)
        int idGuru = rs.getInt("id_guru");
        if (rs.wasNull()) idGuru = 0;
        v.setIdGuru(idGuru);

        // ambil nama guru (bisa null jika tidak ada)
        String namaGuru = rs.getString("nama_guru");
        v.setNamaGuru(namaGuru == null ? "-" : namaGuru);

        String s = rs.getString("current_balance");
        v.setCurrentBalance((s == null || s.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s));
        return v;
    }
}
