import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

/**
 * WIP. Migrate to JavaCV to ease interface to OpenCV and FFmpeg, to include audio in output file
 * 
 * @author alanwhite
 *
 */
@SuppressWarnings("serial")
public class LittleEye3 extends JFrame {
	static{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	private JPanel contentPane;
	private VideoCapture cap;
	private BufferedImage img;
	private byte[] dat;
	private Mat matRaw = new Mat();
	private Mat matFlipped = new Mat();
	private MoviePanel moviePanel = new MoviePanel();
	private Timer timer;
	private long lastFrameCount = 0;
	private long startTime = 0;

	public LittleEye3() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// setBounds(100, 100, 650, 490);
		contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder());
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		contentPane.add(moviePanel, BorderLayout.NORTH);
		contentPane.add(createControls(), BorderLayout.SOUTH);


		// open webcam
		cap = new VideoCapture();
		cap.open(0);

		Size frameSize=new Size((int)cap.get(Videoio.CAP_PROP_FRAME_WIDTH),(int)cap.get(Videoio.CAP_PROP_FRAME_HEIGHT));
		System.out.println("Width="+frameSize.width+"; Height="+frameSize.height);
		moviePanel.setPreferredSize(new Dimension((int)frameSize.width,(int)frameSize.height));

		pack();

		double fps = cap.get(Videoio.CAP_PROP_FPS);
		System.out.println("FPS="+fps);

		int desiredFPS = 32;
		int mfps = 1000/desiredFPS;
		timer = new Timer(mfps, moviePanel);
		System.out.println("set to "+desiredFPS+" desired FPS");

	}

	class MoviePanel extends JPanel implements ActionListener {
		private long frameCount = 0;
		private long readStart, imageStart, bufferStart, getRasterStart, setRasterStart, drawStart, drawDone;
		private String recordingFile = null;
		VideoWriter videoWriter = null;

		public MoviePanel() {
			setLayout(null);
		}

		public void paintComponent(Graphics g){
			frameCount++;

			readStart = System.nanoTime();
			// grab a frame from the webcam
			cap.read(matRaw);

			if ( recordingFile != null )
				videoWriter.write(matRaw);

			imageStart = System.nanoTime();
			// Play with image ....
			// Imgproc.resize(matRaw, matRaw, new Size(640.0f, 360.0f), 0, 0, Imgproc.INTER_CUBIC);
			// matRaw.convertTo(matRaw, -1, 1, 50); // brightness

			// Blue and Red channels need swapped for some reason
			Imgproc.cvtColor(matRaw,matRaw,Imgproc.COLOR_RGB2BGR);

			// adjust to see as if looking in a mirror - us humans expect that
			Core.flip(matRaw, matFlipped, 1);

			bufferStart = System.nanoTime();
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

			getRasterStart = System.nanoTime();
			// get the image data from the grabbed frame and put it in dat
			matFlipped.get(0, 0, dat);

			setRasterStart = System.nanoTime();
			// populate the buffered image with the data from the grabbed frame
			img.getRaster().setDataElements(0, 0, matFlipped.cols(), matFlipped.rows(), dat);

			drawStart = System.nanoTime();
			g.drawImage(img, 0, 0, this);
			drawDone = System.nanoTime();

			// logTimes();
		}

		private void logTimes() {
			System.out.println("Render Times: "+
					"\n\t readStart="+readStart+
					"\n\t imageStart="+imageStart+
					"\n\t bufferStart="+bufferStart+
					"\n\t getRasterStart="+getRasterStart+
					"\n\t setRasterStart="+setRasterStart+
					"\n\t drawStart="+drawStart+
					"\n\t drawDone="+drawDone+
					"\n\t TOTAL = "+(drawDone-readStart)
					);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			this.repaint();
		}

		public long getFrameCount() {
			return frameCount;
		}

		public void recordTo(String filename) {
			if ( filename == null ) {
				if ( recordingFile != null )
					videoWriter.release();
				recordingFile = null;
			} else {
				recordingFile = filename;
				final Size frameSize=new Size((int)cap.get(Videoio.CAP_PROP_FRAME_WIDTH),(int)cap.get(Videoio.CAP_PROP_FRAME_HEIGHT));
//				videoWriter=new VideoWriter(recordingFile,VideoWriter.fourcc('m', 'p', '4', 'v'),
//						cap.get(Videoio.CAP_PROP_FPS),frameSize,true);
				videoWriter=new VideoWriter(recordingFile,-1,
						cap.get(Videoio.CAP_PROP_FPS),frameSize,true);
				System.out.println(videoWriter.isOpened());
			}
		}

	}

	/**
	 * sets up the control buttons
	 * @return
	 */
	private JPanel createControls() {
		JPanel controls = new JPanel();

		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {       
				lastFrameCount = moviePanel.getFrameCount();
				startTime = System.nanoTime();
				timer.start();
			}
		});
		controls.add(btnStart);

		JButton btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moviePanel.recordTo(null);
				timer.stop();
				double elapsedTime = System.nanoTime() - startTime;
				double frames = moviePanel.getFrameCount() - lastFrameCount;
				double tfps=frames/(elapsedTime/1000000000.0f);
				System.out.println("frames="+frames+"; elapsed="+elapsedTime+"; fps="+tfps);
			}
		});
		controls.add(btnStop);

		JButton btnRecord = new JButton("Record");
		btnRecord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String fn = getRecordFile();
				moviePanel.recordTo(fn);
			}
		});
		controls.add(btnRecord);

		return controls;
	}

	private String getRecordFile() {
		JFileChooser chooser=new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.showSaveDialog(null);

		String path=chooser.getSelectedFile().getAbsolutePath();
		String filename=chooser.getSelectedFile().getName();
		System.out.println(path);
		System.out.println(filename);
		
		// must be a .mp4 suffix and must not exist
		
		return path;
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LittleEye3 frame = new LittleEye3();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}
}
