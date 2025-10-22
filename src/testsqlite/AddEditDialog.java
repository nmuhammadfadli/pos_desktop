package testsqlite;

import java.math.BigDecimal;
import javax.swing.*;

public class AddEditDialog extends JDialog {
    private Barang barang;
    private boolean saved = false;

    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtHarga;
    private javax.swing.JTextField txtStok;

    public AddEditDialog(java.awt.Frame parent, Barang b) {
        super(parent, true);
        this.barang = (b == null) ? new Barang() : b;
        initComponents();
        if (b != null) {
            txtNama.setText(b.getNama());
        }
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setTitle("Tambah / Edit Barang");
        setSize(350,200);
        setLayout(null);

        JLabel lblNama = new JLabel("Nama:");
        lblNama.setBounds(10, 10, 80, 25);
        add(lblNama);

        txtNama = new javax.swing.JTextField();
        txtNama.setBounds(100, 10, 220, 25);
        add(txtNama);

        JLabel lblHarga = new JLabel("Harga:");
        lblHarga.setBounds(10, 45, 80, 25);
        add(lblHarga);

        txtHarga = new javax.swing.JTextField();
        txtHarga.setBounds(100, 45, 220, 25);
        add(txtHarga);

        JLabel lblStok = new JLabel("Stok:");
        lblStok.setBounds(10, 80, 80, 25);
        add(lblStok);

        txtStok = new javax.swing.JTextField();
        txtStok.setBounds(100, 80, 220, 25);
        add(txtStok);

        JButton btnSave = new JButton("Simpan");
        btnSave.setBounds(100, 120, 100, 30);
        btnSave.addActionListener(e -> onSave());
        add(btnSave);

        JButton btnCancel = new JButton("Batal");
        btnCancel.setBounds(220, 120, 100, 30);
        btnCancel.addActionListener(e -> onCancel());
        add(btnCancel);
    }

    private void onSave() {
        try {
            String nama = txtNama.getText().trim();
            if (nama.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama tidak boleh kosong.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String hargaText = txtHarga.getText().trim().replace(",", ".");
            BigDecimal harga = new BigDecimal(hargaText);
            int stok = Integer.parseInt(txtStok.getText().trim());

            barang.setNama(nama);

            saved = true;
            setVisible(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Input tidak valid: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Barang getBarang() { return barang; }
}
