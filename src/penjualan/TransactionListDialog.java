package penjualan;

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
        setSize(1000, 600);
        setLayout(new BorderLayout(6,6));

        // ==================== HEADER TABEL TRANSAKSI ====================
        transModel = new DefaultTableModel(
            new Object[]{ "ID", "Kode", "Tanggal", "Kasir", "Guru", 
                          "Total Harga", "Total Bayar", "Kembalian", "Metode" },
            0
        ) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };

        tblTrans = new JTable(transModel);
        tblTrans.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTrans.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Lebar kolom
        int[] widths = {50, 100, 130, 130, 130, 100, 100, 100, 90};
        for (int i = 0; i < widths.length; i++) {
            tblTrans.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Sembunyikan kolom ID (tetap dibutuhkan untuk ambil detail transaksi)
        tblTrans.getColumnModel().getColumn(0).setMinWidth(0);
        tblTrans.getColumnModel().getColumn(0).setMaxWidth(0);
        tblTrans.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane spTrans = new JScrollPane(tblTrans);
        spTrans.setPreferredSize(new Dimension(980, 300));
        add(spTrans, BorderLayout.NORTH);

        // ==================== DETAIL ITEM TRANSAKSI ====================
        itemsModel = new DefaultTableModel(
            new Object[]{ "ID Detail", "ID Barang", "Nama Barang", "Qty", "Harga Unit", "Subtotal" },
            0
        ) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };

        tblItems = new JTable(itemsModel);
        tblItems.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths2 = {80, 80, 200, 60, 100, 100};
        for (int i = 0; i < widths2.length; i++) {
            tblItems.getColumnModel().getColumn(i).setPreferredWidth(widths2[i]);
        }

        JScrollPane spItems = new JScrollPane(tblItems);
        spItems.setPreferredSize(new Dimension(980, 220));
        add(spItems, BorderLayout.CENTER);

        // ==================== TOMBOL BAWAH ====================
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bRefresh = new JButton("Refresh");
        JButton bClose = new JButton("Tutup");
        bRefresh.addActionListener(e -> loadTransactions());
        bClose.addActionListener(e -> setVisible(false));
        bottom.add(bRefresh);
        bottom.add(bClose);
        add(bottom, BorderLayout.SOUTH);

        // Listener: klik transaksi => tampilkan item
        tblTrans.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) onTransSelected();
            }
        });

        loadTransactions();
        setLocationRelativeTo(parent);
    }

    // ==================== LOAD DATA TRANSAKSI ====================
    private void loadTransactions() {
        try {
            List<TransactionRecord> list = txDao.findAllTransactions();
            transModel.setRowCount(0);

            for (TransactionRecord tr : list) {
                String th = tr.getTotalHarga() == null ? "0" : tr.getTotalHarga().toPlainString();
                String tb = tr.getTotalBayar() == null ? "0" : tr.getTotalBayar().toPlainString();
                String kb = tr.getKembalian() == null ? "0" : tr.getKembalian().toPlainString();
                String kasir = tr.getNamaKasir() == null ? "-" : tr.getNamaKasir();
                String guru = tr.getNamaGuru() == null ? "-" : tr.getNamaGuru();

                transModel.addRow(new Object[]{
                    tr.getIdTransaksi(),
                    tr.getKodeTransaksi(),
                    tr.getTglTransaksi(),
                    kasir,
                    guru,
                    th, tb, kb,
                    tr.getPaymentMethod()
                });
            }

            itemsModel.setRowCount(0); // kosongkan tabel detail
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal load transaksi: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== LOAD DETAIL ITEM ====================
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
                itemsModel.addRow(new Object[]{
                    it.getIdDetailPenjualan(),
                    it.getIdDetailBarang(),
                    it.getNamaBarang(),
                    it.getJumlahBarang(),
                    harga,
                    sub
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal load detail transaksi: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
