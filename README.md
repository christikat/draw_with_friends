# 🎨 Draw With Friends

Draw With Friends is a collaborative, turn-based, real-time drawing application built with Java Swing using a socket-based client-server architecture.
Players connect, select their username, and take turns drawing on a shared canvas using the application's built in tools. The server allows up to four players to collaborate at a time.

## 🚀 Features
- **Drawing Tools**: Pencil, Line, Rectangle, Circle, Text
- **Colours**: Default colour palette for quick access, and colour picker for custom colours
- **Thickness Selector**: Used for width of drawing tools, and setting font size of text tool
- **Fill**: Used in combination with the rectangle and circle tool. Creates filled or outlined shapes.
- **Clear**: Resets to a blank canvas
- **Light/Dark Mode Toggle**: Switch between light and dark themes
- **Drawing Logs**: Time stamped game logs - notifies turns, player connections/disconnections, clears, chat
- **Player List/Drawing Logs Toggle**: Hide/Show the right panel
- **Save**: Save the canvas as a png
- **Open**: Open and image to add to the canvas (adds to top left corner)
- **Chat**: Sends messages to everyone connected to the server

## 📷 Screenshots
<div align="center">
<img src="https://i.imgur.com/JIvcfLO.png" alt="screenshot-1">
<img src="https://i.imgur.com/awbozWI.png" alt="screenshot-1">
<img src="https://i.imgur.com/sETTYWw.png" alt="screenshot-1">
</div>

## 🎥 Video Demo
https://github.com/user-attachments/assets/65aa0255-83e3-42c6-bf10-08619a608488

## 📁 Project File Structure
```text
us.otechu
├── client                            # Client-side logic and UI
│   ├── DrawWithFriends.java          # Main client launcher
│   ├── ClientConnection.java         # Socket communication logic
│   ├── DrawData.java                 # Data model for drawing actions
│   └── ui                            # All GUI-related components
│       ├── ColourPanel.java          # Color palette panel
│       ├── DrawingAppFrame.java      # Main application window
│       ├── DrawingPanel.java         # Canvas panel where drawing happens
│       ├── DrawTools.java            # Interface and tools for drawing
│       │   ├── Circle                # Tool: draw ellipses
│       │   ├── DrawTools (interface) # Tool interface
│       │   ├── Line                  # Tool: draw straight lines
│       │   ├── Pencil                # Tool: freehand drawing
│       │   ├── Rectangle             # Tool: draw rectangles
│       │   └── TextTool              # Tool: draw text
│       └── PlayerListRenderer.java   # Custom list cell renderer for player display
├── common
│   └── Utils.java                    # Utility methods shared between client/server
├── server                            # Server-side logic
│   ├── ClientHandler.java            # Manages one client's session on the server
│   └── DrawingServer.java            # Core server logic and state management

```
## How To Run
### Requirements
- Java 17+

### Steps
1. Download the server.jar and client.jar files from the latest release
2. Open up two terminals set to folder containing the `.jar` files
3. Run the server file in the terminal with the command:
    ```bash
    java -jar server.jar
    ```
   - The server will be listening on port 5000, waiting for clients to connect
4. In the second terminal, run the client file with:
    ```bash
    java -jar client.jar
    ```
   - The client will prompt you for a username
   - Wait for your turn
   - Start drawing!
   - Pass the turn to the next player, by clicking "End Turn"



## 📦 Dependencies
<div align="center">
 <table>
   <thead>
     <tr>
       <th>Library</th>
       <th>Version</th>
       <th>Description</th>
       <th>Link</th>
     </tr>
   </thead>
   <tbody>
     <tr>
       <td><strong>FlatLaf</strong></td>
       <td>3.2</td>
       <td>Look & Feel for Swing (light/dark themes)</td>
       <td><a href="https://github.com/JFormDesigner/FlatLaf">FlatLaf on GitHub</a></td>
     </tr>
     <tr>
       <td><strong>GSON</strong></td>
       <td>2.10.1</td>
       <td>JSON serialization/deserialization</td>
       <td><a href="https://github.com/google/gson">GSON on GitHub</a></td>
     </tr>
   </tbody>
 </table>
</div>

## 💻 Collaborators
- Christine Tran
- Kishawn Wynter

## 📖 References
1. Icons: https://fonts.google.com/icons
2. Consumer Interface: https://www.geeksforgeeks.org/java-8-consumer-interface-in-java-with-examples/
3. Convert `Color` to hexadecimal string: https://stackoverflow.com/questions/3607858/convert-a-rgb-color-value-to-a-hexadecimal-string
4. gson: https://google.github.io/gson/
5. Supplier Interface: https://www.geeksforgeeks.org/supplier-interface-in-java-with-examples/