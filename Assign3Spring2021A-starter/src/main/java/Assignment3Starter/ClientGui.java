package Assignment3Starter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.json.JSONException;
import org.json.JSONObject;

import Assignment3Starter.TCP.JsonUtils;
import Assignment3Starter.TCP.NetworkUtils;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with the current state
 *     -> modal means that it opens the GUI and suspends background processes. Processing 
 *        still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements Assignment3Starter.OutputPanel.EventHandlers {
  JDialog frame;
  PicturePanel picturePanel;
  OutputPanel outputPanel;
  OutputStream out;
  InputStream in;
  JSONObject json;

  /**
   * Construct dialog
   */
  public ClientGui() {
    frame = new JDialog();
    frame.setLayout(new GridBagLayout());
    frame.setMinimumSize(new Dimension(500, 500));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    // setup the top picture frame
    picturePanel = new PicturePanel();
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 0.25;
    frame.add(picturePanel, c);

    // setup the input, button, and output area
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.weighty = 0.75;
    c.weightx = 1;
    c.fill = GridBagConstraints.BOTH;
    outputPanel = new OutputPanel();
    outputPanel.addEventHandlers(this);
    frame.add(outputPanel, c);
    

  }

  /**
   * Shows the current state in the GUI
   * @param makeModal - true to make a modal window, false disables modal behavior
   */
  public void show(boolean makeModal) {
    frame.pack();
    //frame.setModal(makeModal);
    frame.setVisible(true);
  }

  /**
   * Creates a new game and set the size of the grid 
   * @param dimension - the size of the grid will be dimension x dimension
   */
  public void newGame(int dimension) {
    picturePanel.newGame(dimension);
    //outputPanel.appendOutput("Started new game with a " + dimension + "x" + dimension + " board.");
  }

  /**
   * Insert an image into the grid at position (col, row)
   * 
   * @param filename - filename relative to the root directory
   * @param row - the row to insert into
   * @param col - the column to insert into
   * @return true if successful, false if an invalid coordinate was provided
   * @throws IOException An error occured with your image file
   */
  public boolean insertImage(ImageIcon img, int row, int col) throws IOException {
    String error = "";
    try {
      // insert the image
      if (picturePanel.insertImage(img, row, col)) {
      // put status in output
        //System.out.println("Inserting " + json.getString("answer") + " in position (" + row + ", " + col + ")");
        return true;
      }
     // error = "File(\"" + filename + "\") not found.";
    } catch(PicturePanel.InvalidCoordinateException e) {
      // put error in output
      error = e.toString();
    }
    outputPanel.appendOutput(error);
    return false;
  }

  /**
   * Submit button handling
   * 
   * Change this to whatever you need
   */
  public void submitClicked() {
	  System.out.println("Submit");
    // Pulls the input box text
    String input = outputPanel.getInputText();
    // if has input
    if (input.length() > 0) {
      // append input to the output panel
      outputPanel.appendOutput(input);
      // clear input text box
      outputPanel.setInputText("");
    }
    try {
	    if(input.compareTo("more") == 0){
	    	sendGuess(input);
	    } else if(input.compareTo("start") == 0) {
	    	startGame();
	    } else if(input.compareTo("quit") == 0) {
	    	System.exit(0);
	    } else {
	    	//check for integer (num questions)
	    	for(int i = 0; i < input.length(); i++) {
	    		if(!Character.isDigit(input.charAt(i))) {
	    			if(json.has("name") && !json.has("start")) {
	    				outputPanel.appendOutput("You must enter an integer!");
	    				return;
	    			} else if (json.has("name") && json.has("start")){
		    	    	sendGuess(input);
		    	    	return;
	    			} else {
	    				sendName(input);
	    				return;
	    			}
	    		}
	    	}
	    	if(json.has("name") && !json.has("start")) {
	    		sendQuestions(input);
	    	} else {
	    		sendGuess(input);
	    	}
	    }
    } catch (IOException e) {
    	e.printStackTrace();
    }
  }
  
  /**
   * Key listener for the input text box
   * 
   * Change the behavior to whatever you need
   */
  @Override
  public void inputUpdated(String input) {
    if (input.equals("surprise")) {
      outputPanel.appendOutput("You found me!");
    }
    outputPanel.appendOutput(input);
  }
  
  public void send() throws IOException {
	  byte[] bytes = JsonUtils.toByteArray(json);
	  NetworkUtils.Send(out, bytes);
  }
  
  public void error() throws IOException {
	  int err = json.getInt("error");
	  switch (err) {
	  case 1:
		  System.out.println("Error 1 received");
		  outputPanel.appendOutput("Please enter your name to play a game!");
		  break;
	  case 2:
		  System.out.println("Error 2 received");
		  String name = (String)json.get("name");
		  bytesToImage();		  
		  outputPanel.appendOutput("Hello " + name + ", how many questions would you like me to ask?");
		  break;
	  case 3:
		  System.out.println("Error 3 received");
		  String name1 = (String)json.get("name");
		  int num = json.getInt("questions");
		  bytesToImage();
		  outputPanel.appendOutput("Thanks " + name1 + ", I will show you " + num + 
				  " different images to guess. You will have " + json.getInt("time") + " seconds." +
				  " Type 'start' to get the first question!");
		  break;
	  default:
		  System.out.println("default in switch");
		  break;
	  }
  }

  public void sendName(String str) throws IOException {
	  json.put("name", str);
	  send();
  }
  
  public void sendQuestions(String str) throws IOException {
	  int num = -1;
	  try {
		  num = Integer.parseInt(str);
	  } catch(NumberFormatException e) {
		  outputPanel.appendOutput("You must enter an interger!");
	  }
	  if(num > 6) {
		  outputPanel.appendOutput("Sorry, I only have 6 questions for you. Answer them all!");
		  num = 6;
	  }
	  json.put("questions", num);
	  send();
  }
  
  public void sendGuess(String str) throws IOException {
	  json.put("guess", str);
	  send();
  }
  
  public void startGame() throws IOException {
	  json.put("start", true);
	  json.put("error", -1);
	  send();
  }
  
  public void playGame() throws JSONException, IOException {
	  bytesToImage();
	  if(json.has("message")) {
		  outputPanel.appendOutput(json.getString("message"));
		  if(json.getString("message").compareTo("Correct! :D") == 0) {
			  outputPanel.appendOutput("What is this?");
		  }
	  } else {
		  outputPanel.appendOutput("What is this?");
	  }
  }
  
  public void endGame() throws IOException {
	  outputPanel.appendOutput(json.getString("message"));
	  if(json.getInt("points") > 0) {
	//	  insertImage("img/win.jpg", 0, 0);
		  bytesToImage();
		  outputPanel.appendOutput("You won with " + json.getInt("points") + " points!");
	  } else {
	//	  insertImage("img/lose.jpg", 0, 0);
		  bytesToImage();
		  outputPanel.appendOutput("Game over, you lost!");
	  }
	  json = new JSONObject();
	  send();
  }
 
 //from https://www.tutorialspoint.com/How-to-convert-Byte-Array-to-Image-in-java 
  public void bytesToImage() throws IOException {
	  Base64.Decoder decoder = Base64.getDecoder();
      byte[] bytes = decoder.decode(json.getString("image"));
      ImageIcon icon = null;
      try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
        BufferedImage image = ImageIO.read(bais);
        icon = new ImageIcon(image);
      }
      if (icon != null) {
    	  insertImage(icon, 0, 0);
      }
  }

  public static void main(String[] args) throws IOException {
    // create the frame
    ClientGui main = new ClientGui();
    main.show(true);
    
    // be sure to run in terminal at least once: 
    //     gradle Maker --args="img/Pineapple-Upside-down-cake.jpg 2"
    
    //get args
  	int port = 0;
  	String host = null;
 	try {
  		port = Integer.parseInt(args[0]);
  		host = args[1];
  	} catch (Exception e) {
 		System.out.println("Port number must be an integer. Host IP must"
  			+ " be valid.");
  	}
  		
  	System.out.println("Usage: runClient port = " + port + " and host = " + host);
  	
  	Socket sock;
 	try {
  		sock = new Socket(host, port);
  		main.out = sock.getOutputStream();
  		main.in = sock.getInputStream();
  		
  		if(sock != null) {
  			main.json = new JSONObject();
  			main.json.put("Connected", true);
  			byte[] helloBytes = JsonUtils.toByteArray(main.json);
  			NetworkUtils.Send(main.out, helloBytes);
  			System.out.println("Saying hi to server");
  			main.newGame(1);
  		}

  	    do {
  	    	byte[] responseBytes = NetworkUtils.Receive(main.in);
  	    	main.json = JsonUtils.fromByteArray(responseBytes);
  	    	if(main.json.getInt("error") > 0) {
  	    		main.error();
  	    	} else if(main.json.has("done")) {
  	    		System.out.println("GAME OVER");
  	    		main.endGame();
  	    	} else if(main.json.getBoolean("start")) {
  	    		main.playGame();
  	    	} else {
  	    		System.out.println("I'm not sure what to do");
  	    	}
  	    	main.show(false);
  	    } while (true);
 
 	} catch (IOException e) {
  		e.printStackTrace();
 	}
    
    
  }
}
