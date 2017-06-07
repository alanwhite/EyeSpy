import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class SpeedTestSimple extends JFrame {
	static{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	private VideoCapture cap;
	
	public SpeedTestSimple() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1280, 720);
		MPanel contentPane = new MPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		setVisible(true);
		
		cap = new VideoCapture();
		cap.open(0);

		final int maxFrames = 120;
		
		Thread animator = new Thread() {
			private int frameCount = 0;
			public void run() {
				for (;;){
					if ( ++frameCount > maxFrames )
						break;
					repaint(); // can be called from any thread, schedules paint on Swing EDT
					try { Thread.sleep(30);
					} catch (InterruptedException e) {    }
				}  
				System.out.println("Asked for repaint "+frameCount+" times");
				contentPane.showResults();
				System.exit(0);
			} 
		};
		
		animator.start();
	}
	
	class MPanel extends JPanel {		
		private BufferedImage img;
		private byte[] dat;
		private Mat matRaw = new Mat();
		private Mat matFlipped = new Mat();
		private int paintCount = 0;
		private long[] readStart = new long[130];
		private long[] imageStart = new long[130];
		private long[] buffStart = new long[130];
		private long[] rasterStart = new long[130];
		private long[] drawStart = new long[130];
		private long[] drawEnd = new long[130];
			
		public void showResults() {
			System.out.println("repainted "+paintCount+" times");
			System.out.println("Count, readTime, imageTime, buffTime, rasterTime, drawTime, totalTime");
			for ( int i=0; i < paintCount; i++ ) {
				System.out.print(i+", ");
				System.out.print(imageStart[i]-readStart[i]+", ");
				System.out.print(buffStart[i]-imageStart[i]+", ");
				System.out.print(rasterStart[i]-buffStart[i]+", ");
				System.out.print(drawStart[i]-rasterStart[i]+", ");
				System.out.print(drawEnd[i]-drawStart[i]+", ");
				System.out.print(drawEnd[i]-readStart[i]+"\n");
			}
			
			System.out.println("Average readTime="+getAverage(readStart,imageStart,paintCount));
			System.out.println("Average imageTime="+getAverage(imageStart,buffStart,paintCount));
			System.out.println("Average buffTime="+getAverage(buffStart,rasterStart,paintCount));
			System.out.println("Average rasterTime="+getAverage(rasterStart,drawStart,paintCount));
			System.out.println("Average drawTime="+getAverage(drawStart,drawEnd,paintCount));
			System.out.println("Average totalTime="+getAverage(readStart,drawEnd,paintCount));
			
			System.out.println("Elapsed="+(drawEnd[paintCount-1]-readStart[0]));
			System.out.println("Approx fps achieved="+((double)paintCount/(((double)(drawEnd[paintCount-1]-readStart[0]))/1000000000.0f)));
		}
		
		private long getAverage(long[] data1, long[] data2, int count) {
			long total=0;
			for (int i=0; i<count; i++) {
				total += (data2[i]-data1[i]);
			}
			return total / count;
		}
		
		public void paintComponent(Graphics g) {
			readStart[paintCount] = System.nanoTime();
			cap.read(matRaw);
			
			imageStart[paintCount] = System.nanoTime();
			Imgproc.cvtColor(matRaw,matRaw,Imgproc.COLOR_RGB2BGR);
			Core.flip(matRaw, matFlipped, 1); 

			buffStart[paintCount] = System.nanoTime();
			int w = matFlipped.cols(), h = matFlipped.rows();
			if ( dat == null || dat.length != w * h * 3 ) {
				dat = new byte[w * h * 3];
			}

			if ( img == null 
					|| img.getWidth() != w 
					|| img.getHeight() != h
					|| img.getType() != BufferedImage.TYPE_3BYTE_BGR ) {
				img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			}
			
			rasterStart[paintCount] = System.nanoTime();
			matFlipped.get(0, 0, dat);
			img.getRaster().setDataElements(0, 0, matFlipped.cols(), matFlipped.rows(), dat);
			
			drawStart[paintCount] = System.nanoTime();
			g.drawImage(img, 0, 0, this);
			drawEnd[paintCount] = System.nanoTime();
			
			paintCount++;
		}
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SpeedTestSimple frame = new SpeedTestSimple();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
