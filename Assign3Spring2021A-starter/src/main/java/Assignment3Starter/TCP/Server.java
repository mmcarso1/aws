//Using helper code from github.com/amehlhase316/ser321examples

package Assignment3Starter.TCP;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

import javax.imageio.ImageIO;

import org.json.*;

public class Server {
	
	OutputStream out;
	InputStream in;
	JSONObject json;
	String[][] images;
	
	public Server() {
		//load images into array
		images = new String[6][4];
		
		images[0][0] = "puppy";
		images[0][1] = "img/puppy/puppy1.png";
		images[0][2] = "img/puppy/puppy2.png";
		images[0][3] = "img/puppy/puppy3.png";
		
		images[1][0] = "cucumber";
		images[1][1] = "img/cucumber/cucumber1.png";
		images[1][2] = "img/cucumber/cucumber2.png";
		images[1][3] = "img/cucumber/cucumber3.png";
		

		images[2][0] = "cat";
		images[2][1] = "img/cat/cat1.png";
		images[2][2] = "img/cat/cat2.png";
		images[2][3] = "img/cat/cat3.png";
		
		images[3][0] = "car";
		images[3][1] = "img/car/car1.png";
		images[3][2] = "img/car/car2.png";
		images[3][3] = "img/car/car3.png";
		
		images[4][0] = "pug";
		images[4][1] = "img/pug/pug1.png";
		images[4][2] = "img/pug/pug2.png";
		images[4][3] = "img/pug/pug3.png";

		images[5][0] = "hat";
		images[5][1] = "img/hat/hat1.png";
		images[5][2] = "img/hat/hat2.png";
		images[5][3] = "img/hat/hat3.png";
		
	}
	
	public JSONObject getName(JSONObject request) {
		JSONObject player = new JSONObject();
		String name = request.getString("name");
		System.out.println("Player's name is " + name);
		player.put("name", name);
		return player;
	}
	
	public void send() {
		try {
			byte[] output = JsonUtils.toByteArray(json);
			NetworkUtils.Send(out, output);
			System.out.println("Sent!");
		 } catch (Exception e) {
			e.printStackTrace();
		 }
	 }
	
	public void receive() throws IOException {
		byte[] messageBytes = NetworkUtils.Receive(in);
		json = JsonUtils.fromByteArray(messageBytes);
	}
	
	public void error(int err) {
		json.put("error", err);
		System.out.println("Sending error " + err);
	}
	
	//from https://www.github.com/amhehlhase316/ser321examples
	public void imageToJson(String str) throws IOException {
		try {
			File file = new File(str);
			BufferedImage img = ImageIO.read(file);
			byte[] bytes = null;
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				ImageIO.write(img, "png", out);
				bytes = out.toByteArray();
			}
			if(bytes != null) {
				Base64.Encoder encoder = Base64.getEncoder();
			    json.put("image", encoder.encodeToString(bytes));
			    //send();
			    return;
			} else {
				System.out.println("Error loading image");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void playGame() throws IOException {
		int num = json.getInt("questions");
		int numAns = 0;
		int points = 0;
		String guess = "";
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0L;
		for(int i = 0; i < num; i++) {
			if(numAns == num) {
				json.put("message", "Game Over! Tallying up your points...");
				break;
			} else if (num > 6) {
				json.put("message", "I ran out of questions.");
				break;
			} else 
			json.put("answer", images[i][0]);
			System.out.println("The answer is " + images[i][0]);
			for(int j = 1; j < 4; j++) {
				//check timer
				elapsedTime = (new Date()).getTime() - startTime;
				System.out.println(elapsedTime);
				if (elapsedTime >= json.getInt("time")*1000) {
					json.put("message", "You ran out of time!");
					points = 0;
					break;
				}
				//add image to json and send
				imageToJson(images[i][j]);
				//json.put("image", images[i][j]);
				send();
				//handle guess
				receive();
				guess = json.getString("guess");
				System.out.println("Guess was " + guess);
				if(guess.compareTo(images[i][0]) == 0) {
					//guess was correct, add points
					switch (j) {
						case 1: points += 10;
							break;
						case 2: points += 5;
							break;
						case 3: points += 2;
							break;	
					}
					System.out.println(guess + " was correct!");
					json.put("message", "Correct! :D");
					numAns++;
					break;
				} else if(guess.compareTo("more") == 0) {
					if(j < 3) {
						System.out.println("sending more details");
						json.put("message", "Here you go with more details!");
					} else {
						System.out.println("full image was already sent");
						json.put("message", "I don't have more to show you.");
						j--;
					}
					continue;
				} else if(guess.compareTo("next") == 0) {
					System.out.println("showing next image");
					json.put("message", "What is this?");
					num++;
					break;
				} else {
					System.out.println("guess was incorrect");
					json.put("message", "WRONG!! Try again :)");
					j--;
				}
				
			}
		}
		System.out.println("GAME OVER");
		json.put("points", points);
		json.put("done", true);
		if(points > 0) {
			imageToJson("img/win.jpg");
		} else  {
			imageToJson("img/lose.jpg");
		}
		send();
	}

	public static void main(String[] args) throws IOException {
		
		Server main = new Server();
		
		//get args
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.out.println("Port number must be an integer.");
			System.exit(1);
		}
		
		System.out.println("Usage: gradle runServer port = " + port);
		
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
			while (true) {
				Socket sock = null;
				try {
					sock = server.accept();
					main.out = sock.getOutputStream();
					main.in = sock.getInputStream();
					
					while(true) {
						byte[] messageBytes = NetworkUtils.Receive(main.in);
						main.json = JsonUtils.fromByteArray(messageBytes);
						//get client name
						if (!main.json.has("name")) {
							main.error(1);
							main.send();
						} else if (!main.json.has("questions")){
							main.imageToJson("img/hi.png");
							main.error(2);
							main.send();
						} else if (!main.json.has("start")){
							main.imageToJson("img/questions.jpg");
							main.json.put("time", main.json.getInt("questions")*30);
							main.error(3);
							main.send();
						} else if (main.json.has("start")){
							main.playGame();
						} else {
							//main.error(0);
						}
					
						//System.out.println("Sent!");
					}
					
					
				} catch(Exception e) {
					System.out.println("Client disconnected");
					e.printStackTrace();
				} finally {
					if (sock != null) {
						sock.close();
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(server != null) {
				server.close();
			}
		}
	}
}
