/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class TransactionListDialog extends JDialog {
    private JTable tblTrans;
    private DefaultTableModel transModel;

    private JTable tblItems;
    private DefaultTableModel itemsModel;

    private TransactionDAO txDao = new TransactionDAO();

    public TransactionListDialog(Frame parent) {
        super(parent, true);
        setTitle("Daftar Transaksi");
        setSize(900,600);
        setLayout(new BorderLayout(6,6));

        transModel = new DefaultTableModel(new Object[]{"ID","Kode","Tanggal","Total Harga","Total Bayar","Kembalian","Metode","Voucher"},0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tblTrans = new JTable(transModel);
        tblTrans.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane spTrans = new JScrollPane(tblTrans);
        spTrans.setPreferredSize(new Dimension(880, 300));
        add(spTrans, BorderLayout.NORTH);

        itemsModel = new DefaultTableModel(new Object[]{"ID DetailPenj","ID DetailBarang","Qty","Harga Unit","Subtotal"},0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tblItems = new JTable(itemsModel);
        JScrollPane spItems = new JScrollPane(tblItems);
        spItems.setPreferredSize(new Dimension(880, 220));
        add(spItems, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bRefresh = new JButton("Refresh");
        JButton bClose = new JButton("Tutup");
        bRefresh.addActionListener(e -> loadTransactions());
        bClose.addActionListener(e -> setVisible(false));
        bottom.add(bRefresh); bottom.add(bClose);
        add(bottom, BorderLayout.SOUTH);

        // selection listener: ketika pilih transaksi, load items
        tblTrans.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) onTransSelected();
            }
        });

        loadTransactions();
        setLocationRelativeTo(parent);
    }

    private void loadTransactions() {
        try {
            List<TransactionRecord> list = txDao.findAllTransactions();
            transModel.setRowCount(0);
            for (TransactionRecord tr : list) {
                String th = tr.getTotalHarga() == null ? "0" : tr.getTotalHarga().toPlainString();
                String tb = tr.getTotalBayar() == null ? "0" : tr.getTotalBayar().toPlainString();
                String kb = tr.getKembalian() == null ? "0" : tr.getKembalian().toPlainString();
                String v = tr.getIdVoucher() == null ? "" : String.valueOf(tr.getIdVoucher());
                transModel.addRow(new Object[]{ tr.getIdTransaksi(), tr.getKodeTransaksi(), tr.getTglTransaksi(), th, tb, kb, tr.getPaymentMethod(), v });
            }
            itemsModel.setRowCount(0); // kosongkan detail
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load transaksi: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onTransSelected() {
        int r = tblTrans.getSelectedRow();
        if (r < 0) return;
        try {
            long id = Long.parseLong(tblTrans.getValueAt(r,0).toString());
            List<TransactionItem> items = txDao.findItemsByTransaction(id);
            itemsModel.setRowCount(0);
            for (TransactionItem it : items) {
                String harga = it.getHargaUnit() == null ? "0" : it.getHargaUnit().toPlainString();
                String sub = it.getSubtotal() == null ? "0" : it.getSubtotal().toPlainString();
                itemsModel.addRow(new Object[]{ it.getIdDetailPenjualan(), it.getIdDetailBarang(), it.getJumlahBarang(), harga, sub });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load detail transaksi: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
