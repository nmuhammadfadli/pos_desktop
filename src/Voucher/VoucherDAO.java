package Voucher;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import testsqlite.DatabaseHelper;

public class VoucherDAO {

    // SELECT: urutkan kolom agar konsisten dengan UI: id_voucher, kode, bulan, id_guru, nama_guru, current_balance
    public List<Voucher> findAll() throws SQLException {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT a.id_voucher, a.kode, a.bulan, a.id_guru, b.nama_guru, a.current_balance " +
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
        String sql = "SELECT a.id_voucher, a.kode, a.bulan, a.id_guru, b.nama_guru, a.current_balance " +
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

    // INSERT: urutan kolom kode, bulan, id_guru, current_balance
    public int insert(Voucher v) throws SQLException {
        String sql = "INSERT INTO kode_voucher (kode, bulan, id_guru, current_balance) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, v.getKode());

            // bulan (String) - bisa null/empty
            String bulan = v.getBulan();
            if (bulan == null || bulan.trim().isEmpty()) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, bulan);

            // id_guru: treat 0 as NULL
            int idGuru = v.getIdGuru();
            if (idGuru == 0) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, idGuru);

            BigDecimal bal = v.getCurrentBalance() == null ? BigDecimal.ZERO : v.getCurrentBalance();
            ps.setString(4, bal.toPlainString());

            int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("Insert voucher gagal, tidak ada row yang tersisip.");

            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
        }
        return -1;
    }

    // UPDATE: urutan parameter sama (kode, bulan, id_guru, current_balance)
    public void update(Voucher v) throws SQLException {
        String sql = "UPDATE kode_voucher SET kode = ?, bulan = ?, id_guru = ?, current_balance = ? WHERE id_voucher = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, v.getKode());

            String bulan = v.getBulan();
            if (bulan == null || bulan.trim().isEmpty()) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, bulan);

            int idGuru = v.getIdGuru();
            if (idGuru == 0) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, idGuru);

            BigDecimal bal = v.getCurrentBalance() == null ? BigDecimal.ZERO : v.getCurrentBalance();
            ps.setString(4, bal.toPlainString());

            ps.setInt(5, v.getIdVoucher());
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

    // mapping: baca bulan sebagai String dulu, lalu id_guru
    private Voucher mapRowToVoucher(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setIdVoucher(rs.getInt("id_voucher"));
        v.setKode(rs.getString("kode"));

        // bulan (String)
        String bulanVal = rs.getString("bulan");
        v.setBulan(bulanVal); // will be null if DB NULL

        // id_guru (nullable) -> Voucher.idGuru is int; treat NULL as 0
        int idGuruVal = rs.getInt("id_guru");
        if (rs.wasNull()) v.setIdGuru(0);
        else v.setIdGuru(idGuruVal);

        // nama guru (bisa null)
        String namaGuru = rs.getString("nama_guru");
        v.setNamaGuru(namaGuru == null ? null : namaGuru);

        // saldo
        String s = rs.getString("current_balance");
        v.setCurrentBalance((s == null || s.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s));
        return v;
    }
}
