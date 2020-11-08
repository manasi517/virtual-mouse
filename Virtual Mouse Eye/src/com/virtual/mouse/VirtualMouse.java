/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.virtual.mouse;

import com.sun.java_cup.internal.runtime.virtual_parse_stack;
import com.virtual.mouse.commands.MouseCommand;
import static com.virtual.mouse.commands.MouseCommand.BUTTON_LEFT;
import static com.virtual.mouse.commands.MouseCommand.BUTTON_RIGHT;
import static com.virtual.mouse.commands.MouseCommand.OPERATION_CLICK;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import static org.opencv.highgui.Highgui.CV_CAP_PROP_FRAME_HEIGHT;
import static org.opencv.highgui.Highgui.CV_CAP_PROP_FRAME_WIDTH;
import org.opencv.highgui.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;

/**
 *
 * @author Admin
 */
public class VirtualMouse extends javax.swing.JFrame {

    private DaemonRGBThread myRGBThread;
    private Robot rb;
    private MouseCommand mc;
    private Thread rgbT;
    private final VideoCapture video = new VideoCapture();
    private final VideoResolution vr = new VideoResolution();
    Toolkit toolkit = Toolkit.getDefaultToolkit();

    private Graphics graphics;
    volatile BufferedImage img = null;
    private final Mat imgX = new Mat();
    int resizeCount = 0;
    private static int width = 0;
    private static int height = 0;

    public static final String PARENT = System.getProperty("user.dir");
    private static final String CASCADE_FILE_EYE = PARENT + "\\resources\\haarcascade_eye_tree_eyeglasses.xml";
    private static final String CASCADE_FILE_FACE = PARENT + "\\resources\\haarcascade_frontalface_alt_tree.xml";
    private static final String CASCADE_FILE_nose = PARENT + "\\resources\\haarcascade_mcs_nose.xml";

    MatOfRect faceDetections2 = new MatOfRect();
    
    private CascadeClassifier FaceCascade = null;
    private CascadeClassifier EyeDetector = null;
    private CascadeClassifier noseDetector = null;

    private MatOfRect eyeDetections = null;
    private MatOfRect noseDetections = null;
    private MatOfRect mFaces = null;

    private Rect[] eyeArray = null;
    private Rect[] nose = null;
    private Rect[] facesArray = null;

    private static final float FACE_SIZE_PERCENTAGE = 0.3f;
    private static final Scalar mClr1 = new Scalar(0, 255, 0, 255);
    private static final Point mPt1 = new Point();
    private static final Point mPt2 = new Point();

    private int mAbsoluteFaceSize;
    Image castle;
        BufferedImage bufferedImage;

    /**
     * Daemon Thread to framing the video or camera
     */
    class DaemonRGBThread implements Runnable {

        protected volatile boolean runnable = false;
        protected volatile boolean runnableMouse = false;
        private volatile int mBlinkSDCTime = 0;
        private volatile int mBlinkRCTime = 0;

        private volatile int mPreviousEyesState = -1;
        private volatile boolean mIsEyeClosingDetected = false;
        private volatile boolean mIsAlreadyElapsed = false;
        private volatile long mBlinkStartTime = 0;
        private volatile long mBlinkEndTime = 0;
        private volatile int mBlinkCounter = 0;
        

        @Override
        public void run() {
            synchronized (this) {
                while (runnable) {

                    /**
                     * Returns true if video capturing has been initialized
                     * already. If the previous call to VideoCapture constructor
                     * or VideoCapture.open succeeded, the method returns true
                     */
                    if (video.isOpened()) {
                        /**
                         * Grabs, decodes and returns the next video frame.
                         */
                        video.read(imgX);

                        Size mMinSize = new Size(mAbsoluteFaceSize, mAbsoluteFaceSize);
                        Size mMaxSize = new Size();

                        /**
                         * Detects objects of different sizes in the input
                         * image. The detected objects are returned as a list of
                         * rectangles.
                         *
                         */
                        FaceCascade.detectMultiScale(imgX, mFaces, 1.1, 2, 2, mMinSize, mMaxSize);
                        
                        facesArray = mFaces.toArray();

                        /*
                         *  In case of simplicity first detected face is used
                         *  Replace comment from FOR-loop in case of advances detection
                         */
                        if (facesArray.length > 0) {
                            int i = 0;
                            /*
                             *  Face rectangle are used for debug purposes
                             *  Replace comments if has to debug
                             */
                            Mat faceROI = imgX.submat(facesArray[i]);

                            /**
                             * Detects objects of different sizes in the input
                             * image. The detected objects are returned as a
                             * list of rectangles. The function is parallelized
                             * with the TBB library
                             */
                            noseDetector.detectMultiScale(faceROI, noseDetections);
                            EyeDetector.detectMultiScale(faceROI, eyeDetections);
                            
                            eyeArray = eyeDetections.toArray();
                            nose=noseDetections.toArray();
                            System.out.println("nose length "+nose.length);
                            int timelaps = 0;
                            /**
                             * eye blinker logic
                             */
                            if (eyeArray.length < 2 && mPreviousEyesState == 2) {
                                mBlinkStartTime = System.currentTimeMillis();
                                mIsEyeClosingDetected = true;
                            } else if (eyeArray.length == 2 && mIsEyeClosingDetected && !mIsAlreadyElapsed) {
                                mBlinkEndTime = System.currentTimeMillis();
                                timelaps = (int) (mBlinkEndTime - mBlinkStartTime);
                                int SingleDoubleCTime = (int) jSpinner1.getValue();
                                int RightCTime = (int) jSpinner2.getValue();
                                if (timelaps > SingleDoubleCTime && timelaps < RightCTime) {
                                    System.out.println("ELAPSE TIME :" + timelaps + " Single CLICK: " + SingleDoubleCTime + " : " + (mBlinkCounter++));
                                    jLabel16.setText("LEFT CLICK");
                                    /**
                                     * send mouse keys to system
                                     */
                                    if (runnableMouse) {
                                        mc.assignButtonFromCode(BUTTON_LEFT);
                                        mc.execute(OPERATION_CLICK, null);
                                    }
                                } else if (timelaps > RightCTime) {
                                    System.out.println("ELAPSE TIME :" + timelaps + " RIGHT CLICK: " + RightCTime + " : " + (mBlinkCounter++));
                                    jLabel16.setText("RIGHT CLICK");
                                    /**
                                     * send mouse keys to system
                                     */
                                    if (runnableMouse) {
                                        mc.assignButtonFromCode(BUTTON_RIGHT);
                                        mc.execute(OPERATION_CLICK, null);
                                    }
                                }
                                mIsEyeClosingDetected = false;
                                mIsAlreadyElapsed = true;
                            } else if (eyeArray.length == 2 && mIsEyeClosingDetected && mIsAlreadyElapsed) {
                                mBlinkEndTime = System.currentTimeMillis();
                                int time = timelaps - (int) mBlinkEndTime;
                                System.out.println("ELAPSE TIME :" + time + " DOUBLE CLICK: " + (mBlinkCounter++));
                                jLabel16.setText("DOUBLE CLICK");
                                /**
                                 * send mouse keys to system
                                 */
                                if (runnableMouse) {
                                    mc.assignButtonFromCode(BUTTON_LEFT);
                                    mc.execute(OPERATION_CLICK, null);
                                    mc.assignButtonFromCode(BUTTON_LEFT);
                                    mc.execute(OPERATION_CLICK,null);
                                }
                                mIsAlreadyElapsed = false;
                            }
                            mPreviousEyesState = eyeArray.length;

                            /*
                             *  Eyes rectangles are used for debug purposes
                             *  Replace comments if has to debug
                             */
                            
                            nose=noseDetections.toArray();
                            for (int k = 0; k < nose.length; ++k) {
                                System.out.println("nose for loop");
                                mPt1.x = facesArray[i].x + nose[k].x;
                                mPt1.y = facesArray[i].y + nose[k].y;

                                mPt2.x = facesArray[i].x + nose[k].x + nose[k].width;
                                mPt2.y = facesArray[i].y + nose[k].y + nose[k].height;

                                int x,y;
                                x=(int) ((mPt1.x+mPt2.x)/2);
                                y=(int) ((mPt1.y+mPt2.y)/2);
                                System.out.println("center of nose: "+x+" "+y);
                                mc.move(x,y);
                                Core.rectangle(imgX, mPt1, mPt2, mClr1, 2);
                                
                            }
                            
                            for (int j = 0; j < eyeArray.length; ++j) {
                                mPt1.x = facesArray[i].x+ eyeArray[j].x;
                                mPt1.y = facesArray[i].y+ eyeArray[j].y;

                                mPt2.x =  facesArray[i].x+eyeArray[j].x + eyeArray[j].width;
                                mPt2.y =  facesArray[i].y+eyeArray[j].y + eyeArray[j].height;

                                
                                Core.rectangle(imgX, mPt1, mPt2, mClr1, 2);
                            }
                            
                            
                            
                        }
                        /**
                         * display the image in preview
                         * 
                         */
                        
                        setImage(imgX);
                    }
                    if (runnable == false) {
                        System.out.println("Going to wait()");
                    }
                }
            }
        }
    }

    /**
     * Creates new form VirtualMouse
     */
    public VirtualMouse() {
        initComponents();
        Dimension sd = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(sd.width / 2 - this.getWidth() / 2, sd.height / 2 - this.getHeight() / 2);
        loadConfiguaration();
    }

    /**
     * Load default configuration, training dataset, svm classifier
     */
    private void loadConfiguaration() {

        jButton3.setEnabled(true);
        jButton4.setEnabled(false);

        jSpinner1.setEnabled(false);
        jSpinner2.setEnabled(false);

       // jLabel4.setText("<html><body>Set blink timer for Single <br/> Click or Double Click and <br/> Right Click, All timings <br/>are in ms - miliseconds</body></html>");

        /**
         * Creates an uninitialized image icon
         */
        /*ImageIcon imageIcon = new ImageIcon(
                new ImageIcon(".//assests//Gesture-Icon.png")
                .getImage()
                .getScaledInstance(jLabel21.getWidth(), jLabel21.getHeight(), Image.SCALE_DEFAULT));
        jLabel21.setIcon(imageIcon);*/
        /**
         * Sets the image to be displayed as the icon for this window.
         */
        /*this.setIconImage(new ImageIcon(".//assests//Gesture-Icon.png")
                .getImage()
                .getScaledInstance(200, 200, Image.SCALE_DEFAULT));*/

        /*if (jCheckBox1.isSelected()) {
            jLabel15.setForeground(Color.green);
            jLabel15.setText("ON");
        } else {
            jLabel15.setForeground(Color.red);
            jLabel15.setText("OFF");
        }*/

        try {
            rb = new Robot();
            mc = new MouseCommand(rb);

        } catch (AWTException ex) {
            Logger.getLogger(VirtualMouse.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        jCheckBox1.setEnabled(false);
        jButton1.setEnabled(false);
        jButton3.setEnabled(false);
        jComboBox1.setEnabled(false);

        /**
         * listing connected camera
         */
        for (int device : loadCamera()) {
            jComboBox2.addItem(device);
        }

        mAbsoluteFaceSize = (int) (height * FACE_SIZE_PERCENTAGE);

        /**
         * Cascade classifier class for object detection.
         */
        FaceCascade = new CascadeClassifier(CASCADE_FILE_FACE);
        mFaces = new MatOfRect();
        System.out.println("\nRunning FaceDetector " + CASCADE_FILE_FACE);
        /**
         * Cascade classifier class for object detection.
         */
        EyeDetector = new CascadeClassifier(CASCADE_FILE_EYE);
        noseDetector = new CascadeClassifier(CASCADE_FILE_nose);
        eyeDetections = new MatOfRect();
        noseDetections = new MatOfRect();
        System.out.println("\nRunning EyeDetector " + CASCADE_FILE_EYE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel21 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSpinner1 = new javax.swing.JSpinner();
        jSpinner2 = new javax.swing.JSpinner();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(6);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel1.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel19.setFont(new java.awt.Font("Calibri", 0, 36)); // NOI18N
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("NATURAL EYE-COMPUTER INTERACTION");

        jButton3.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N
        jButton3.setText("Start Camera");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N
        jButton4.setText("Stop Camera");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jLabel20.setText("Select Camera & Resolution:");

        jComboBox1.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Select Resolution", "320 X 240", "640 X 480", "Fit to Screen" }));
        jComboBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox1ItemStateChanged(evt);
            }
        });
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jComboBox2.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Camera" }));
        jComboBox2.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox2ItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(42, 42, 42)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(jButton3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Preview", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Calibri", 0, 18))); // NOI18N
        jPanel3.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N

        jLabel11.setBackground(new java.awt.Color(255, 255, 255));
        jLabel11.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jLabel11.setOpaque(true);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Calibri", 0, 18))); // NOI18N

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel1.setText("Short Term Blink: ");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel7.setText("Long Term Blink: ");

        jButton1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButton1.setText("Set");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 0, 0));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 0, 0));

        jSpinner1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jSpinner2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSpinner2)
                    .addComponent(jSpinner1)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1))
                        .addGap(0, 12, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(jButton1)
                .addContainerGap())
        );

        jCheckBox1.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox1.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N
        jCheckBox1.setText("Send Key Stroke");
        jCheckBox1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jCheckBox1StateChanged(evt);
            }
        });

        jLabel15.setFont(new java.awt.Font("Calibri", 0, 24)); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel16.setFont(new java.awt.Font("Calibri", 0, 24)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel17.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("Mouse Operation:");
        jLabel17.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(28, 28, 28))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     *
     * @param evt
     */
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        if (jComboBox2.getSelectedItem().equals("Camera")) {
            JOptionPane.showMessageDialog(this, "Please select the camera !!!", "Choose Cameara", INFORMATION_MESSAGE);
        } else {
            if (jComboBox1.getSelectedItem().equals("Select Resolution")) {
                JOptionPane.showMessageDialog(this, "Please select the camera resolution !!!", "Choose Resolution", INFORMATION_MESSAGE);
            } else {
                if (!jComboBox1.getSelectedItem().equals("Fit to Screen")) {
                    String[] resolution = jComboBox1.getSelectedItem().toString().split("X");
                    vr.setWidth(resolution[0].trim());
                    vr.setHeight(resolution[1].trim());
                } else {
                    vr.setWidth("" + jLabel11.getWidth());
                    vr.setHeight("" + jLabel11.getHeight());
                }
                jButton3.setEnabled(false);
                jButton4.setEnabled(true);
                jButton1.setEnabled(true);
                jComboBox1.setEnabled(false);
                jComboBox2.setEnabled(false);

                /**
                 * Open video file or a capturing device for video capturing The
                 * methods first call "VideoCapture.release" to close the
                 * already opened file or camera.
                 */
                video.open(Integer.parseInt(jComboBox2.getSelectedItem().toString()));
                /**
                 * Sets a property in the VideoCapture.
                 */
                video.set(CV_CAP_PROP_FRAME_WIDTH, Double.valueOf("" + vr.getWidth()));
                video.set(CV_CAP_PROP_FRAME_HEIGHT, Double.valueOf("" + vr.getHeight()));

                width = Integer.parseInt(vr.getWidth());
                height = Integer.parseInt(vr.getHeight());

                jSpinner1.setEnabled(true);
                jSpinner2.setEnabled(true);

                /**
                 * Start the daemon thread
                 */
                myRGBThread = new DaemonRGBThread();
                rgbT = new Thread(myRGBThread);
                rgbT.setDaemon(true);
                myRGBThread.runnable = true;
                rgbT.start();
                resizeCount = 0;
            }
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     *
     * @param evt
     */
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        jButton4.setEnabled(false);
        jButton3.setEnabled(true);
        myRGBThread.runnable = false;
        rgbT.interrupt();
        video.release();

        jButton1.setEnabled(false);
        jCheckBox1.setEnabled(false);
        jCheckBox1.setSelected(false);
        jComboBox2.setEnabled(true);
        jComboBox1.setEnabled(false);
        jButton3.setEnabled(false);
        jComboBox2.setSelectedIndex(0);
        jComboBox1.setSelectedIndex(0);
        jLabel11.setIcon(null);
    }//GEN-LAST:event_jButton4ActionPerformed

    /**
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        int rtn = JOptionPane.showConfirmDialog(this, "Are you Sure want to close the window ?", "Close Window", YES_NO_OPTION);
        if (rtn == JOptionPane.YES_OPTION) {
            if (!(rgbT == null)) {
                myRGBThread.runnable = false;
                rgbT.interrupt();
                video.release();
            }
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);//yes
        } else {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);//cancel
        }
    }//GEN-LAST:event_formWindowClosing

    /**
     *
     * @param evt
     */
    private void jCheckBox1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jCheckBox1StateChanged
        try {
            /*if (jCheckBox1.isSelected()) {
                jLabel15.setForeground(Color.green);
                jLabel15.setText("ON");
                myRGBThread.runnableMouse = true;
            } else {
                jLabel15.setForeground(Color.red);
                jLabel15.setText("OFF");
                myRGBThread.runnableMouse = false;
            }*/
        } catch (Exception e) {
        }


    }//GEN-LAST:event_jCheckBox1StateChanged

    private void jComboBox2ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox2ItemStateChanged

        if (jComboBox2.getSelectedItem().equals("Camera")) {
            jComboBox1.setEnabled(false);
        } else {
            jComboBox1.setEnabled(true);
        }
    }//GEN-LAST:event_jComboBox2ItemStateChanged

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged

        if (jComboBox1.getSelectedItem().equals("Select Resolution")) {
            jButton3.setEnabled(false);
        } else {
            jButton3.setEnabled(true);
        }
    }//GEN-LAST:event_jComboBox1ItemStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        int sdClickTime = (int) jSpinner1.getValue();
        int rClickTime = (int) jSpinner2.getValue();

        if (sdClickTime < 1000 || rClickTime < 1000) {
            JOptionPane.showMessageDialog(this, "Timing gap should be greater than 999ms - milliseconds");
        } else {

            if (sdClickTime == rClickTime) {
                JOptionPane.showMessageDialog(this, "Both Timings should different!!");
            } else {
                myRGBThread.mBlinkSDCTime = (int) jSpinner1.getValue();
                myRGBThread.mBlinkRCTime = (int) jSpinner2.getValue();
                JOptionPane.showMessageDialog(this, "Timing are successfully set");
                jCheckBox1.setEnabled(true);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed

    /**
     * Converts mat To BufferedImage
     *
     * @param matrix
     * @return
     */
    public static BufferedImage matToBufferedImage(Mat matrix) {
        if (matrix.channels() == 1) {
            int cols = matrix.cols();
            int rows = matrix.rows();
            int elemSize = (int) matrix.elemSize();
            byte[] data = new byte[cols * rows * elemSize];
            int type;
            matrix.get(0, 0, data);
            switch (matrix.channels()) {
                case 1:
                    type = BufferedImage.TYPE_BYTE_GRAY;
                    break;
                case 3:
                    type = BufferedImage.TYPE_3BYTE_BGR;
                    // bgr to rgb
                    byte b;
                    for (int i = 0; i < data.length; i = i + 3) {
                        b = data[i];
                        data[i] = data[i + 2];
                        data[i + 2] = b;
                    }
                    break;
                default:
                    return null;
            }

            BufferedImage image2 = new BufferedImage(cols, rows, type);
            image2.getRaster().setDataElements(0, 0, cols, rows, data);
            return image2;
        }

        if (matrix.channels() == 3) {
            int widthm = matrix.width(), heightm = matrix.height(), channels = matrix.channels();
            byte[] sourcePixels = new byte[widthm * heightm * channels];
            matrix.get(0, 0, sourcePixels);
            // create new image and get reference to backing data
            BufferedImage image = new BufferedImage(widthm, heightm, BufferedImage.TYPE_3BYTE_BGR);
            final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
            return image;
        }
        return null;
    }

    /**
     * Set image to jlable11 ie preview image
     * @param mat
     */
    public void setImage(Mat mat) {
        if (mat == null) {
            img = null;
        } else {
            this.img = matToBufferedImage(mat);
            /*bufferedImage =  new BufferedImage(img.getWidth(null), 
                 img.getHeight(null), BufferedImage.TYPE_INT_RGB);
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-castle.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, 
                                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        bufferedImage = op.filter(bufferedImage, null);*/



            Image imageScaled = this.img.getScaledInstance(
                    width,
                    height, Image.SCALE_SMOOTH);
            ImageIcon iic = new ImageIcon(imageScaled);
            jLabel11.setIcon(iic);
        }
    }

    /**
     * Query to system to for get connected cameras
     *
     * @return
     */
    private int[] loadCamera() {
        ArrayList camDevice = new ArrayList();
        int count = 0;
        for (int device = 0; device < 10; device++) {
            VideoCapture cap = new VideoCapture(device);
            if (cap.isOpened()) {
                camDevice.add((count++), device);
            }
            cap.release();
        }

        int[] intArray = new int[camDevice.size()];
        for (int i = 0; i < intArray.length; i++) {
            intArray[i] = (int) camDevice.get(i);
        }

        return intArray;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VirtualMouse.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    System.out.println("Failed loading L&F: ");
                    System.out.println(ex);
                    System.out.println("Loading default Look & Feel Manager!");
                }
                new VirtualMouse().setVisible(true);
            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    // End of variables declaration//GEN-END:variables
}
