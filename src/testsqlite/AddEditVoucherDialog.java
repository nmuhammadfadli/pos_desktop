package testsqlite;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dialog tambah / edit Voucher.
 * Menambahkan dropdown Bulan. Combo guru diisi dari GuruDAO.
 * idGuru disimpan sebagai primitive int (0 = tidak ada).
 */
public class AddEditVoucherDialog extends JDialog {
    private JTextField txtKode = new JTextField();
    private JComboBox<String> cmbBulan;
    private JComboBox<Guru> cmbGuru = new JComboBox<>();
    private JTextField txtSaldo = new JTextField();
    private boolean saved = false;
    private Voucher voucher;

    public AddEditVoucherDialog(Frame parent, Voucher v) {
        super(parent, true);
        this.voucher = (v == null ? new Voucher() : v);
        setTitle(v == null ? "Tambah Voucher" : "Edit Voucher");
        initUI();
        loadGuruList();

        // jika edit: isi nilai awal (lakukan setelah loadGuruList)
        if (v != null) {
            txtKode.setText(v.getKode());
            txtSaldo.setText(v.getCurrentBalance() == null ? "0" : v.getCurrentBalance().toPlainString());

            // set bulan (cocokkan berdasarkan nama bulan)
            String vb = v.getBulan();
            if (vb != null) {
                for (int i = 0; i < cmbBulan.getItemCount(); i++) {
                    String item = cmbBulan.getItemAt(i);
                    if (vb.equalsIgnoreCase(item)) {
                        cmbBulan.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // pilih guru di combo (cari id match)
            int idGuru = v.getIdGuru();
            for (int i = 0; i < cmbGuru.getItemCount(); i++) {
                Guru g = cmbGuru.getItemAt(i);
                if (g != null && g.getIdGuru() == idGuru) {
                    cmbGuru.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            // default: pilih bulan pertama dan "Tidak ada" guru (index 0)
            if (cmbBulan.getItemCount() > 0) cmbBulan.setSelectedIndex(0);
            if (cmbGuru.getItemCount() > 0) cmbGuru.setSelectedIndex(0);
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(8,8));

        // Bulan: nama bulan (sesuaikan jika mau angka "01","02"...)
        String[] months = { "Januari","Februari","Maret","April","Mei","Juni",
                "Juli","Agustus","September","Oktober","November","Desember" };
        cmbBulan = new JComboBox<>(months);

        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Kode:"));
        form.add(txtKode);

        form.add(new JLabel("Bulan:"));
        form.add(cmbBulan);

        form.add(new JLabel("Guru:"));
        form.add(cmbGuru);

        form.add(new JLabel("Saldo:"));
        form.add(txtSaldo);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel();
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> { saved = false; dispose(); });
        btns.add(bSave); btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        // Renderer supaya combo menunjukkan "id - nama" atau "-" untuk opsi none
        cmbGuru.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Guru) {
                    Guru g = (Guru) value;
                    if (g.getIdGuru() == 0) {
                        setText("-"); // opsi "tidak ada"
                    } else {
                        setText(g.getIdGuru() + " - " + (g.getNamaGuru() == null ? "-" : g.getNamaGuru()));
                    }
                } else {
                    setText(value == null ? "-" : value.toString());
                }
                return this;
            }
        });
    }

    private void loadGuruList() {
        try {
            // tambahkan opsi "Tidak ada" -> idGuru = 0
            Guru none = new Guru();
            none.setIdGuru(0);
            none.setNamaGuru("-");
            cmbGuru.addItem(none);

            GuruDAO dao = new GuruDAO();
            List<Guru> list = dao.findAll();
            for (Guru g : list) {
                cmbGuru.addItem(g);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load daftar guru: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        if (txtKode.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode harus diisi.");
            return;
        }

        // ambil nilai idGuru dari selected item
        Object sel = cmbGuru.getSelectedItem();
        int idGuruVal = 0;
        if (sel instanceof Guru) {
            idGuruVal = ((Guru) sel).getIdGuru(); // 0 = tidak ada
        }

        // parse saldo (bisa kosong)
        BigDecimal saldo;
        String s = txtSaldo.getText().trim();
        if (s.isEmpty()) {
            saldo = BigDecimal.ZERO;
        } else {
            try {
                saldo = new BigDecimal(s);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Saldo harus berupa angka.", "Input error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // ambil bulan
        String bulan = (String) cmbBulan.getSelectedItem();

        voucher.setKode(txtKode.getText().trim());
        voucher.setIdGuru(idGuruVal);        // primitive int
        voucher.setCurrentBalance(saldo);
        voucher.setBulan(bulan);            // simpan nama bulan sebagai String

        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
    public Voucher getVoucher() { return voucher; }
}
