//
// WindowingSim.java
// Go-Back-N ARQ Protocol Simulation
// Last Updated: 8/27/17
//
// Descrip: Simulates Go-Back-N ARQ flow control via applet or Jar.
//

package main;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

//simple Frame class to specify Frame entity
class Frame {
    boolean isMoving;
    boolean reachedDest;
    boolean acknowledged;
    boolean needsAck;
    boolean isSelected;
    int vPos;
    
    //@descrip: default constructors to initialize members
    Frame() {
        isMoving = false;
        isSelected = false;
        reachedDest = false;
        acknowledged = false;
        needsAck = true;
        vPos = 0;
    }
    
    //@normal constructor to initialize members
    Frame(boolean _isMoving, int _vPos) {
        isMoving = _isMoving;
        isSelected = false;
        reachedDest = false;
        acknowledged = false;
        needsAck = true;
        vPos = _vPos;
    }
}

public class WindowingSim extends Applet implements ActionListener, Runnable {
    
    //clickable buttoms for the GUI
    Button send;
    Button pause;
    Button kill;
    Button reset;
    
    //threads to perform multiple actions
    Thread mainThread, timerThread;
    
    //to control frame rate of graphics
    boolean timerFlag;
    boolean timerSleep;
    
    //stops flickering by double buffering
    Dimension altDimension;
    Image altImage;
    Graphics altGraphics;
    
    //colors for different Frame types
    final Color color_regFrame = Color.gray;
    final Color color_roamRegFrame = Color.gray;
    final Color color_ackFrame = Color.green;
    final Color color_roamAckFrame = Color.green;
    final Color color_recFrame = Color.yellow;
    final Color color_selFrame = Color.red;
    
    //displays status message at end of each action
    String eventMsg;
    String eventLog[];
    
    //GUI properties regarding different elements
    int winLen = 5;
    int frameWidth = 10;
    int frameHeight = 30;
    int hStart = 100;
    int vStart = 50;
    int vPadding = 300;
    int totalFrames = 20;
    int timeOutSec = 20; //CHANGES TIMEOUT TIME HERE(keep above 18)
    int eventLogSize = 5;
    
    //important variables that control GUI parameters
    int winBase;
    int nextFrame;
    int frameRate;
    int selFrame = -1;
    
    //array of packets avaiable to be sent
    Frame frames[];
    
    
    //@param: none
    //@ret: none
    //@descrip: creates a main thread and starts it
    //NOTE: Overrides method from java.applet.Applet
    @Override
    public void start() {
        if (mainThread == null) {
            mainThread = new Thread(this);
        }
        mainThread.start();
    }
    
    //@param: none
    //@ret: none
    //@descrip: initializes parameters for painting
    @Override
    public void init() {
        
        winBase = 0; //initialize winBase
        nextFrame = 0; //initialize next seq. number of Frame to be sent
        frameRate = 5; //default value for frameRate
        
        //initialize the transmittable Frame array to set of all packets(total packets)
        frames = new Frame[totalFrames];
        //initialize the message indicating simulation is ready to be run
        eventMsg = "Click 'Send Frame' button to start.";
        //will hold on to multiple messages
        eventLog = new String[eventLogSize];
        
        //define the buttons and their action commands
        send = new Button("Send Frame");
        send.setActionCommand("sendF");
        send.addActionListener(this);
        
        pause = new Button("Pause Sim");
        pause.setActionCommand("pauseSim");
        pause.addActionListener(this);
        
        kill = new Button("Kill Frame");
        kill.setActionCommand("killFrame");
        kill.addActionListener(this);
        kill.setEnabled(false);
        
        reset = new Button("Reset");
        reset.setActionCommand("reset");
        reset.addActionListener(this);
        
        //Add the buttons
        add(send);
        add(pause);
        add(kill);
        add(reset);
    }
    
    //@param: array of packets
    //@ret: boolean(indicating if any Frame moving in the array of packets)
    //@descrip: Checks the array of packets to see if any Frame is moving.
    //			if moving returns true, else returns false
    public boolean areFramesMoving(Frame packets[]) {
        for (int i = 0; i < packets.length; i++) {
            if (packets[i] == null) {
                return false;
            } else if (packets[i].isMoving) {
                return true;
            }
        }
        return false;
    }
    
    //@param: int(indicating Frame index)
    //@ret: boolean (if packets reached receiver)
    //@descrip: checks all packets before @param Frame index to see if those packets
    //			have reached their destination
    public boolean checkRecFrames(int fIndex) {
        for (int i = 0; i < fIndex; i++) {
            if (!frames[i].reachedDest) {
                return false;
            }
        }
        return true;
    }
    
    //@param: none
    //@ret: none
    //@descrip: start running the animation
    //NOTE: overrides method from java.lang.Runnable
    @Override
    public void run() {
        //get current thread
        Thread curThread = Thread.currentThread();
        //while current thread is the main thread (meaning animation running)
        while (curThread == mainThread) {
            //check if any frames are moving
            if (areFramesMoving(frames)) {
                //while any frames are moving... iterate through each Frame
                for (int i = 0; i < totalFrames; i++) {
                    //if Frame exists AND is moving
                    if (frames[i] != null && frames[i].isMoving) {
                        //if a Frame hasn't reached it destination
                        //move Frame downwards(toward receiver)
                        if (frames[i].vPos < (vPadding - frameHeight)) {
                            //move Frame 5 pixels down
                            frames[i].vPos += 5;
                        }
                        //Frame is moving towards the receiver(downwards)
                        //and is within one frame height of being on top of destination
                        else if (frames[i].needsAck) {
                            //mark Frame as having reached its destination
                            frames[i].reachedDest = true;
                            //check if all preceeding frames have been received (0..i-1)
                            //if they have, then send acknowledgement for receiving current frame
                            if (checkRecFrames(i)) {
                                frames[i].vPos = frameHeight + 5;
                                frames[i].needsAck = false;
                                eventMsg = "Frame #" + i + " has been received. Acknowledgement sent.";
                            }
                            //one or more previous packets are missing
                            //current Frame will we received, but no acknowledgement will be sent
                            else {
                                frames[i].isMoving = false;
                                eventMsg = "Frame #" + i + " has been received. No acknowledge sent.";
                                //if the current Frame was selFrame; unselect it in this case
                                //since there is no acknowledgement being sent which can be selFrame
                                if (i == selFrame) {
                                    selFrame = -1;
                                    kill.setEnabled(false);
                                }
                            }
                        }
                        //Frame is not moving AND has reached the destination(sender)
                        //if this current Frame is an acknowledgement(needsAck indicates wheather ack is needed)
                        else if (!frames[i].needsAck) {
                            eventMsg = "Frame #" + i + " acknowledgement has been received.";
                            frames[i].isMoving = false;
                            //iterate through previous packets and label their acknowledgements received
                            for (int n = 0; n <= i; n++) {
                                frames[n].acknowledged = true;
                            }
                            //if the Frame was selected upon reaching the destination, then unselect it
                            if (i == selFrame) {
                                selFrame = -1;
                                kill.setEnabled(false);
                            }
                            //reset timeout timer when an acknowledgement is received
                            timerThread = null;
                            //move window base toward right
                            if (i + winLen < totalFrames) {
                                winBase = i + 1;
                            }
                            //increment nextFrame count upon received acknowledgement AND enable send button
                            if (nextFrame < winBase + winLen) {
                                send.setEnabled(true);
                            }
                            //when window base != nextFrame it means there is a succeeding
                            //Frame in the window that was sent, but whose acknowledgement
                            //has not yet been received. Thus, timeout timer needs to be reset
                            //for the succeding Frame's acknowledgement to be received within that
                            //time, else that Frame will need to be resent. Timer is reset when the
                            //current packets acknowledgement is received, the succeeding Frame is given extra time.
                            if (winBase != nextFrame) {
                                eventMsg += " Timeout timer has restarted.";
                                timerThread = new Thread(this);
                                timerSleep = true;
                                timerThread.start();
                            }
                            //No other frames are moving
                            else {
                                eventMsg += " Timeout timer stopped.";
                            }
                        }
                    }
                }
                repaint();
                //put animation to sleep for little bit
                try {
                    Thread.sleep(1000 / frameRate);
                } catch (InterruptedException e) {
                    System.out.println("ERROR: Unable to put mainThread to sleep!");
                }
            }
            //no frames are moving; i.e. animation is sitting idle
            else {
                mainThread = null;
            }
        }
        
        //if the current thread is the timer
        while (curThread == timerThread) {
            if (timerSleep) {
                timerSleep = false;
                try {
                    Thread.sleep(timeOutSec * 1000);
                } catch (InterruptedException e) {
                    System.out.println("ERROR: Unable to put timerThread to sleep!");
                }
            }
            //flag frames in the window to be resent if their acknowledgements have not been received
            else {
                for (int n = winBase; n < winBase + winLen; n++) {
                    if (frames[n] != null) {
                        if (!frames[n].acknowledged) {
                            frames[n].isMoving = true;
                            frames[n].needsAck = true;
                            frames[n].vPos = frameHeight + 5;
                        }
                    }
                }
                timerSleep = true;
                if (mainThread == null) {
                    mainThread = new Thread(this);
                    mainThread.start();
                }
                
                eventMsg = "Frames resent due to frame exceeding timeout timer.";
                eventMsg += " Timer has restarted.";
            }
        }
    }
    
    //@param: graphics
    //@ret: none
    //@descrip: call the update() to update animation
    //NOTE: overwritten paint() from java.awt.Component
    @Override
    public void paint(Graphics g) {
        update(g);
    }
    
    //@param: graphics
    //@ret: none
    //@descrip: updates the animation
    @Override
    public void update(Graphics g) {
        int altHStart;
        int altVStart;
        Dimension d = size();
        
        //Create the offscreen graphics context, if no good one exists.
        if ((altGraphics == null) || (d.width != altDimension.width) || (d.height != altDimension.height)) {
            altDimension = d;
            altImage = createImage(d.width, d.height);
            altGraphics = altImage.getGraphics();
        }
        
        //destroy preious image
        altGraphics.setColor(Color.white);
        altGraphics.fillRect(0, 0, d.width, d.height);
        
        //drawing window
        altGraphics.setColor(Color.black);
        //handles spacing for the 'windowing' box
        altGraphics.draw3DRect(hStart + winBase * (frameWidth + 7) - 4, vStart - 3, (winLen) * (frameWidth + 7) + 1, frameHeight + 6, true);
        
        //draw recangles indicating the 'Frame-rectangles'
        for (int i = 0; i < totalFrames; i++) {
            
            //if Frame is empty draw the ractangle with no color fill(default fill is white)
            if (frames[i] == null) {
                altGraphics.setColor(Color.black);
                //sender's Frame
                altGraphics.draw3DRect(hStart + (frameWidth + 7) * i, vStart, frameWidth, frameHeight, true);
                //receiver's Frame
                altGraphics.draw3DRect(hStart + (frameWidth + 7) * i, vStart + vPadding, frameWidth, frameHeight, true);
            }
            //else the Frame is not empty; i.e. it has data
            else {
                //pick color depending on if frames has received the cooresponding acknowledgement Frame
                if (frames[i].acknowledged) {
                    altGraphics.setColor(color_ackFrame);
                }
                //sender has NOT received acknowledgement Frame. i.e. regular data Frame
                else {
                    altGraphics.setColor(color_regFrame);
                }
                //draw the senders's Frame with specifed color above
                altGraphics.fill3DRect(hStart + (frameWidth + 7) * i, vStart, frameWidth, frameHeight, true);
                
                //draw the receiver's Frame with specified color
                altGraphics.setColor(color_recFrame);
                //ONLY fill Frame-rectangle with color if it has reached its destination
                if (frames[i].reachedDest) {
                    altGraphics.fill3DRect(hStart + (frameWidth + 7) * i, vStart + vPadding, frameWidth, frameHeight, true);
                }
                //if Frame hasn't reached destination then dont fill Frame-rectangle with any color
                else {
                    altGraphics.draw3DRect(hStart + (frameWidth + 7) * i, vStart + vPadding, frameWidth, frameHeight, true);
                }
                
                //moving frames
                if (frames[i].isMoving) {
                    //set color scheme for moving-selFrame Frame
                    //NOTE: This gives select-color priority over reg-color frames
                    if (i == selFrame) {
                        altGraphics.setColor(color_selFrame);
                    }
                    //regular Frame
                    else if (frames[i].needsAck) {
                        altGraphics.setColor(color_roamRegFrame);
                    }
                    //acknowledgement moving Frame
                    else {
                        altGraphics.setColor(color_roamAckFrame);
                    }
                    //fill the frame-rectangle with specified color above
                    if (frames[i].needsAck) {
                        altGraphics.fill3DRect(hStart + (frameWidth + 7) * i, vStart + frames[i].vPos, frameWidth, frameHeight, true);
                    } else {
                        altGraphics.fill3DRect(hStart + (frameWidth + 7) * i, vStart + vPadding - frames[i].vPos, frameWidth, frameHeight, true);
                    }
                }
            }
        }
        
        // alternative coordinates for optional boxes below main simulation
        altHStart = hStart;
        altVStart = vStart + vPadding + frameHeight;
        
        //draw the message boxes
        altGraphics.setColor(Color.black);
        //displays status string below the receiver's Frame ractangles
        altGraphics.drawString("EVENT LOG:", altHStart, altVStart + 25);
        altGraphics.draw3DRect(altHStart, altVStart + 28, 510, 100, true);
        //altGraphics.drawString(eventMsg, altHStart+4, altVStart + 45);
        //update the log history
        for (int i = eventLog.length - 2; 0 <= i; i--) {
            //if there is a NEW message then shift the log downwards
            if (eventLog[i] != null && !eventLog[0].equals(eventMsg)) {
                eventLog[i + 1] = eventLog[i];
            }
        }
        //if there is no new msg this will keep inserting old msg into
        //same position; effcient to do this than to do a check for new message
        eventLog[0] = eventMsg;
        //display all the messages in the log on each repaint()
        for (int i = 0; i < eventLog.length; i++) {
            //after each individual msg printing; print succeeding msgs with equal spacing
            if (eventLog[i] != null)
                altGraphics.drawString(eventLog[i], altHStart + 4, (altVStart + 45) + i * 20);
        }
        
        //string displaying the base of the window and next sequence number of Frame to be sent
        altGraphics.setColor(Color.black);
        altGraphics.drawString("Sliding Window Info.", hStart + (frameWidth + 7) * totalFrames + 30, vStart + frameHeight - 5);
        altGraphics.drawString("Window Size = 5", hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 15);
        altGraphics.drawString("Base Frame = " + winBase, hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 30);
        altGraphics.drawString("Next Frame = " + nextFrame, hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 45);
        altGraphics.drawString("Timeout = " + timeOutSec + " secs", hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 60);
        //draws a box around the 'Window base' and 'next seq' text
        altGraphics.setColor(Color.black);
        altGraphics.draw3DRect(hStart + (frameWidth + 7) * totalFrames + 30, vStart + frameHeight, 140, 65, true);
        
        //Legend captions
        altGraphics.drawString("Legend", hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 100);
        altGraphics.drawString("Frame", hStart + (frameWidth + 7) * totalFrames + 50, vStart + frameHeight + 120);
        altGraphics.drawString("Acknowledgement", hStart + (frameWidth + 7) * totalFrames + 50, vStart + frameHeight + 135);
        altGraphics.drawString("Received Frame", hStart + (frameWidth + 7) * totalFrames + 50, vStart + frameHeight + 150);
        altGraphics.drawString("Selected Frame", hStart + (frameWidth + 7) * totalFrames + 50, vStart + frameHeight + 165);
        //draws a box around the 'Legend'
        altGraphics.setColor(Color.gray);
        altGraphics.draw3DRect(hStart + (frameWidth + 7) * totalFrames + 30, vStart + frameHeight + 105, 140, 100, true);
        //draw color-coded boxes in the 'Legend'
        //REG FRAME
        altGraphics.setColor(color_roamRegFrame);
        altGraphics.fill3DRect(hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 110, 10, 10, true);
        //ACK FRAME
        altGraphics.setColor(color_roamAckFrame);
        altGraphics.fill3DRect(hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 125, 10, 10, true);
        //RECEIVED FRAME
        altGraphics.setColor(color_recFrame);
        altGraphics.fill3DRect(hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 140, 10, 10, true);
        //SELECTED FRAME
        altGraphics.setColor(color_selFrame);
        altGraphics.fill3DRect(hStart + (frameWidth + 7) * totalFrames + 35, vStart + frameHeight + 155, 10, 10, true);
        
        //string displaying 'frames' and 'receiver' labels for frames
        altGraphics.setColor(Color.red);
        altGraphics.drawString("Sender", hStart - 50, vStart + 18);
        altGraphics.drawString("Receiver", hStart - 55, vStart + vPadding + 18);
        
        //draw the image
        g.drawImage(altImage, 0, 0, this);
    }
    
    //@param: event, int, int
    //@ret: boolean
    //@check if the mouse is clicked
    @Override
    public boolean mouseDown(Event e, int x, int y) {
        int i, xPos, yPos;
        i = (x - hStart) / (frameWidth + 7);
        if (frames[i] != null) {
            xPos = hStart + (frameWidth + 7) * i;
            yPos = frames[i].vPos;
            //check if the mouse click occuured near a frame by creating a reference box
            if (x >= xPos && x <= xPos + frameWidth && frames[i].isMoving) {
                if ((frames[i].needsAck && y >= vStart + yPos &&
                        y <= vStart + yPos + frameHeight) || ((!frames[i].needsAck) &&
                        y >= vStart + vPadding - yPos && y <= vStart + vPadding - yPos + frameHeight)) {
                    eventMsg = "Frame #" + i + " has been selected.";
                    frames[i].isSelected = true;
                    selFrame = i;
                    kill.setEnabled(true);
                }
            }
        }
        return true;
    }
    
    //@param: event(obj receives all events)
    //@ret: none
    //@descrip: check what type of action to perform
    @Override
    public void actionPerformed(ActionEvent e) {
        String actCmd = e.getActionCommand();
        //if send button is pressed AND next frame is within the window
        if ("sendF".equals(actCmd) && nextFrame < winBase + winLen) {
            //label the Frame as 'moving' and change its position down by 5 pixels
            frames[nextFrame] = new Frame(true, frameHeight + 5);
            //generate sring indicating the action performed
            eventMsg = "Frame #" + nextFrame + " has been sent.";
            //start timeout timer for that Frame if it's the first Frame in
            //the window being sent
            if (winBase == nextFrame) {
                eventMsg += " Timer set for Frame #" + winBase + ".";
                if (timerThread == null) {
                    timerThread = new Thread(this);
                }
                timerSleep = true;
                timerThread.start();
            }
            //update the animation
            repaint();
            //update next sequence to be sent
            nextFrame++;
            //if all packets in the window have been sent, then disable send button
            if (nextFrame == winBase + winLen) {
                send.setEnabled(false);
            }
            //run main thread
            start();
        }
        //pause button was pressed
        else if ("pauseSim".equals(actCmd)) {
            mainThread = null;
            if (timerThread != null) {
                timerFlag = true;
                timerThread = null;
            }
            //update the pause button to show 'resume' text and action
            pause.setLabel("Resume");
            pause.setActionCommand("resumeSim");
            
            //when animation is paused disable all buttons except 'reset' button
            send.setEnabled(false);
            kill.setEnabled(false);
            
            eventMsg = "Simulation has been paused.";
            //only insert timer warning when there are moving frames(i.e. nextFrame != winBase)
            if (nextFrame != winBase) {
                eventMsg += " Timeout timer has been paused.";
            }
            //update the animation
            repaint();
        }
        //pause/resume button is pressed
        else if ("resumeSim".equals(actCmd)) {
            eventMsg = "Simulation has been resumed.";
            //update the pause/resume button text
            pause.setLabel("Pause Sim");
            pause.setActionCommand("pauseSim");
            //only insert timer warning when there are moving frames
            if (timerFlag) {
                if (nextFrame != winBase) {
                    eventMsg += " Timeout timer has resumed running.";
                }
                timerThread = new Thread(this);
                timerSleep = true;
                timerThread.start();
            }
            //enable all disabled buttons
            //only enable send on resume when window not full
            if (nextFrame != winBase + winLen) {
                send.setEnabled(true);
            }
            //only enable kill if frame is selected
            if (selFrame != -1) {
                kill.setEnabled(true);
            }
            //update animation
            repaint();
            //start the main thread
            start();
        }
        //kill button was pressed
        else if ("killFrame".equals(actCmd)) {
            //check if the selFrame Frame needed acknowledgement
            if (frames[selFrame].needsAck) {
                eventMsg = "Frame #" + selFrame + " has been destroyed.";
                eventMsg += " Timeout timer still running for Frame #" + selFrame + ".";
            }
            //if no acknowledgement was needed then its a acknowledgement Frame
            else {
                eventMsg = "Acknowledgement of Frame #" + selFrame + " has been destroyed.";
                eventMsg += "Timeout timer still running for Frame " + selFrame + ".";
            }
            //mark the destroyed Frame as immobile
            frames[selFrame].isMoving = false;
            kill.setEnabled(false);
            selFrame = -1;
            repaint();
        }
        //reset button was pressed
        else if ("reset".equals(actCmd)) {
            //empty frames array
            for (int i = 0; i < totalFrames; i++) {
                if (frames[i] != null) {
                    frames[i] = null;
                }
            }
            winBase = 0;
            nextFrame = 0;
            selFrame = -1;
            frameRate = 5;
            timerFlag = false;
            timerSleep = false;
            mainThread = null;
            timerThread = null;
            //return buttons to initial state
            send.setEnabled(true);
            kill.setEnabled(false);
            pause.setLabel("Pause Sim");
            pause.setActionCommand("pauseSim");
            
            eventMsg = "Simulation has been restarted.";
            repaint();
        }
    }
    
    public static void main(String[] args) {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        
        java.applet.Applet applet = new WindowingSim();
        frame.add(applet);
        applet.init();
        applet.start();
        
        frame.setTitle("Go-Back-N ARQ Windowing Simulation");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(650, 550);
        //frame.pack();
        frame.setVisible(true);
    }    
}