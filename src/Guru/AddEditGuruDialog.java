/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Guru;

import javax.swing.*;
import java.awt.*;

public class AddEditGuruDialog extends JDialog {
    private JTextField txtNama = new JTextField(30);
    private JTextField txtNotelp = new JTextField(15);
    private JTextField txtJabatan = new JTextField(20);

    private boolean saved = false;
    private Guru guru;
    private boolean editMode = false;

    public AddEditGuruDialog(Frame parent, Guru g) {
        super(parent, true);
        this.guru = (g == null) ? new Guru() : g;
        this.editMode = (g != null);
        setTitle(editMode ? "Edit Guru" : "Tambah Guru");
        initComponents();

        if (editMode) {
            txtNama.setText(guru.getNamaGuru());
            txtNotelp.setText(guru.getNotelpGuru());
            txtJabatan.setText(guru.getJabatan());
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Nama Guru:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; form.add(txtNama, gbc);


        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("No Telp:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; form.add(txtNotelp, gbc);

        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("Jabatan:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL; form.add(txtJabatan, gbc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        wrapper.add(form, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan"); bSave.addActionListener(e -> onSave());
        JButton bCancel = new JButton("Batal"); bCancel.addActionListener(e -> onCancel());
        btns.add(bSave); btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        txtNama.setMaximumSize(new Dimension(Integer.MAX_VALUE, txtNama.getPreferredSize().height));
    }

    private void onSave() {
        String nama = txtNama.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama guru harus diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        guru.setNamaGuru(nama);
        guru.setNotelpGuru(txtNotelp.getText().trim());
        guru.setJabatan(txtJabatan.getText().trim());

        saved = true;
        setVisible(false);
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Guru getGuru() { return guru; }
}
