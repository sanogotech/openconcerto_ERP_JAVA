package org.openconcerto.modules.ocr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InvoiceRendererComponent extends JComponent {
	private BufferedImage image;
	private int mX = -1000;
	private int mY = -1000;

	public InvoiceRendererComponent(InvoiceOCR invoice, final OCRPage page) throws IOException {
		this.image = new BufferedImage(page.getImage().getWidth(), page
				.getImage().getHeight(), BufferedImage.TYPE_INT_ARGB);

		page.getImage();

		this.addMouseListener(new MouseAdapter() {
			public void mouseExited(MouseEvent e) {
				setPos(-1000, -1000);
			};

			@Override
			public void mouseClicked(MouseEvent e) {
				JFrame f = new JFrame();
				StringBuilder b = new StringBuilder();
				List<OCRLine> lines = page.getLines();
				int i = 0;
				for (OCRLine ocrLine : lines) {
					b.append(i);
					b.append(":");
					b.append(ocrLine.getText());
					b.append("\n");
					i++;
				}
				JTextArea area = new JTextArea(b.toString());
				f.setContentPane(new JScrollPane(area));
				f.setSize(new Dimension(500, 500));
				f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				f.setVisible(true);

			}
		});
		this.addMouseMotionListener(new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				setPos(e.getX(), e.getY());
			}

		});
		Graphics g = this.image.getGraphics();
		g.drawImage(page.getImage(), 0, 0, null);
		g.setColor(new Color(255, 255, 0, 60));
		List<OCRLine> lines = invoice.getHighlight(page);
		for (OCRLine ocrLine : lines) {

			final int x = ocrLine.getxMin();
			int w = ocrLine.getxMax() - x;
			final int y = ocrLine.getyMin();
			int h = ocrLine.getyMax() - y;

			g.fillRect(x, y, w, h);
		}
		g.dispose();
	}

	protected void setPos(int x, int y) {
		this.mX = x;
		this.mY = y;
		repaint();
	}

	@Override
	public void paint(Graphics g) {

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		double ratio = (double) this.image.getWidth()
				/ (double) this.image.getHeight();

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		g.drawImage(this.image, 0, 0, this.getWidth(),
				(int) Math.round(this.getWidth() / ratio), 0, 0,
				this.image.getWidth(), this.image.getHeight(), null);
		double zoomRatio = (double) this.getWidth()
				/ (double) this.image.getWidth();

		int B_W = 150;
		int B_H = 90;
		int B_W2 = B_W / 2;
		int B_H2 = B_H / 2;

		// Destination
		final int dx1 = this.mX - B_W2;
		final int dy1 = this.mY - B_H2;
		final int dx2 = dx1 + B_W;
		final int dy2 = dy1 + B_H;

		// Source
		final int sx1 = (int) ((dx1 + B_W2 / 2) / zoomRatio);
		final int sy1 = (int) ((dy1 + B_H2 / 2) / zoomRatio);
		final int sx2 = (int) ((dx2 - B_W2 / 2) / zoomRatio);
		final int sy2 = (int) ((dy2 - B_H2 / 2) / zoomRatio);

		g.drawImage(this.image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
		g.setColor(new Color(232, 242, 255));

		g.drawRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
		g.drawRect(dx1 - 1, dy1 - 1, dx2 - dx1 + 2, dy2 - dy1 + 2);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = getParent().getSize();
		return new Dimension(d.width, (int) Math.round(((d.width * this.image
				.getHeight()) / (double) this.image.getWidth())));
	}
}
