import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Taken from an opencv example at http://computervisionandjava.blogspot.co.uk/2013/10/java-opencv-webcam.html
 * 
 * Observation, without any form of processing, it shows the image as captured. We expect to see a mirror 
 * image of ourselves, ie raise right hand it's on our right, so we need to flip it somehow for a realtime view
 * but keep it unprocessed for saving to disk.
 * 
 * @author alanwhite
 *
 */
@SuppressWarnings("serial")
public class LittleEye extends JFrame {
	static{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	private JPanel contentPane;
	private VideoCapture cap;
	private BufferedImage img;
	private byte[] dat;
	private Mat matRaw = new Mat();
	private Mat matFlipped = new Mat();

	public LittleEye() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 650, 490);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// open webcam
		cap = new VideoCapture();
		// by default we are 1280 x 720 on my mac, let's make it go old school 640 x 480 - doesn't work
//		cap.set(Videoio.CAP_PROP_FRAME_WIDTH, 640.0f);
//		cap.set(Videoio.CAP_PROP_FRAME_WIDTH, 480.0f);
		
		cap.open(0);

		
		Size frameSize=new Size((int)cap.get(Videoio.CAP_PROP_FRAME_WIDTH),(int)cap.get(Videoio.CAP_PROP_FRAME_HEIGHT));
		System.out.println("Width="+frameSize.width+"; Height="+frameSize.height);
		
		double fps = cap.get(Videoio.CAP_PROP_FPS);
		System.out.println("FPS="+fps);
		
		
		
		// start the thread that triggers the regular capture
		new Animator().start();
	}

	public void paint(Graphics g){
		long startT=System.nanoTime();
		g = contentPane.getGraphics();
		
		// grab a frame from the webcam
		cap.read(matRaw);
		
		// Play with image ....
		// Imgproc.resize(matRaw, matRaw, new Size(640.0f, 360.0f), 0, 0, Imgproc.INTER_CUBIC);
		// matRaw.convertTo(matRaw, -1, 1, 50); // brightness
	
		// Blue and Red channels need swapped for some reason
		Imgproc.cvtColor(matRaw,matRaw,Imgproc.COLOR_RGB2BGR);
		
		// adjust to see as if looking in a mirror - us humans expect that
		Core.flip(matRaw, matFlipped, 1);
		
		// see if we need to allocate space in the target byte array for image conversion
		int w = matFlipped.cols(), h = matFlipped.rows();
		if ( dat == null || dat.length != w * h * 3 ) {
			dat = new byte[w * h * 3];
		}

		// see if we need to allocate a buffered image in the correct shape
		if ( img == null 
				|| img.getWidth() != w 
				|| img.getHeight() != h
				|| img.getType() != BufferedImage.TYPE_3BYTE_BGR ) {

			img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		}
		
		// get the image data from the grabbed frame and put it in dat
		matFlipped.get(0, 0, dat);
		
		// populate the buffered image with the data from the grabbed frame
		img.getRaster().setDataElements(0, 0, matFlipped.cols(), matFlipped.rows(), dat);
		
//		long startD = System.nanoTime();
		g.drawImage(img, 0, 0, this);
//		long stopD = System.nanoTime();
//		System.out.println("Elapsed="+(System.nanoTime()-startT));
//		System.out.println("Draw="+(stopD-startD));
	}

	class Animator extends Thread{
		@Override
		public void run() {
			for (;;){
				repaint(); // can be called from any thread, schedules paint on Swing EDT
				try { Thread.sleep(30);
				} catch (InterruptedException e) {    }
			}  
		} 
	}

	private JPanel createControls() {
		JPanel controls = new JPanel();
		
		return null;
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LittleEye frame = new LittleEye();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

}
