package testsqlite;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog tambah / edit Kategori
 * - pada mode edit, id_kategori tidak dapat diubah
 * - revisi: ukuran TextField diperbaiki agar lebih lebar dan responsive
 */
public class AddEditKategoriDialog extends JDialog {
    // set kolom default agar ukuran lebih lebar
    private JTextField txtId = new JTextField(12);
    private JTextField txtNama = new JTextField(25);

    private boolean saved = false;
    private Kategori kategori;

    private boolean editMode = false;

    public AddEditKategoriDialog(Frame parent, Kategori k) {
        super(parent, true);
        this.kategori = (k == null) ? new Kategori() : k;
        this.editMode = (k != null);

        setTitle(editMode ? "Edit Kategori" : "Tambah Kategori");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();

        if (editMode) {
            txtId.setText(kategori.getIdKategori());
            txtId.setEnabled(false); // tidak boleh ubah PK saat edit
            txtNama.setText(kategori.getNamaKategori());
        }

        getRootPane().setDefaultButton(findSaveButton());
        pack();
        setLocationRelativeTo(parent);
    }

    private JButton findSaveButton() {
        // helper kecil untuk mendukung default button sebelum UI ditampilkan
        // jika belum dibuat, kembalikan null
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JPanel) {
                for (Component b : ((JPanel)c).getComponents()) {
                    if (b instanceof JButton && "Simpan".equals(((JButton)b).getText())) return (JButton)b;
                }
            }
        }
        return null;
    }

    private void initComponents() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);

        // label kolom kiri
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("ID Kategori (max 10):"), gbc);

        // field kolom kanan - mengisi ruang yang tersisa
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // sangat penting agar textfield melebar saat dialog diperbesar
        form.add(txtId, gbc);

        // baris berikutnya
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Nama Kategori:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        form.add(txtNama, gbc);

        // tambahkan sedikit padding di sekitar form
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        wrapper.add(form, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        // tombol
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());
        btns.add(bSave);
        btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        // register ESC untuk batal
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // supaya textfield bisa mengisi area saat user meresize
        txtId.setMaximumSize(new Dimension(Integer.MAX_VALUE, txtId.getPreferredSize().height));
        txtNama.setMaximumSize(new Dimension(Integer.MAX_VALUE, txtNama.getPreferredSize().height));

        // fokus awal ke field nama ketika tambah baru
        if (!editMode) {
            SwingUtilities.invokeLater(() -> txtId.requestFocusInWindow());
        }
    }

    private void onSave() {
        String id = txtId.getText().trim();
        String nama = txtNama.getText().trim();

        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID kategori harus diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (id.length() > 10) {
            JOptionPane.showMessageDialog(this, "ID kategori maksimal 10 karakter.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama kategori harus diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        kategori.setIdKategori(id);
        kategori.setNamaKategori(nama);

        // simpan via DAO di panggil dari caller (MainFrame)
        saved = true;
        setVisible(false);
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Kategori getKategori() { return kategori; }
}
