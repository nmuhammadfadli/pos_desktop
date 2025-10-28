package testsqlite;

import Voucher.Voucher;
import Barang.DetailBarang;
import Barang.Barang;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper: inisialisasi DB + CRUD helper untuk barang, voucher, detail_barang.
 * Java 8 compatible. Package: testsqlite
 */
public class DatabaseHelper {
    private static final String DB_PATH = "data/pos_app.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    // ------------------ init & connection ------------------
    public static void initDatabase() throws Exception {
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        File dbFile = new File(DB_PATH);
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        boolean existed = dbFile.exists();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // enable pragma early
            try { stmt.execute("PRAGMA foreign_keys = ON;"); } catch (Throwable t) {}
            try { stmt.execute("PRAGMA journal_mode = WAL;"); } catch (Throwable t) {}

            // reference tables (create data_pengguna early so FK can reference it)
            stmt.execute("CREATE TABLE IF NOT EXISTS data_kategori (" +
                    "id_kategori VARCHAR(10) PRIMARY KEY, " +
                    "nama_kategori TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS data_guru (" +
                    "id_guru INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama_guru TEXT UNIQUE NOT NULL, " +
                    "notelp_guru TEXT DEFAULT '0', " +
                    "jabatan TEXT DEFAULT '')");

            stmt.execute("CREATE TABLE IF NOT EXISTS data_supplier (" +
                    "id_supplier INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama_supplier TEXT NOT NULL, " +
                    "alamat_supplier TEXT, " +
                    "notelp_supplier TEXT)");

            // ensure data_pengguna exists BEFORE transaksi_penjualan (so FK valid)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS data_pengguna (" +
                    "id_pengguna TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password TEXT, " +
                    "alamat TEXT, " +
                    "jabatan TEXT, " +
                    "nama_lengkap TEXT, " +
                    "hak_akses INTEGER DEFAULT 0, " +
                    "email TEXT, " +
                    "notelp_pengguna TEXT" +
                ")"
            );

            // barang
            stmt.execute("CREATE TABLE IF NOT EXISTS barang (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama TEXT NOT NULL, " +
                    "id_kategori VARCHAR(10) NOT NULL, " +
                    "FOREIGN KEY(id_kategori) REFERENCES data_kategori(id_kategori))");

            // pembelian header
            stmt.execute("CREATE TABLE IF NOT EXISTS data_pembelian (" +
                    "id_pembelian TEXT PRIMARY KEY, " +
                    "tgl_pembelian TEXT NOT NULL, " +
                    "payment_method TEXT NOT NULL DEFAULT 'CASH', " +
                    "total_harga INTEGER NOT NULL DEFAULT 0)");

            // detail_pembelian (line items)
            stmt.execute("CREATE TABLE IF NOT EXISTS detail_pembelian (" +
                    "id_detail_pembelian INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_pembelian TEXT NOT NULL, " +
                    "id_barang INTEGER NOT NULL, " +
                    "id_supplier INTEGER NOT NULL, " +
                    "harga_beli INTEGER NOT NULL, " +
                    "stok INTEGER NOT NULL, " +
                    "subtotal INTEGER NOT NULL, " +
                    "FOREIGN KEY(id_pembelian) REFERENCES data_pembelian(id_pembelian) ON DELETE CASCADE, " +
                    "FOREIGN KEY(id_barang) REFERENCES barang(id) ON DELETE RESTRICT, " +
                    "FOREIGN KEY(id_supplier) REFERENCES data_supplier(id_supplier) ON DELETE RESTRICT" +
                    ")");

            // detail_barang (batches)
            stmt.execute("CREATE TABLE IF NOT EXISTS detail_barang (" +
                    "id_detail_barang INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_barang INTEGER NOT NULL, " +
                    "id_supplier INTEGER, " +
                    "stok INTEGER NOT NULL DEFAULT 0, " +
                    "harga_jual TEXT NOT NULL, " +
                    "tanggal_exp TEXT, " +
                    "barcode TEXT, " +
                    "id_detail_pembelian INTEGER, " +
                    "FOREIGN KEY (id_barang) REFERENCES barang(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (id_supplier) REFERENCES data_supplier(id_supplier) ON DELETE SET NULL, " +
                    "FOREIGN KEY (id_detail_pembelian) REFERENCES detail_pembelian(id_detail_pembelian) ON DELETE CASCADE" +
                    ")");

            // kode_voucher
            stmt.execute("CREATE TABLE IF NOT EXISTS kode_voucher (" +
                    "id_voucher INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "kode TEXT UNIQUE, " +
                    "id_guru INTEGER, " +
                    "bulan TEXT, " +
                    "current_balance TEXT DEFAULT '0', " +
                    "created_at TEXT DEFAULT (datetime('now')), " +
                    "FOREIGN KEY(id_guru) REFERENCES data_guru(id_guru))");

            // transaksi_penjualan (fixed syntax; FK references exist)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS transaksi_penjualan (" +
                    "id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "kode_transaksi TEXT UNIQUE, " +
                    "tgl_transaksi TEXT DEFAULT (datetime('now')), " +
                    "total_harga TEXT NOT NULL, " +
                    "total_bayar TEXT NOT NULL DEFAULT '0', " +
                    "kembalian TEXT NOT NULL DEFAULT '0', " +
                    "payment_method TEXT NOT NULL DEFAULT 'CASH', " +
                    "id_voucher INTEGER, " +
                    "id_pengguna TEXT, " +   // nullable to allow legacy / tests
                    "FOREIGN KEY (id_voucher) REFERENCES kode_voucher(id_voucher), " +
                    "FOREIGN KEY (id_pengguna) REFERENCES data_pengguna(id_pengguna)" +
                ")"
            );

            // detail_penjualan
            stmt.execute("CREATE TABLE IF NOT EXISTS detail_penjualan (" +
                    "id_detail_penjualan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_transaksi INTEGER NOT NULL, " +
                    "id_detail_barang INTEGER NOT NULL, " +
                    "jumlah_barang INTEGER NOT NULL, " +
                    "harga_unit TEXT NOT NULL, " +
                    "subtotal TEXT NOT NULL, " +
                    "FOREIGN KEY (id_transaksi) REFERENCES transaksi_penjualan(id_transaksi))");

            // voucher_usage
            stmt.execute("CREATE TABLE IF NOT EXISTS voucher_usage (" +
                    "id_usage INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_voucher INTEGER NOT NULL, " +
                    "id_transaksi INTEGER NOT NULL, " +
                    "used_amount TEXT NOT NULL, " +
                    "used_at TEXT DEFAULT (datetime('now')))");

            // receivable
            stmt.execute("CREATE TABLE IF NOT EXISTS receivable (" +
                    "id_receivable INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_transaksi INTEGER NOT NULL UNIQUE, " +
                    "amount_total TEXT NOT NULL, " +
                    "amount_paid TEXT NOT NULL DEFAULT '0', " +
                    "amount_outstanding TEXT NOT NULL, " +
                    "created_at TEXT DEFAULT (datetime('now')), " +
                    "status TEXT DEFAULT 'OPEN')");

            // indexes
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_detail_barang_idx ON detail_barang(id_barang, id_supplier, harga_jual)"); } catch (Throwable t) {}
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_detail_pembelian_id_barang ON detail_pembelian(id_barang)"); } catch (Throwable t) {}
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_detail_pembelian_id_supplier ON detail_pembelian(id_supplier)"); } catch (Throwable t) {}

            // triggers (unchanged)
            try {
                stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS trg_after_insert_detail_pembelian_merge " +
                    "AFTER INSERT ON detail_pembelian " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  UPDATE detail_barang SET stok = stok + NEW.stok " +
                    "    WHERE id_barang = NEW.id_barang AND id_supplier = NEW.id_supplier AND harga_jual = CAST(NEW.harga_beli AS TEXT);\n" +
                    "  INSERT INTO detail_barang (id_barang, id_supplier, stok, harga_jual, tanggal_exp, barcode, id_detail_pembelian) " +
                    "    SELECT NEW.id_barang, NEW.id_supplier, NEW.stok, CAST(NEW.harga_beli AS TEXT), NULL, NULL, NEW.id_detail_pembelian " +
                    "    WHERE NOT EXISTS (SELECT 1 FROM detail_barang WHERE id_barang = NEW.id_barang AND id_supplier = NEW.id_supplier AND harga_jual = CAST(NEW.harga_beli AS TEXT));\n" +
                    "END;"
                );
            } catch (Throwable t) {
                System.err.println("warning creating trg_after_insert_detail_pembelian_merge: " + t.getMessage());
            }
            try {
                stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS trg_after_delete_detail_pembelian_cleanup " +
                    "AFTER DELETE ON detail_pembelian " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  DELETE FROM detail_barang WHERE id_detail_pembelian = OLD.id_detail_pembelian; " +
                    "END;"
                );
            } catch (Throwable t) {
                System.err.println("warning creating trg_after_delete_detail_pembelian_cleanup: " + t.getMessage());
            }

            // sample data if db new
            if (!existed) {
                // sample kategori
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO data_kategori (id_kategori, nama_kategori) VALUES (?, ?)")) {
                    ps.setString(1, "KTG01");
                    ps.setString(2, "Umum");
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample kategori: " + t.getMessage());
                }

                // sample pengguna (dummy) - gunakan setInt untuk hak_akses
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO data_pengguna (id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                    // Admin
                    ps.setString(1, "USR001");
                    ps.setString(2, "admin");
                    ps.setString(3, "admin"); // plain-text for dev only
                    ps.setString(4, "Jl. Administrator 1");
                    ps.setString(5, "Administrator");
                    ps.setString(6, "Admin Sistem");
                    ps.setInt(7, 99);
                    ps.setString(8, "admin@example.com");
                    ps.setString(9, "0811000001");
                    ps.executeUpdate();

                    // Kasir 1
                    ps.setString(1, "USR002");
                    ps.setString(2, "kasir1");
                    ps.setString(3, "kasir123");
                    ps.setString(4, "Jl. Kasir 2");
                    ps.setString(5, "Kasir");
                    ps.setString(6, "Budi Santoso");
                    ps.setInt(7, 1);
                    ps.setString(8, "budi@example.com");
                    ps.setString(9, "0811000002");
                    ps.executeUpdate();

                    // Kasir 2
                    ps.setString(1, "USR003");
                    ps.setString(2, "kasir2");
                    ps.setString(3, "kasir123");
                    ps.setString(4, "Jl. Kasir 3");
                    ps.setString(5, "Kasir");
                    ps.setString(6, "Siti Rahma");
                    ps.setInt(7, 1);
                    ps.setString(8, "siti@example.com");
                    ps.setString(9, "0811000003");
                    ps.executeUpdate();

                    System.out.println("Sample pengguna disisipkan (USR001..USR003).");
                } catch (Throwable t) {
                    System.err.println("warning inserting sample pengguna: " + t.getMessage());
                }

                // sample barang
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO barang (nama, id_kategori) VALUES (?, ?)")) {
                    ps.setString(1, "Kopi Bondowoso");
                    ps.setString(2, "KTG01");
                    ps.executeUpdate();

                    ps.setString(1, "Teh Hijau");
                    ps.setString(2, "KTG01");
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample barang: " + t.getMessage());
                }

                // sample suppliers
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO data_supplier (nama_supplier, alamat_supplier, notelp_supplier) VALUES (?, ?, ?)")) {
                    ps.setString(1, "Supplier A");
                    ps.setString(2, "Jl. Merdeka 1");
                    ps.setString(3, "081100111");
                    ps.executeUpdate();

                    ps.setString(1, "Supplier B");
                    ps.setString(2, "Jl. Sudirman 2");
                    ps.setString(3, "081200222");
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample suppliers: " + t.getMessage());
                }

                // sample voucher
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO kode_voucher (kode,id_guru,current_balance) VALUES (?,?,?)")) {
                    ps.setString(1, "VCHR001");
                    ps.setObject(2, null);
                    ps.setString(3, new BigDecimal("50000").toPlainString());
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample voucher: " + t.getMessage());
                }

                System.out.println("Database baru dibuat dan sample data disisipkan.");
            } else {
                System.out.println("Database sudah ada: " + DB_PATH);
            }

            // after init, print pengguna to help debugging (shows whether dummy present)
            printAllPenggunaDetailed();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite tidak ditemukan. Pastikan sqlite-jdbc.jar ada di Libraries.");
        }
        Connection conn = DriverManager.getConnection(URL);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");
            // jangan set WAL berulang-ubah jika tidak perlu, tapi boleh:
            // s.execute("PRAGMA journal_mode = WAL;");
        } catch (SQLException ignore) {}
        return conn;
    }

    // ----- ensure sample pengguna exists even if DB file already existed -----
    private static void ensureSamplePengguna(Connection conn) {
    try (PreparedStatement psCheck = conn.prepareStatement("SELECT COUNT(1) FROM data_pengguna");
         ResultSet rs = psCheck.executeQuery()) {
        int count = 0;
        if (rs.next()) count = rs.getInt(1);
        if (count == 0) {
            System.out.println("data_pengguna kosong — akan disisipkan sample pengguna.");
            // gunakan transaction agar semua insert konsisten
            boolean oldAuto = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO data_pengguna (id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    // Admin
                    ps.setString(1, "USR001");
                    ps.setString(2, "admin");
                    ps.setString(3, "admin");
                    ps.setString(4, "Jl. Administrator 1");
                    ps.setString(5, "Administrator");
                    ps.setString(6, "Admin Sistem");
                    ps.setInt(7, 99);
                    ps.setString(8, "admin@example.com");
                    ps.setString(9, "0811000001");
                    int u = ps.executeUpdate();
                    System.out.println("Inserted admin rows = " + u);

                    // Kasir 1
                    ps.setString(1, "USR002");
                    ps.setString(2, "kasir1");
                    ps.setString(3, "kasir123");
                    ps.setString(4, "Jl. Kasir 2");
                    ps.setString(5, "Kasir");
                    ps.setString(6, "Budi Santoso");
                    ps.setInt(7, 1);
                    ps.setString(8, "budi@example.com");
                    ps.setString(9, "0811000002");
                    u = ps.executeUpdate();
                    System.out.println("Inserted kasir1 rows = " + u);

                    // Kasir 2
                    ps.setString(1, "USR003");
                    ps.setString(2, "kasir2");
                    ps.setString(3, "kasir123");
                    ps.setString(4, "Jl. Kasir 3");
                    ps.setString(5, "Kasir");
                    ps.setString(6, "Siti Rahma");
                    ps.setInt(7, 1);
                    ps.setString(8, "siti@example.com");
                    ps.setString(9, "0811000003");
                    u = ps.executeUpdate();
                    System.out.println("Inserted kasir2 rows = " + u);
                }
                conn.commit();
            } catch (Throwable t) {
                try { conn.rollback(); } catch (Throwable ignore) {}
                System.err.println("Gagal menyisipkan sample pengguna (rollback): " + t.getMessage());
            } finally {
                try { conn.setAutoCommit(oldAuto); } catch (SQLException ignore) {}
            }
        } else {
            System.out.println("data_pengguna sudah berisi " + count + " baris; sample tidak disisipkan.");
        }
    } catch (Throwable t) {
        System.err.println("Gagal mengecek/menyisipkan sample pengguna: " + t.getMessage());
    }
}
    
    public static String generateNextVoucherCode() throws SQLException {
    try (Connection conn = getConnection()) {
        return generateNextVoucherCode(conn);
    }
}

public static String generateNextVoucherCode(Connection conn) throws SQLException {
    String sql = "SELECT MAX(CAST(substr(kode, 5) AS INTEGER)) AS maxn " +
                 "FROM kode_voucher WHERE kode GLOB 'VCHR[0-9]*'";
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        int next = 1;
        if (rs.next()) {
            Object mx = rs.getObject("maxn");
            if (mx != null) {
                try {
                    next = ((Number) mx).intValue() + 1;
                } catch (ClassCastException cce) {
                    // fallback: parse as string
                    try {
                        next = Integer.parseInt(rs.getString("maxn")) + 1;
                    } catch (Exception ignore) { next = 1; }
                }
            }
        }
        // format minimal 3 digit, tumbuh jika angka lebih besar (001, 010, 100, 1000, dll)
        String suffix = String.format("%03d", next);
        return "VCHR" + suffix;
    }
}

    
    // helper: print contents of data_pengguna (for quick debugging)
   // print semua kolom (debug)
public static void printAllPenggunaDetailed() {
    String sql = "SELECT id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna FROM data_pengguna ORDER BY id_pengguna";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        System.out.println("== daftar data_pengguna (detailed) ==");
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("id_pengguna   : " + rs.getString("id_pengguna"));
            System.out.println("username      : " + rs.getString("username"));
            System.out.println("password      : " + rs.getString("password"));
            System.out.println("alamat        : " + rs.getString("alamat"));
            System.out.println("jabatan       : " + rs.getString("jabatan"));
            System.out.println("nama_lengkap  : " + rs.getString("nama_lengkap"));
            System.out.println("hak_akses     : " + rs.getObject("hak_akses"));
            System.out.println("email         : " + rs.getString("email"));
            System.out.println("notelp        : " + rs.getString("notelp_pengguna"));
            System.out.println("----------------------------------------");
        }
        if (count == 0) System.out.println("(kosong)");
        System.out.println("== end daftar data_pengguna ==");
    } catch (Exception e) {
        System.err.println("Gagal membaca data_pengguna: " + e.getMessage());
    }
}

// kembalian programatik: list of maps (optional)
public static List<java.util.Map<String,Object>> getAllPenggunaAsMap() throws SQLException {
    List<java.util.Map<String,Object>> list = new ArrayList<>();
    String sql = "SELECT id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna FROM data_pengguna ORDER BY id_pengguna";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            java.util.Map<String,Object> row = new java.util.HashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            list.add(row);
        }
    }
    return list;
}


    // ------------------ CRUD Barang ------------------
    public static List<Barang> getAllBarang() throws SQLException {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT b.id, b.nama, b.id_kategori, k.nama_kategori " +
                "FROM barang b LEFT JOIN data_kategori k ON b.id_kategori = k.id_kategori ORDER BY b.nama";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Barang b = new Barang();
                b.setId(rs.getInt("id"));
                b.setNama(rs.getString("nama"));
                b.setIdKategori(rs.getString("id_kategori"));
                b.setNamaKategori(rs.getString("nama_kategori"));
                list.add(b);
            }
        }
        return list;
    }

    public static void insertBarang(Barang b) throws SQLException {
        String sql = "INSERT INTO barang (nama, id_kategori) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getIdKategori());
            ps.executeUpdate();
        }
    }

    public static void updateBarang(Barang b) throws SQLException {
        String sql = "UPDATE barang SET nama=?, id_kategori=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getIdKategori());
            ps.setInt(3, b.getId());
            ps.executeUpdate();
        }
    }

    public static void deleteBarang(int id) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM barang WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------ CRUD Voucher ------------------
public static List<Voucher> getAllVouchers() throws SQLException {
    List<Voucher> list = new ArrayList<>();
    String sql = "SELECT a.id_voucher, a.kode, a.current_balance, b.nama_guru, a.bulan FROM kode_voucher a LEFT JOIN data_guru b ON a.id_guru = b.id_guru ORDER BY a.id_voucher";
    
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String bal = rs.getString("current_balance");
            BigDecimal b = (bal == null || bal.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(bal);
            
            Voucher v = new Voucher();
            v.setIdVoucher(rs.getInt("id_voucher"));
            v.setKode(rs.getString("kode"));
            v.setCurrentBalance(b);
            v.setNamaGuru(rs.getString("nama_guru")); 
            v.setBulan(rs.getString("bulan"));// ← tambahan baru
            list.add(v);
        }
    }
    return list;
}

public static Voucher findVoucherById(int idVoucher) throws SQLException {
    String sql = "SELECT a.id_voucher, a.kode, a.current_balance, b.nama_guru FROM kode_voucher a LEFT JOIN data_guru b ON a.id_guru = b.id_guru WHERE a.id_voucher = ? ";
    
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, idVoucher);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String bal = rs.getString("current_balance");
                BigDecimal b = (bal == null || bal.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(bal);
                
                Voucher v = new Voucher();
                v.setIdVoucher(rs.getInt("id_voucher"));
                v.setKode(rs.getString("kode"));
                v.setCurrentBalance(b);
                v.setNamaGuru(rs.getString("nama_guru")); // ← tambahan baru
                return v;
            }
        }
    }
    return null;
}


  public static int insertVoucher(Voucher v) throws SQLException {
    String sql = "INSERT INTO kode_voucher (kode, bulan, id_guru, current_balance) VALUES (?, ?, ?, ?)";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        // 1) kode
        ps.setString(1, v.getKode());

        // 2) bulan (String) - treat empty/null as NULL in DB
        String bulan = v.getBulan();
        if (bulan == null || bulan.trim().isEmpty()) {
            ps.setNull(2, java.sql.Types.VARCHAR);
        } else {
            ps.setString(2, bulan.trim());
        }

        // 3) id_guru - modelmu pakai int; treat 0 sebagai "tidak ada" -> simpan NULL di DB
        int idGuru = v.getIdGuru();
        if (idGuru == 0) {
            ps.setNull(3, java.sql.Types.INTEGER);
        } else {
            ps.setInt(3, idGuru);
        }

        // 4) current_balance (simpan sebagai string dari BigDecimal)
        BigDecimal bal = v.getCurrentBalance() == null ? BigDecimal.ZERO : v.getCurrentBalance();
        ps.setString(4, bal.toPlainString());

        int updated = ps.executeUpdate();
        if (updated == 0) {
            throw new SQLException("Insert voucher gagal, tidak ada row yang tersisip.");
        }

        try (ResultSet g = ps.getGeneratedKeys()) {
            if (g.next()) return g.getInt(1);
        }
    }
    return -1;
}


 public static void updateVoucher(int idVoucher, String kode, String bulan, Integer idGuru, BigDecimal currentBalance) throws SQLException {
    String sql = "UPDATE kode_voucher SET kode = ?, bulan = ?, id_guru = ?, current_balance = ? WHERE id_voucher = ?";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, kode);

        if (bulan == null || bulan.trim().isEmpty()) {
            ps.setNull(2, java.sql.Types.VARCHAR);
        } else {
            ps.setString(2, bulan);
        }

        if (idGuru == null || idGuru == 0) {
            ps.setNull(3, java.sql.Types.INTEGER);
        } else {
            ps.setInt(3, idGuru);
        }

        ps.setString(4, currentBalance == null ? BigDecimal.ZERO.toPlainString() : currentBalance.toPlainString());
        ps.setInt(5, idVoucher);
        ps.executeUpdate();
    }
}

    public static void deleteVoucher(int idVoucher) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kode_voucher WHERE id_voucher = ?")) {
            ps.setInt(1, idVoucher);
            ps.executeUpdate();
        }
    }

    // ------------------ CRUD Detail Barang (revised: NO id_supplier) ------------------
//    public static List<DetailBarang> getAllDetailBarang() throws SQLException {
//        List<DetailBarang> list = new ArrayList<>();
//        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang ORDER BY id_detail_barang";
//        try (Connection conn = getConnection();
//             PreparedStatement ps = conn.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//            while (rs.next()) {
//                DetailBarang d = new DetailBarang();
//                d.setId(rs.getInt("id_detail_barang"));
//                d.setBarcode(rs.getString("barcode"));
//                d.setStok(rs.getInt("stok"));
//                String hargaStr = rs.getString("harga_jual");
//                d.setHargaJual((hargaStr == null || hargaStr.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(hargaStr));
//                d.setTanggalExp(rs.getString("tanggal_exp"));
//                d.setIdBarang(rs.getInt("id_barang"));
//                // if your DetailBarang model has idDetailPembelian field, set it here (optional)
//                try {
//                    DetailBarang.class.getMethod("setIdDetailPembelian", Integer.class).invoke(d, (Integer) rs.getObject("id_detail_pembelian"));
//                } catch (NoSuchMethodException nsme) {
//                    // model doesn't have field, ignore
//                } catch (Exception ignore) {}
//                list.add(d);
//            }
//        }
//        return list;
//    }

public static DetailBarang findDetailById(int idDetail) throws SQLException {
    String sql =
        "SELECT a.id_detail_barang, a.barcode, a.stok, a.harga_jual, a.tanggal_exp, " +
        "a.id_barang, b.nama AS nama_barang, a.id_supplier, c.nama_supplier, a.id_detail_pembelian " +
        "FROM detail_barang a " +
        "LEFT JOIN barang b ON a.id_barang = b.id " +
        "LEFT JOIN data_supplier c ON a.id_supplier = c.id_supplier " +
        "WHERE a.id_detail_barang = ?";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, idDetail);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                DetailBarang d = new DetailBarang();
                d.setId(rs.getInt("id_detail_barang"));
                d.setBarcode(rs.getString("barcode"));
                d.setStok(rs.getInt("stok"));

                String hargaStr = rs.getString("harga_jual");
                d.setHargaJual((hargaStr == null || hargaStr.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(hargaStr));

                d.setTanggalExp(rs.getString("tanggal_exp"));

                // id_barang + nama_barang
                int idBarang = rs.getInt("id_barang");
                if (rs.wasNull()) {
                    d.setIdBarang(0); // atau sesuaikan jika model memakai Integer
                } else {
                    d.setIdBarang(idBarang);
                }
                d.setNamaBarang(rs.getString("nama_barang")); // bisa null

                // id_supplier + nama_supplier (nullable)
                int idSup = rs.getInt("id_supplier");
                if (rs.wasNull()) d.setIdSupplier(null);
                else d.setIdSupplier(idSup);
                d.setNamaSupplier(rs.getString("nama_supplier")); // bisa null

                // id_detail_pembelian (nullable) - gunakan getObject agar null tetap null
                Object idDetObj = rs.getObject("id_detail_pembelian");
                try {
                    // jika model punya setter dengan Integer
                    DetailBarang.class.getMethod("setIdDetailPembelian", Integer.class).invoke(d, (Integer) idDetObj);
                } catch (NoSuchMethodException nsme) {
                    // jika tidak ada setter dengan Integer, coba setter dengan int (primitive)
//                    try {
//                        if (idDetObj != null) {
//                            DetailBarang.class.getMethod("setIdDetailPembelian", int.class).invoke(d, ((Number) idDetObj).intValue());
//                        }
//                    } catch (NoSuchMethodException | Exception) {}
                } catch (Exception ignore) {}

                return d;
            }
        }
    }
    return null;
}

public static List<DetailBarang> getAllDetailBarang() throws SQLException {
    List<DetailBarang> list = new ArrayList<>();
    String sql =
        "SELECT a.id_detail_barang, a.barcode, a.stok, a.harga_jual, a.tanggal_exp, " +
        "a.id_barang, b.nama AS nama_barang, a.id_supplier, c.nama_supplier, a.id_detail_pembelian " +
        "FROM detail_barang a " +
        "LEFT JOIN barang b ON a.id_barang = b.id " +
        "LEFT JOIN data_supplier c ON a.id_supplier = c.id_supplier " +
        "ORDER BY a.id_detail_barang";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            DetailBarang d = new DetailBarang();
            d.setId(rs.getInt("id_detail_barang"));
            d.setBarcode(rs.getString("barcode"));
            d.setStok(rs.getInt("stok"));

            String hargaStr = rs.getString("harga_jual");
            d.setHargaJual((hargaStr == null || hargaStr.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(hargaStr));

            d.setTanggalExp(rs.getString("tanggal_exp"));

            // id_barang + nama_barang
            int idBarang = rs.getInt("id_barang");
            if (rs.wasNull()) d.setIdBarang(0); else d.setIdBarang(idBarang);
            d.setNamaBarang(rs.getString("nama_barang"));

            // id_supplier + nama_supplier
            int idSup = rs.getInt("id_supplier");
            if (rs.wasNull()) d.setIdSupplier(null); else d.setIdSupplier(idSup);
            d.setNamaSupplier(rs.getString("nama_supplier"));

            // id_detail_pembelian nullable
            Object idDetObj = rs.getObject("id_detail_pembelian");
            try {
                DetailBarang.class.getMethod("setIdDetailPembelian", Integer.class).invoke(d, (Integer) idDetObj);
            } catch (NoSuchMethodException nsme) {
//                try {
//                    if (idDetObj != null) {
//                        DetailBarang.class.getMethod("setIdDetailPembelian", int.class).invoke(d, ((Number) idDetObj).intValue());
//                    }
//                } catch (NoSuchMethodException | Exception ignore) {}
            } catch (Exception ignore) {}

            list.add(d);
        }
    }
    return list;
}


    public static int insertDetailBarang(DetailBarang d) throws SQLException {
        String sql = "INSERT INTO detail_barang (barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getBarcode());
            ps.setInt(2, d.getStok());
            ps.setString(3, d.getHargaJual() == null ? BigDecimal.ZERO.toPlainString() : d.getHargaJual().toPlainString());
            ps.setString(4, d.getTanggalExp());
            ps.setString(5, d.getNamaBarang());
            // set id_detail_pembelian if model exposes it, else null
            try {
                Integer idDet = (Integer) DetailBarang.class.getMethod("getIdDetailPembelian").invoke(d);
                if (idDet == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, idDet);
            } catch (NoSuchMethodException nsme) {
                ps.setNull(6, Types.INTEGER);
            } catch (Exception ex) {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
        }
        return -1;
    }

    public static void updateDetailBarang(DetailBarang d) throws SQLException {
        String sql = "UPDATE detail_barang SET barcode=?, stok=?, harga_jual=?, tanggal_exp=?, id_barang=?, id_detail_pembelian=? WHERE id_detail_barang=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getBarcode());
            ps.setInt(2, d.getStok());
            ps.setString(3, d.getHargaJual() == null ? BigDecimal.ZERO.toPlainString() : d.getHargaJual().toPlainString());
            ps.setString(4, d.getTanggalExp());
            ps.setString(5, d.getNamaBarang());
            try {
                Integer idDet = (Integer) DetailBarang.class.getMethod("getIdDetailPembelian").invoke(d);
                if (idDet == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, idDet);
            } catch (NoSuchMethodException nsme) {
                ps.setNull(6, Types.INTEGER);
            } catch (Exception ex) {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, d.getId());
            ps.executeUpdate();
        }
    }

    public static void deleteDetailBarang(int idDetail) throws SQLException {
        String sql = "DELETE FROM detail_barang WHERE id_detail_barang = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            ps.executeUpdate();
        }
    }

    public static void backupDatabase(String targetPath) throws IOException {
        Path src = Paths.get(DB_PATH);
        Path dst = Paths.get(targetPath);
        if (Files.notExists(src)) throw new IOException("Database file tidak ditemukan: " + src.toString());
        Files.copy(src, dst);
    }
}
