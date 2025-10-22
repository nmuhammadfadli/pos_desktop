package testsqlite;

import javax.swing.SwingUtilities;

public class testsqlite {
    public static void main(String[] args) {
        try {
            DatabaseHelper.initDatabase();
        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Gagal inisialisasi database: " + ex.getMessage());
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
