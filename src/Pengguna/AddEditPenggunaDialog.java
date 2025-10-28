/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Pengguna;

/**
 *
 * @author COMPUTER
 */

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class AddEditPenggunaDialog extends JDialog {
    private JTextField txtId = new JTextField(12);
    private JTextField txtUsername = new JTextField(20);
    private JPasswordField txtPassword = new JPasswordField(20);
    private JTextField txtNama = new JTextField(30);
    private JTextField txtJabatan = new JTextField(20);
    private JTextArea txtAlamat = new JTextArea(3, 30);
    private JComboBox<String> cbHakAkses;
    private JTextField txtEmail = new JTextField(30);
    private JTextField txtNotelp = new JTextField(14);

    private boolean saved = false;
    private Pengguna pengguna;

    public AddEditPenggunaDialog(Frame parent, Pengguna p) {
        super(parent, true);
        this.pengguna = (p == null) ? new Pengguna() : p;
        setTitle(p == null ? "Tambah Pengguna" : "Edit Pengguna");
        initComponents();
        if (p != null) loadToForm(p);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.anchor = GridBagConstraints.EAST;

        // ID (readonly for edit/new — you can show generated ID after insert)
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("ID Pengguna:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtId.setEditable(false);
        form.add(txtId, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtPassword, gbc);

        // Nama lengkap
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Nama Lengkap:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtNama, gbc);

        // Jabatan
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Jabatan:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtJabatan, gbc);

        // Alamat
        gbc.gridx = 0; gbc.gridy = 5; gbc.anchor = GridBagConstraints.NORTHEAST;
        form.add(new JLabel("Alamat:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.BOTH;
        txtAlamat.setLineWrap(true);
        txtAlamat.setWrapStyleWord(true);
        form.add(new JScrollPane(txtAlamat), gbc);

        // hak akses
        gbc.gridx = 0; gbc.gridy = 6; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Hak Akses:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        cbHakAkses = new JComboBox<>(new String[] { "0 - Pengguna", "1 - Admin" });
        form.add(cbHakAkses, gbc);

        // email
        gbc.gridx = 0; gbc.gridy = 7; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtEmail, gbc);

        // no telp
        gbc.gridx = 0; gbc.gridy = 8; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("No. Telp:"), gbc);
        gbc.gridx = 1; gbc.gridy = 8; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtNotelp, gbc);

        add(form, BorderLayout.CENTER);

        // buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());
        btns.add(bSave); btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(bSave);
        getRootPane().registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void loadToForm(Pengguna p) {
        if (p == null) return;
        txtId.setText(p.getIdPengguna());
        txtUsername.setText(p.getUsername());
        txtPassword.setText(p.getPassword());
        txtNama.setText(p.getNamaLengkap());
        txtJabatan.setText(p.getJabatan());
        txtAlamat.setText(p.getAlamat());
        if (p.getHakAkses() != null) cbHakAkses.setSelectedIndex(p.getHakAkses() == 1 ? 1 : 0);
        txtEmail.setText(p.getEmail());
        txtNotelp.setText(p.getNotelpPengguna());
    }

    private void onSave() {
        try {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword()).trim();
            String nama = txtNama.getText().trim();
            String jabatan = txtJabatan.getText().trim();
            String alamat = txtAlamat.getText().trim();
            int hak = cbHakAkses.getSelectedIndex() == 1 ? 1 : 0;
            String email = txtEmail.getText().trim();
            String notelp = txtNotelp.getText().trim();

            if (username.isEmpty()) { JOptionPane.showMessageDialog(this, "Username harus diisi"); return; }
            // optional: require password when creating new user
            if (pengguna.getIdPengguna() == null || pengguna.getIdPengguna().trim().isEmpty()) {
                if (password.isEmpty()) { JOptionPane.showMessageDialog(this, "Password harus diisi untuk pengguna baru"); return; }
            }

            pengguna.setUsername(username);
            pengguna.setPassword(password);
            pengguna.setNamaLengkap(nama);
            pengguna.setJabatan(jabatan);
            pengguna.setAlamat(alamat);
            pengguna.setHakAkses(hak);
            pengguna.setEmail(email);
            pengguna.setNotelpPengguna(notelp);

            // persist
            PenggunaDAO dao = new PenggunaDAO();
            if (pengguna.getIdPengguna() == null || pengguna.getIdPengguna().trim().isEmpty()) {
                // insert -> sets generated id in model as side-effect
                String newId = dao.insert(pengguna);
                pengguna.setIdPengguna(newId);
                txtId.setText(newId);
            } else {
                dao.update(pengguna);
            }

            saved = true;
            setVisible(false);
        } catch (SQLException sqe) {
            JOptionPane.showMessageDialog(this, "Gagal simpan pengguna: " + sqe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            sqe.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Pengguna getPengguna() { return pengguna; }
}
