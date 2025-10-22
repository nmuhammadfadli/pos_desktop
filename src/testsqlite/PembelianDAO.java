package testsqlite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PembelianDAO - CRUD sederhana untuk tabel data_pembelian & detail_pembelian
 */
public class PembelianDAO {

    public PembelianDAO() throws SQLException {
        ensureTables();
    }

    private void ensureTables() throws SQLException {
        String createHeader = "CREATE TABLE IF NOT EXISTS data_pembelian ("
                + "id_pembelian TEXT PRIMARY KEY, "
                + "tgl_pembelian TEXT NOT NULL, "
                + "total_harga INTEGER NOT NULL"
                + ");";

        String createDetail = "CREATE TABLE IF NOT EXISTS detail_pembelian ("
                + "id_detail_pembelian INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "harga_beli INTEGER, "
                + "stok INTEGER, "
                + "subtotal INTEGER, "
                + "id_supplier INTEGER, "
                + "id_pembelian TEXT, "
                + "id_barang INTEGER, "
                + "FOREIGN KEY (id_pembelian) REFERENCES data_pembelian(id_pembelian)"
                + ");";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement st = conn.createStatement()) {
            try { st.execute("PRAGMA foreign_keys = ON;"); } catch (Throwable ignore) {}
            st.execute(createHeader);
            st.execute(createDetail);
        }
    }

    public boolean exists(String idPembelian) throws SQLException {
        String sql = "SELECT 1 FROM data_pembelian WHERE id_pembelian = ? LIMIT 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPembelian);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Insert pembelian header + details dalam satu transaksi.
     * Akan melempar SQLException bila id_pembelian sudah ada.
     */
    public void insertPembelianWithDetails(Pembelian p) throws SQLException {
        if (p == null) throw new IllegalArgumentException("Pembelian tidak boleh null");
        if (p.getIdPembelian() == null || p.getIdPembelian().trim().isEmpty())
            throw new IllegalArgumentException("idPembelian harus diisi");
        if (p.getDetails() == null || p.getDetails().isEmpty())
            throw new IllegalArgumentException("Tambahkan minimal 1 detail");

        String insHeader = "INSERT INTO data_pembelian (id_pembelian, tgl_pembelian, total_harga) VALUES (?, ?, ?)";
        String insDetail = "INSERT INTO detail_pembelian (harga_beli, stok, subtotal, id_supplier, id_pembelian, id_barang) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // cek duplikat
                if (exists(p.getIdPembelian())) {
                    throw new SQLException("ID pembelian '" + p.getIdPembelian() + "' sudah ada.");
                }

                // insert header
                try (PreparedStatement ph = conn.prepareStatement(insHeader)) {
                    ph.setString(1, p.getIdPembelian());
                    ph.setString(2, p.getTglPembelian());
                    ph.setInt(3, p.getTotalHarga() == null ? 0 : p.getTotalHarga());
                    ph.executeUpdate();
                }

                // insert detail
                try (PreparedStatement pd = conn.prepareStatement(insDetail, Statement.RETURN_GENERATED_KEYS)) {
                    for (DetailPembelian d : p.getDetails()) {
                        // pastikan idPembelian di detail di-set
                        d.setIdPembelian(p.getIdPembelian());

                        pd.setInt(1, d.getHargaBeli() == null ? 0 : d.getHargaBeli());
                        pd.setInt(2, d.getStok() == null ? 0 : d.getStok());
                        pd.setInt(3, d.getSubtotal() == null ? 0 : d.getSubtotal());
                        if (d.getIdSupplier() == null) pd.setNull(4, Types.INTEGER);
                        else pd.setInt(4, d.getIdSupplier());
                        pd.setString(5, d.getIdPembelian());
                        if (d.getIdBarang() == null) pd.setNull(6, Types.INTEGER);
                        else pd.setInt(6, d.getIdBarang());

                        pd.executeUpdate();
                        try (ResultSet gk = pd.getGeneratedKeys()) {
                            if (gk.next()) {
                                d.setIdDetailPembelian(gk.getInt(1));
                            }
                        }
                    }
                }

                conn.commit();
            } catch (Exception ex) {
                try { conn.rollback(); } catch (Throwable t) {}
                throw (ex instanceof SQLException) ? (SQLException) ex : new SQLException(ex);
            } finally {
                try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            }
        }
    }

    public List<Pembelian> findAllPembelian() throws SQLException {
        List<Pembelian> out = new ArrayList<>();
        String sql = "SELECT id_pembelian, tgl_pembelian, total_harga FROM data_pembelian ORDER BY tgl_pembelian DESC, id_pembelian DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pembelian p = new Pembelian();
                p.setIdPembelian(rs.getString("id_pembelian"));
                p.setTglPembelian(rs.getString("tgl_pembelian"));
                p.setTotalHarga(rs.getInt("total_harga"));
                out.add(p);
            }
        }
        return out;
    }

    public List<DetailPembelian> findDetailsByPembelian(String idPembelian) throws SQLException {
        List<DetailPembelian> out = new ArrayList<>();
        String sql = "SELECT id_detail_pembelian, harga_beli, stok, subtotal, id_supplier, id_pembelian, id_barang FROM detail_pembelian WHERE id_pembelian = ? ORDER BY id_detail_pembelian";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPembelian);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetailPembelian d = new DetailPembelian();
                    d.setIdDetailPembelian(rs.getInt("id_detail_pembelian"));
                    d.setHargaBeli(rs.getInt("harga_beli"));
                    d.setStok(rs.getInt("stok"));
                    d.setSubtotal(rs.getInt("subtotal"));
                    int sup = rs.getInt("id_supplier"); d.setIdSupplier(rs.wasNull() ? null : sup);
                    d.setIdPembelian(rs.getString("id_pembelian"));
                    int ib = rs.getInt("id_barang"); d.setIdBarang(rs.wasNull() ? null : ib);
                    out.add(d);
                }
            }
        }
        return out;
    }

    public void deletePembelian(String idPembelian) throws SQLException {
        String delDetail = "DELETE FROM detail_pembelian WHERE id_pembelian = ?";
        String delHeader = "DELETE FROM data_pembelian WHERE id_pembelian = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pd = conn.prepareStatement(delDetail);
                 PreparedStatement ph = conn.prepareStatement(delHeader)) {
                pd.setString(1, idPembelian);
                pd.executeUpdate();

                ph.setString(1, idPembelian);
                ph.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (Throwable t) {}
                throw ex;
            } finally {
                try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            }
        }
    }
}
