package Barang;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import Kategori.Kategori;
import Kategori.KategoriDAO;

public class AddEditBarangDialog extends JDialog {
    private JTextField txtNama = new JTextField();
    private JComboBox<Kategori> cmbKategori = new JComboBox<>();
    private boolean saved = false;
    private Barang barang;

    public AddEditBarangDialog(Frame parent, Barang b) {
        super(parent, true);
        this.barang = (b == null ? new Barang() : b);
        setTitle(b == null ? "Tambah Barang" : "Edit Barang");
        initUI();
        loadKategori();

        if (b != null) {
            txtNama.setText(b.getNama());
            // pilih kategori di combo
            for (int i = 0; i < cmbKategori.getItemCount(); i++) {
                if (cmbKategori.getItemAt(i).getIdKategori().equals(b.getIdKategori())) {
                    cmbKategori.setSelectedIndex(i);
                    break;
                }
            }
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Nama Barang:"));
        form.add(txtNama);
        form.add(new JLabel("Kategori:"));
        form.add(cmbKategori);
        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel();
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e->onSave());
        bCancel.addActionListener(e->setVisible(false));
        btns.add(bSave); btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);
    }

    private void loadKategori() {
        try {
            KategoriDAO dao = new KategoriDAO();
            List<Kategori> list = dao.findAll();
            for (Kategori k : list) cmbKategori.addItem(k);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load kategori: " + ex.getMessage());
        }
    }

    private void onSave() {
        if (txtNama.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,"Nama harus diisi.");
            return;
        }
        if (cmbKategori.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,"Pilih kategori.");
            return;
        }

        barang.setNama(txtNama.getText().trim());
        barang.setIdKategori(((Kategori)cmbKategori.getSelectedItem()).getIdKategori());
        saved = true;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Barang getBarang() { return barang; }
}
