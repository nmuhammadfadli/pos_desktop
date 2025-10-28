package Hutang;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dialog menampilkan daftar receivable (piutang) dan action "Bayar"
 * Menampilkan juga nama guru pemilik voucher terkait transaksi (jika ada).
 */
public class ReceivableListDialog extends JDialog {
    private JTable tbl;
    private DefaultTableModel model;
    private ReceivableDAO dao = new ReceivableDAO();

    public ReceivableListDialog(Frame parent) {
        super(parent, true);
        setTitle("Daftar Piutang (Receivable)");
        setSize(1000, 450);
        setLayout(new BorderLayout(6,6));

        model = new DefaultTableModel(new Object[]{
                "ID","ID Transaksi","Total","Dibayar","Belum Dibayar","Diperbarui Pada","Status","Nama Guru"
        }, 0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tbl = new JTable(model);
        add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton bRefresh = new JButton("Refresh");
        JButton bPay = new JButton("Bayar");
        JButton bDelete = new JButton("Hapus");
        JButton bClose = new JButton("Tutup");

        bRefresh.addActionListener(e -> loadReceivables());
        bPay.addActionListener(e -> onBayar());
        bDelete.addActionListener(e -> onDelete());
        bClose.addActionListener(e -> setVisible(false));

        bottom.add(bRefresh); bottom.add(bPay); bottom.add(bDelete); bottom.add(bClose);
        add(bottom, BorderLayout.SOUTH);

        loadReceivables();
        setLocationRelativeTo(parent);
    }

    private void loadReceivables() {
        try {
            List<Receivable> list = dao.findAll();
            model.setRowCount(0);
            for (Receivable r : list) {
                model.addRow(new Object[]{
                        r.getIdReceivable(),
                        r.getIdTransaksi(),
                        r.getAmountTotal()==null?"0":r.getAmountTotal().toPlainString(),
                        r.getAmountPaid()==null?"0":r.getAmountPaid().toPlainString(),
                        r.getAmountOutstanding()==null?"0":r.getAmountOutstanding().toPlainString(),
                        r.getCreatedAt(),
                        r.getStatus(),
                        r.getOwnerName() == null ? "-" : r.getOwnerName()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load receivables: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onBayar() {
        int r = tbl.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris receivable terlebih dahulu."); return; }
        int id = Integer.parseInt(tbl.getValueAt(r,0).toString());
        String outStr = tbl.getValueAt(r,4).toString();
        BigDecimal outstanding = new BigDecimal(outStr);

        String s = JOptionPane.showInputDialog(this, "Masukkan jumlah pembayaran (outstanding: " + outstanding.toPlainString() + "):");
        if (s == null) return;
        try {
            BigDecimal paid = new BigDecimal(s.trim());
            if (paid.compareTo(BigDecimal.ZERO) <= 0) { JOptionPane.showMessageDialog(this, "Jumlah harus > 0"); return; }

            dao.applyPayment(id, paid);
            JOptionPane.showMessageDialog(this, "Pembayaran dicatat.");
            loadReceivables();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Input tidak valid: " + nfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mencatat pembayaran: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int r = tbl.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris receivable terlebih dahulu."); return; }
        int id = Integer.parseInt(tbl.getValueAt(r,0).toString());
        if (JOptionPane.showConfirmDialog(this, "Hapus piutang ID " + id + " ?") == JOptionPane.YES_OPTION) {
            try {
                dao.delete(id);
                loadReceivables();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
