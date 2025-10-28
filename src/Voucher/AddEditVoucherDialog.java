package Voucher;

import Guru.Guru;
import Guru.GuruDAO;
import testsqlite.DatabaseHelper;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Dialog tambah / edit Voucher.
 * - Jika membuat baru, kode di-generate otomatis (VCHR001, VCHR002, ...)
 * - Combo bulan berisi nama-nama bulan
 * - Combo guru diisi dari GuruDAO; opsi "Tidak ada" => idGuru = 0
 * - Saat simpan, dilakukan validasi sederhana (kode wajib, saldo angka, kode unik saat tambah)
 */
public class AddEditVoucherDialog extends JDialog {
    private final Frame parentFrame;

    private JTextField txtKode = new JTextField(16);
    private JComboBox<String> cmbBulan;
    private JComboBox<Guru> cmbGuru = new JComboBox<>();
    private JTextField txtSaldo = new JTextField(12);

    private boolean saved = false;
    private Voucher voucher;

    public AddEditVoucherDialog(Frame parent, Voucher v) {
        super(parent, true);
        this.parentFrame = parent;
        this.voucher = (v == null) ? new Voucher() : v;
        setTitle(v == null ? "Tambah Voucher" : "Edit Voucher");
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        // bulan
        String[] months = { "Januari","Februari","Maret","April","Mei","Juni",
                "Juli","Agustus","September","Oktober","November","Desember" };
        cmbBulan = new JComboBox<>(months);

        // top form
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0;
        top.add(new JLabel("Kode:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        top.add(txtKode, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        top.add(new JLabel("Bulan:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        top.add(cmbBulan, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        top.add(new JLabel("Guru:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        top.add(cmbGuru, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        top.add(new JLabel("Saldo:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        top.add(txtSaldo, gbc);

        add(top, BorderLayout.CENTER);

        // buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bottom.add(bSave);
        bottom.add(bCancel);
        add(bottom, BorderLayout.SOUTH);

        // actions
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());

        // esc cancel
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(bSave);

        // renderer for guru combo (show "id - nama" or "-")
        cmbGuru.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Guru) {
                    Guru g = (Guru) value;
                    if (g.getIdGuru() == 0) setText("-"); else setText(g.getIdGuru() + " - " + (g.getNamaGuru() == null ? "-" : g.getNamaGuru()));
                } else {
                    setText(value == null ? "-" : value.toString());
                }
                return this;
            }
        });

        // load guru list
        loadGuruList();

        // fill fields if editing
        if (voucher != null && voucher.getIdVoucher() != 0) {
            // editing existing
            txtKode.setText(voucher.getKode());
            txtKode.setEditable(true); // allow editing if you want; unique check will be applied
            txtSaldo.setText(voucher.getCurrentBalance() == null ? "0" : voucher.getCurrentBalance().toPlainString());

            // set bulan
            String vb = voucher.getBulan();
            if (vb != null) {
                for (int i = 0; i < cmbBulan.getItemCount(); i++) {
                    String item = cmbBulan.getItemAt(i);
                    if (vb.equalsIgnoreCase(item)) { cmbBulan.setSelectedIndex(i); break; }
                }
            }

            // select guru
            int idGuru = voucher.getIdGuru();
            for (int i = 0; i < cmbGuru.getItemCount(); i++) {
                Object item = cmbGuru.getItemAt(i);
                if (item instanceof Guru && ((Guru) item).getIdGuru() == idGuru) {
                    cmbGuru.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            // tambah baru: generate kode otomatis
            try {
                String next = DatabaseHelper.generateNextVoucherCode();
                txtKode.setText(next);
                txtKode.setEditable(false); // non-editable by default for consistency
            } catch (Exception ex) {
                // jika gagal, biarkan user isi manual
                System.err.println("Gagal generate kode voucher otomatis: " + ex.getMessage());
                txtKode.setEditable(true);
            }
            // defaults
            if (cmbBulan.getItemCount() > 0) cmbBulan.setSelectedIndex(0);
            if (cmbGuru.getItemCount() > 0) cmbGuru.setSelectedIndex(0);
            txtSaldo.setText("0");
        }
    }

    private void loadGuruList() {
        cmbGuru.removeAllItems();
        try {
            // "Tidak ada" option
            Guru none = new Guru();
            none.setIdGuru(0);
            none.setNamaGuru("-");
            cmbGuru.addItem(none);

            GuruDAO dao = new GuruDAO();
            List<Guru> list = dao.findAll();
            if (list != null) {
                for (Guru g : list) cmbGuru.addItem(g);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load daftar guru: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        JButton defaultBtn = (JButton) getRootPane().getDefaultButton();
        if (defaultBtn != null) defaultBtn.setEnabled(false);

        try {
            String kode = txtKode.getText().trim();
            if (kode.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kode harus diisi.", "Input error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // parse saldo
            BigDecimal saldo;
            String s = txtSaldo.getText().trim();
            if (s.isEmpty()) saldo = BigDecimal.ZERO;
            else {
                try { saldo = new BigDecimal(s); }
                catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Saldo harus berupa angka.", "Input error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // bulan
            String bulan = (String) cmbBulan.getSelectedItem();

            // id guru
            Object sel = cmbGuru.getSelectedItem();
            int idGuru = 0;
            if (sel instanceof Guru) idGuru = ((Guru) sel).getIdGuru();

            // uniqueness check for kode (jika tambah atau saat edit dan kode diubah)
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id_voucher FROM kode_voucher WHERE kode = ?")) {
                ps.setString(1, kode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int foundId = rs.getInt(1);
                        int currentId = voucher == null ? 0 : voucher.getIdVoucher();
                        if (currentId == 0 || foundId != currentId) {
                            // ada voucher lain dengan kode sama
                            JOptionPane.showMessageDialog(this, "Kode sudah digunakan oleh voucher lain.", "Validation error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }

            // set to model
            voucher.setKode(kode);
            voucher.setBulan(bulan);
            voucher.setIdGuru(idGuru); // primitive int in model (0 = none)
            voucher.setCurrentBalance(saldo);

            saved = true;
            setVisible(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal saat menyimpan voucher: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (defaultBtn != null) defaultBtn.setEnabled(true);
        }
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Voucher getVoucher() { return voucher; }
}
