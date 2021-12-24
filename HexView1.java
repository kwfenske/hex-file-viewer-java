/*
  Hexadecimal File Viewer #1
  Written by: Keith Fenske, http://kwfenske.github.io/
  Wednesday, 7 May 2014
  Java class name: HexView1
  Copyright (c) 2014 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 graphical (GUI) application to display the contents of a
  file in hexadecimal and as plain text (7-bit ASCII).  Files may be very
  large.  A window is shown with a number of rows (lines) and columns (bytes
  per row).  You may move this window with buttons on top to scroll through the
  file, go to locations (offsets) within the file, or copy text from the
  display.  The mouse scroll wheel can be combined with the Shift key to move
  one page at a time.  Keyboard shortcuts for navigation buttons combine the
  Alt key with another key:

      |<    start of file           Alt+Home
      <<    back one page           Alt+PageUp
      <     back one line/row       Alt+Up (up arrow)
      >     forward one line/row    Alt+Down (down arrow)
      >>    forward one page        Alt+PageDown
      >|    end of file             Alt+End

  Editing and searching are not supported.  Large files in gigabytes or
  terabytes often exceed the capacity of hex editors, because indexing is done
  with 32-bit integers.  Do not use this program on sequential media (CD, DVD,
  tape, etc), only on regular disk drives that provide true random access.  The
  hex display looks best with the "Lucida Console" font installed.

  Apache License or GNU General Public License
  --------------------------------------------
  HexView1 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options for the position and size of the
  application window, the size of the display font, and a file name to be
  opened.  See the "-?" option for a help summary:

      java  HexView1  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.

  Restrictions and Limitations
  ----------------------------
  Many things break in the Java language and run-time environment when objects
  have more than two billion separately indexable items (the largest signed
  32-bit integer or 2,147,483,647).  In this program, the critical factor is
  the total number of rows in the hexadecimal display.  Hence, there are
  buttons for precise scrolling in addition to a vertical scroll bar.  Beyond
  two billion rows, scroll bars alone would not have enough resolution to
  select individual rows (up one row, down one row, etc), and beyond about 100
  billion rows, entire pages would be unavailable.

  Suggestions for New Features
  ----------------------------
  (1) Save the options, window position and size, and most recently used file
      or folder in a preferences file.  PE, 2014-05-12.
  (2) Complaints will be ignored if they assume this program should act more
      like a word processor or text editor.  Design choices are the result of
      supporting large files, and that will not change.  KF, 2014-05-26.
*/

import java.awt.*;                // older Java GUI support
import java.awt.datatransfer.*;   // older Java GUI data transfer
import java.awt.dnd.*;            // older Java GUI drag-and-drop
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class HexView1
{
  /* constants */

  static final String ACTION_FILE_END = "HexViewFileEnd";
                                  // action strings for keyboard mappings
  static final String ACTION_FILE_START = "HexViewFileStart";
  static final String ACTION_LINE_DOWN = "HexViewLineDown";
  static final String ACTION_LINE_UP = "HexViewLineUp";
  static final String ACTION_PAGE_DOWN = "HexViewPageDown";
  static final String ACTION_PAGE_UP = "HexViewPageUp";
  static final int COLUMN_DEFAULT = 16; // default number of columns
  static final String[] COLUMN_LIST = {"4", "8", "12", "16", "24", "32", "40"};
                                  // number of columns (display bytes per row)
  static final int COLUMN_MAX = 99; // maximum number of columns
  static final int COLUMN_MIN = 1; // minimum number of columns
  static final String COLUMN_TEXT = "Number of columns must be from 1 to 99";
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2014 by Keith Fenske.  Apache License or GNU GPL.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
//static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String FONT_DISPLAY = "Monospaced"; // hex display text
                                  // this font is always available in Java
  static final String FONT_SYSTEM = "Dialog"; // buttons, dialogs, etc.
                                  // this font is always available in Java
  static final String HEX_TEXT =
    "File offset must be hexadecimal with no spaces or punctuation";
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Hexadecimal File Viewer - by: Keith Fenske";
  static final int ROW_DEFAULT = 24; // default number of rows
  static final String[] ROW_LIST = {"8", "16", "24", "32", "48", "64", "80"};
                                  // number of rows (lines) in hex display
  static final int ROW_MAX = 99;  // maximum number of rows
  static final int ROW_MIN = 2;   // minimum number of rows
  static final String ROW_TEXT = "Number of rows must be from 2 to 99";
  static final int SCROLL_BLOCK = 10; // big scroll bar, block or page size
  static final int SCROLL_KEEP = 1; // zero or more, and less than <ROW_MIN>
  static final int SCROLL_MAX = 100; // big scroll bar, maximum resolution
  static final int SCROLL_ROWS = 0x7FFF0000; // safe positive 32-bit integer
  static final int SIZE_DEFAULT = 16; // default font point size
  static final String[] SIZE_LIST = {"12", "14", "16", "18", "24", "30", "36"};
                                  // font point sizes for output text area
  static final int SIZE_MAX = 99; // maximum font point size
  static final int SIZE_MIN = 10; // minimum font point size
  static final String SIZE_TEXT = "Font point size must be from 10 to 99";

  /* Data is read from the file in relatively large buffers, which must be at
  least four times bigger than <COLUMN_MAX> times <ROW_MAX>.  Powers of two are
  recommended for almost all computer systems. */

  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)

  /* Java uses signed 64-bit integers for file offsets and file sizes in bytes.
  We do the same, but have some calculations (mostly addition) that assume the
  result won't overflow.  We limit the maximum file size by an amount bigger
  than <BUFFER_SIZE>.  The number below is so large, nobody will notice that we
  stop four gigabytes (10**9) short of the last exabyte (10**18).  Disk drives
  of two terabytes (10**12) were common when this program was written.  Server
  arrays were in the petabyte range (10**15).  Very few users or applications
  have individual files of this size. */

  static final long MAX_FILE_SIZE = 0x7FFFFFFF00000000L;
                                  // safe positive 64-bit integer

  /* class variables: alphabetical by variable name */

  static long bufferStart;        // byte offset in file for file buffer
  static int bufferUsed;          // number of data bytes in file buffer
  static int columnCount, rowCount; // number of columns, rows in output
  static RandomAccessFile currentFile; // pointer for our open file
  static long displayStart;       // byte offset in file for text display
  static byte[] fileBuffer;       // small window of data bytes from file
  static long fileSize;           // total size of open file in bytes
  static String fontName;         // font name for output text area
  static int fontSize;            // point size for output text area
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static boolean scrollSmall;     // true if using small model for scroll bar

  /* GUI elements: alphabetical by class name, then by variable name */

  static ActionListener actionListener; // our shared action listener
  static JButton exitButton, fileEndButton, fileStartButton, lineDownButton,
    lineUpButton, openButton, pageDownButton, pageUpButton;
  static JComboBox columnSizeDialog, fontSizeDialog, rowSizeDialog;
  static JFileChooser fileChooser; // asks for input and output file names
  static JFrame mainFrame;        // this application's window if GUI
  static JScrollBar scrollBar;    // vertical scroll bar
  static JTextArea outputText;    // where hexadecimal file data goes
  static JTextField gotoText;     // jump to location (offset) in file

/*
  main() method

  We run as a graphical application only.  Set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    Font buttonFont;              // font for buttons, labels, status, etc
    String fileName;              // one file name from command line
    long gotoStart;               // starting offset for file on command line
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    bufferStart = 0;              // byte offset in file for file buffer
    bufferUsed = 0;               // number of data bytes in file buffer
    buttonFont = null;            // by default, don't use customized font
    columnCount = COLUMN_DEFAULT; // default number of columns in hex display
    currentFile = null;           // no file is currently open for reading
    displayStart = 0;             // byte offset in file for text display
    fileBuffer = new byte[BUFFER_SIZE]; // allocate space for data bytes
    fileName = null;              // no file name from command line yet
    fileSize = 0;                 // no file open, so no total bytes yet
    fontName = "Lucida Console";  // preferred font name for output text area
    fontSize = SIZE_DEFAULT;      // default point size for output text area
    gotoStart = 0;                // starting offset for file on command line
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    rowCount = ROW_DEFAULT;       // default number of rows in hex display
    scrollSmall = true;           // prefer small model for scroll bar
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.startsWith("-c") || (mswinFlag && word.startsWith("/c")))
      {
        /* This option is followed by the number of columns (bytes per row) in
        the hex display (output text area). */

        int size = -1;            // default value for number of columns
        try { size = Integer.parseInt(word.substring(2)); } // try parsing
        catch (NumberFormatException nfe) { size = -1; } // mark as error
        if ((size < COLUMN_MIN) || (size > COLUMN_MAX))
        {
          System.err.println(COLUMN_TEXT + ": " + args[i]);
                                  // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        columnCount = size;       // use this number of columns in hex display
      }

      else if (word.startsWith("-f") || (mswinFlag && word.startsWith("/f")))
        fontName = args[i].substring(2); // accept anything for font name

      else if (word.startsWith("-g") || (mswinFlag && word.startsWith("/g")))
      {
        /* This option is followed by a starting file offset in hexadecimal,
        which only applies to a file named on the command line. */

        long offset = -1;         // default value for file offset
        try { offset = Long.parseLong(word.substring(2), 16); } // try parsing
        catch (NumberFormatException nfe) { offset = -1; } // mark as error
        if (offset < 0)           // recognize most errors, ignore maximum
        {
          System.err.println(HEX_TEXT + ": " + args[i]);
                                  // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        gotoStart = offset;       // displayText() will limit the range
      }

      else if (word.startsWith("-r") || (mswinFlag && word.startsWith("/r")))
      {
        /* This option is followed by the number of rows (lines) in the hex
        display (output text area). */

        int size = -1;            // default value for number of rows
        try { size = Integer.parseInt(word.substring(2)); } // try parsing
        catch (NumberFormatException nfe) { size = -1; } // mark as error
        if ((size < ROW_MIN) || (size > ROW_MAX))
        {
          System.err.println(ROW_TEXT + ": " + args[i]);
                                  // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        rowCount = size;          // use this number of rows in hex display
      }

      else if (word.startsWith("-s") || (mswinFlag && word.startsWith("/s")))
      {
        /* This option is followed by a font point size for the output text
        area. */

        int size = -1;            // default value for font point size
        try { size = Integer.parseInt(word.substring(2)); } // try parsing
        catch (NumberFormatException nfe) { size = -1; } // mark as error
        if ((size < SIZE_MIN) || (size > SIZE_MAX))
        {
          System.err.println(SIZE_TEXT + ": " + args[i]);
                                  // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        fontSize = size;          // use this point size for output text area
      }

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size for buttons, dialogs,
        labels, etc. */

        int size = -1;            // default value for font point size
        try { size = Integer.parseInt(word.substring(2)); } // try parsing
        catch (NumberFormatException nfe) { size = -1; } // mark as error
        if ((size < SIZE_MIN) || (size > SIZE_MAX))
        {
          System.err.println(SIZE_TEXT + ": " + args[i]);
                                  // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonFont = new Font(FONT_SYSTEM, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(FONT_SYSTEM, Font.BOLD, size); // for small sizes
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume this is the name of
        a file to be opened later. */

        if (fileName == null)     // is this the first non-option parameter?
          fileName = args[i];     // yes, accept anything for a file name
        else                      // no, only one file name allowed
        {
          System.err.println("Only one file name allowed: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }
    }

    /* Open the graphical user interface (GUI).  The standard Java style is the
    most reliable, but you can switch to something closer to the local system,
    if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    actionListener = new HexView1User(null); // our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = FONT_DISPLAY;    // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel1, panel2, etc). */

    /* Navigation buttons (mostly centered on top).  Java 1.4 and 5.0 include
    keyboard shortcuts in the tool tip text; Java 6 and later don't. */

    JPanel panel1 = new JPanel(new BorderLayout(10, 0));

    openButton = new JButton("Open...");
    openButton.addActionListener(actionListener);
    if (buttonFont != null) openButton.setFont(buttonFont);
    openButton.setMnemonic(KeyEvent.VK_O);
    openButton.setToolTipText("Open file for reading.");
    panel1.add(openButton, BorderLayout.WEST);

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

    fileStartButton = new JButton("|<");
    fileStartButton.addActionListener(actionListener);
    if (buttonFont != null) fileStartButton.setFont(buttonFont);
//  fileStartButton.setMnemonic(KeyEvent.VK_HOME);
    fileStartButton.setToolTipText("Start of file.");
    panel2.add(fileStartButton);

    pageUpButton = new JButton("<<");
    pageUpButton.addActionListener(actionListener);
    if (buttonFont != null) pageUpButton.setFont(buttonFont);
//  pageUpButton.setMnemonic(KeyEvent.VK_PAGE_UP);
    pageUpButton.setToolTipText("Back one page.");
    panel2.add(pageUpButton);

    lineUpButton = new JButton("<");
    lineUpButton.addActionListener(actionListener);
    if (buttonFont != null) lineUpButton.setFont(buttonFont);
//  lineUpButton.setMnemonic(KeyEvent.VK_UP);
    lineUpButton.setToolTipText("Back one line or row.");
    panel2.add(lineUpButton);

    lineDownButton = new JButton(">");
    lineDownButton.addActionListener(actionListener);
    if (buttonFont != null) lineDownButton.setFont(buttonFont);
//  lineDownButton.setMnemonic(KeyEvent.VK_DOWN);
    lineDownButton.setToolTipText("Forward one line or row.");
    panel2.add(lineDownButton);

    pageDownButton = new JButton(">>");
    pageDownButton.addActionListener(actionListener);
    if (buttonFont != null) pageDownButton.setFont(buttonFont);
//  pageDownButton.setMnemonic(KeyEvent.VK_PAGE_DOWN);
    pageDownButton.setToolTipText("Forward one page.");
    panel2.add(pageDownButton);

    fileEndButton = new JButton(">|");
    fileEndButton.addActionListener(actionListener);
    if (buttonFont != null) fileEndButton.setFont(buttonFont);
//  fileEndButton.setMnemonic(KeyEvent.VK_END);
    fileEndButton.setToolTipText("End of file.");
    panel2.add(fileEndButton);

    panel1.add(panel2, BorderLayout.CENTER);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(actionListener);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel1.add(exitButton, BorderLayout.EAST);

    /* Dummy panel for buttons for better control of margins. */

    JPanel panel3 = new JPanel(new BorderLayout(0, 0));
    panel3.add(Box.createVerticalStrut(6), BorderLayout.NORTH);
    panel3.add(Box.createHorizontalStrut(7), BorderLayout.WEST);
    panel3.add(panel1, BorderLayout.CENTER);
    panel3.add(Box.createHorizontalStrut(7), BorderLayout.EAST);
    panel3.add(Box.createVerticalStrut(6), BorderLayout.SOUTH);

    /* Create a scrolling text area for the generated output.  The same factors
    for the number of rows and columns must be in the userButton() method.  We
    allow for the maximum file offset: 16 hex digits plus 3 punctuation. */

//  outputText = new JTextArea(20, 60); // plain and simple initial size
    outputText = new JTextArea((rowCount + 2),
      ((columnCount * 4) + ((columnCount + 3) / 4) + 25));
    outputText.addFocusListener((FocusListener) actionListener);
    outputText.addMouseWheelListener((MouseWheelListener) actionListener);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right

    /* Vertical scroll bar.  There is no horizontal scroll bar, because we
    change the window size to match the user's options. */

    scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    scrollBar.addAdjustmentListener((AdjustmentListener) actionListener);
    scrollBar.addMouseWheelListener((MouseWheelListener) actionListener);

    /* Options (centered on bottom). */

    JPanel panel4 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

    JLabel label1 = new JLabel("Rows:");
    label1.setDisplayedMnemonic(KeyEvent.VK_R); // links to <rowSizeDialog>
    if (buttonFont != null) label1.setFont(buttonFont);
    label1.setToolTipText("Number of lines to display.");
    panel4.add(label1);

    rowSizeDialog = new JComboBox(ROW_LIST);
    rowSizeDialog.setEditable(false); // disable editing during layout
    if (buttonFont != null) rowSizeDialog.setFont(buttonFont);
    rowSizeDialog.setPrototypeDisplayValue("999"); // allow for three digits
    label1.setLabelFor(rowSizeDialog); // link label and combo box
    panel4.add(rowSizeDialog);
    panel4.add(Box.createHorizontalStrut(30));

    JLabel label2 = new JLabel("Columns:");
    label2.setDisplayedMnemonic(KeyEvent.VK_C); // links to <columnSizeDialog>
    if (buttonFont != null) label2.setFont(buttonFont);
    label2.setToolTipText("Number of bytes per line.");
    panel4.add(label2);

    columnSizeDialog = new JComboBox(COLUMN_LIST);
    columnSizeDialog.setEditable(false); // disable editing during layout
    if (buttonFont != null) columnSizeDialog.setFont(buttonFont);
    columnSizeDialog.setPrototypeDisplayValue("999"); // allow for three digits
    label2.setLabelFor(columnSizeDialog); // link label and combo box
    panel4.add(columnSizeDialog);
    panel4.add(Box.createHorizontalStrut(30));

    JLabel label3 = new JLabel("Font size:");
    label3.setDisplayedMnemonic(KeyEvent.VK_S); // links to <fontSizeDialog>
    if (buttonFont != null) label3.setFont(buttonFont);
    label3.setToolTipText("Display font point size.");
    panel4.add(label3);

    fontSizeDialog = new JComboBox(SIZE_LIST);
    fontSizeDialog.setEditable(false); // disable editing during layout
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setPrototypeDisplayValue("999"); // allow for three digits
    label3.setLabelFor(fontSizeDialog); // link label and combo box
    panel4.add(fontSizeDialog);
    panel4.add(Box.createHorizontalStrut(30));

    JLabel label4 = new JLabel("Go to (hex):");
    label4.setDisplayedMnemonic(KeyEvent.VK_G); // links to <gotoText>
    if (buttonFont != null) label4.setFont(buttonFont);
    label4.setToolTipText("File offset or location.");
    panel4.add(label4);

    gotoText = new JTextField(10);
    gotoText.setEditable(true);   // user changes this field, presses Enter
    if (buttonFont != null) gotoText.setFont(buttonFont);
    gotoText.setMargin(new Insets(1, 3, 2, 3)); // top, left, bottom, right
    gotoText.addActionListener(actionListener); // do last so don't fire early
    label4.setLabelFor(gotoText); // link label and text field
    panel4.add(gotoText);

    /* Dummy panel for options for better control of margins. */

    JPanel panel5 = new JPanel(new BorderLayout(0, 0));
    panel5.add(Box.createVerticalStrut(6), BorderLayout.NORTH);
    panel5.add(Box.createHorizontalStrut(1), BorderLayout.WEST);
    panel5.add(panel4, BorderLayout.CENTER);
    panel5.add(Box.createHorizontalStrut(1), BorderLayout.EAST);
    panel5.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons above
    the text area.  Keep text in the center so that it expands horizontally and
    vertically.  Put options on the bottom. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    JPanel panel6 = (JPanel) mainFrame.getContentPane(); // content meets frame
    panel6.setLayout(new BorderLayout(0, 0));
    panel6.add(panel3, BorderLayout.NORTH); // buttons
    panel6.add(outputText, BorderLayout.CENTER); // text area
    panel6.add(scrollBar, BorderLayout.EAST); // vertical scroll bar            // optional code
    panel6.add(panel5, BorderLayout.SOUTH); // options

    mainFrame.pack();             // do component layout with minimum size
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight >= MIN_FRAME) && (windowWidth >= MIN_FRAME))
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//  mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* The default width for editable combo boxes is much too wide.  A better
    width is obtained by making the dialog non-editable and fixing the dialog
    at that size, before turning editing back on.  Java 1.4 incorrectly uses
    small spaces for setPrototypeDisplayValue(), probably measured with the
    default GUI font and not our selected font.  Java 5.0 and later have the
    expected size.  Our prototypes in Java 1.4 need four digits; on Java 5.0
    and later, three digits are good, and two digits are almost enough. */

    columnSizeDialog.setMaximumSize(columnSizeDialog.getPreferredSize());
    columnSizeDialog.setEditable(true); // allow user to edit this dialog field
    columnSizeDialog.setSelectedItem(Integer.toString(columnCount));
    columnSizeDialog.addActionListener(actionListener);
                                  // do last so don't fire early

    fontSizeDialog.setMaximumSize(fontSizeDialog.getPreferredSize());
    fontSizeDialog.setEditable(true); // allow user to edit this dialog field
    fontSizeDialog.setSelectedItem(Integer.toString(fontSize));
    fontSizeDialog.addActionListener(actionListener);
                                  // do last so don't fire early

    rowSizeDialog.setMaximumSize(rowSizeDialog.getPreferredSize());
    rowSizeDialog.setEditable(true); // allow user to edit this dialog field
    rowSizeDialog.setSelectedItem(Integer.toString(rowCount));
    rowSizeDialog.addActionListener(actionListener);
                                  // do last so don't fire early

    /* Action map for navigation keys.  We ask for more keys than we will see.
    What we get depends upon which GUI element has keyboard focus.  Many keys
    are consumed by other components, i.e., our JTextArea.  Overriding this is
    non-standard and might lead to strange behavior on some systems.

    Regular keys (no modifier) may not reach the JFrame.  All our desired keys
    work when combined with the Alt key.  The following work when combined with
    the Ctrl key: Down, PageDown, PageUp, Up.  These keys are often unavailable
    with Ctrl (and for good reason): End, Home, Left, Right. */

    InputMap inmap = panel6.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ACTION_LINE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_MASK), ACTION_LINE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK), ACTION_LINE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK), ACTION_PAGE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), ACTION_FILE_END);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.ALT_MASK), ACTION_FILE_END);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_MASK), ACTION_FILE_END);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), ACTION_FILE_START);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.ALT_MASK), ACTION_FILE_START);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_MASK), ACTION_FILE_START);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), ACTION_PAGE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), ACTION_PAGE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.ALT_MASK), ACTION_PAGE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK), ACTION_PAGE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), ACTION_PAGE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.ALT_MASK), ACTION_PAGE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK), ACTION_PAGE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ACTION_LINE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK), ACTION_LINE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK), ACTION_LINE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), ACTION_PAGE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_MASK), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK), ACTION_PAGE_UP);
    ActionMap acmap = panel6.getActionMap();
    acmap.put(ACTION_FILE_END, new HexView1User(ACTION_FILE_END));
    acmap.put(ACTION_FILE_START, new HexView1User(ACTION_FILE_START));
    acmap.put(ACTION_LINE_DOWN, new HexView1User(ACTION_LINE_DOWN));
    acmap.put(ACTION_LINE_UP, new HexView1User(ACTION_LINE_UP));
    acmap.put(ACTION_PAGE_DOWN, new HexView1User(ACTION_PAGE_DOWN));
    acmap.put(ACTION_PAGE_UP, new HexView1User(ACTION_PAGE_UP));

    inmap = outputText.getInputMap(JComponent.WHEN_FOCUSED);
                                  // steal selected mappings from JTextArea
//  inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_MASK), ACTION_FILE_END);
//  inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_MASK), ACTION_FILE_START);
//  inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK), ACTION_LINE_UP);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), ACTION_PAGE_DOWN);
    inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), ACTION_PAGE_UP);
//  inmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK), ACTION_LINE_DOWN);

    /* Allow the user to drag-and-drop files onto this window, including the
    output text area.  Input fields are excluded by default: the combo boxes,
    the "go to" text field, etc. */

    DropTarget drop1 = new DropTarget(mainFrame, (DropTargetListener)
      actionListener);
    DropTarget drop2 = new DropTarget(outputText, (DropTargetListener)
      actionListener);

    /* Let the graphical interface run the application now. */

    if (fileName != null)         // did command line have a file name?
      openFile(new File(fileName), gotoStart); // open user's command file
    else if (true)                // dummy <true> to force file open dialog
      doOpenButton();             // ask user to select a file for reading      // standard code
    else                          // dummy <false> to display empty screen
      openFile(null, 0);          // pretend to open empty file, no name        // optional code

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  closeFile() method

  Close any file currently open and reset all our data about the file.
*/
  static void closeFile()
  {
    if (currentFile != null)      // is a file currently open?
    {
      try { currentFile.close(); } // try to close the file
      catch (IOException ioe) { /* ignore errors */ }
    }
    bufferStart = 0;              // byte offset in file for file buffer
    bufferUsed = 0;               // number of data bytes in file buffer
    currentFile = null;           // don't use this file pointer anymore
    displayStart = 0;             // byte offset in file for text display
    fileSize = 0;                 // no file open, so no total bytes yet
    mainFrame.setTitle(PROGRAM_TITLE); // remove file name from title bar
  }


/*
  displayText() method

  Redraw the display text (hexadecimal file data), even if the file is closed
  or empty.  Over 900 lines of GUI code depend upon this one text-mode method!
*/
  static void displayText(
    boolean scrollFlag)           // true if we also update scroll bar
  {
    int displaySize;              // actual number of bytes to display
    int gridSize;                 // maximum number of bytes in display
    final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'A', 'B', 'C', 'D', 'E', 'F'}; // for converting binary to hexadecimal
    int i;                        // index variable
    long start;                   // temporary file offset

    /* GUI elements change the display position without regard for the actual
    size of the file, or even if a file is currently open.  Adjust the top-left
    corner to fit as best as possible.  Disable buttons in reverse order of
    appearance, to prevent focus traversal problems.  Otherwise, focus may be
    given to the next enabled button just before it also gets disabled. */

    displayStart -= displayStart % columnCount; // align to start of column
    gridSize = rowCount * columnCount; // maximum number of bytes to display
    start = fileSize - 1;         // last file offset (not aligned)
    start -= start % columnCount; // align to start of column
    start -= gridSize;            // go back one complete page
    start += columnCount;         // then forward to first row on last page
    if (displayStart >= start)    // are we at the end of file?
    {
      fileEndButton.setEnabled(false); // yes, disable forward scrolling
      pageDownButton.setEnabled(false);
      lineDownButton.setEnabled(false);
    }
    else                          // no, we can still go forwards
    {
      fileEndButton.setEnabled(true); // enable forward scrolling
      pageDownButton.setEnabled(true);
      lineDownButton.setEnabled(true);
    }
    displayStart = Math.min(displayStart, start); // can't go after ending
    displayStart = Math.max(0, displayStart); // can't go before beginning
    if (displayStart <= 0)        // are we at the start of file?
    {
      lineUpButton.setEnabled(false); // yes, disable backward scrolling
      pageUpButton.setEnabled(false);
      fileStartButton.setEnabled(false);
    }
    else                          // no, we can still go backwards
    {
      lineUpButton.setEnabled(true); // enable backward scrolling
      pageUpButton.setEnabled(true);
      fileStartButton.setEnabled(true);
    }
    displaySize = (int) Math.min((long) gridSize, (fileSize - displayStart));
                                  // actual number of bytes to display

    /* Check if this display range is in our file buffer.  Read from the file,
    if necessary.  File I/O is faster if we keep the buffer aligned.  Put the
    display in the approximate center of the data buffer, to make the next few
    scroll operations faster. */

    if ((currentFile != null) && ((displayStart < bufferStart) ||
      ((displayStart + displaySize) > (bufferStart + bufferUsed))))
    {
      int half = BUFFER_SIZE / 2; // try to center display bytes in buffer
      start = displayStart + (BUFFER_SIZE / 4); // ready to round nearest half
      bufferStart = Math.max(0, (start - (start % half) - half)); // align
      int wanted = (int) Math.min((long) BUFFER_SIZE, (fileSize - bufferStart));
      try
      {
        currentFile.seek(bufferStart); // where to start reading data bytes
        currentFile.readFully(fileBuffer, 0, wanted); // read some data
        bufferUsed = wanted;      // remember how much data we have
      }
      catch (IOException ioe)     // all I/O errors are unexpected here
      {
        JOptionPane.showMessageDialog(mainFrame, ("Can't read "
          + formatComma.format(wanted) + " bytes from file offset 0x"
          + Long.toHexString(bufferStart) + ":\n" + ioe.getMessage()));
        closeFile();              // file may be open, so close again
        System.exit(EXIT_FAILURE); // exit application, with error code
      }
    }

    /* Display file data.  Each line has the file offset in hex, the file bytes
    in hex, and an ASCII representation of the bytes as characters.  Lines are
    created in a fixed-sized character array, then appended to a StringBuffer.
    When finished, the StringBuffer replaces the output text area.  There is no
    way to make this code look pretty and still be reasonably efficient. */

    int firstHex;                 // where hex goes for first data byte
    int offsetDigits;             // number of hex digits in file offsets
    if (fileSize > 0xFFFFFFFFFFFFL) { firstHex = 21; offsetDigits = 16; }
    else if (fileSize > 0xFFFFFFFFL) { firstHex = 16; offsetDigits = 12; }
    else { firstHex = 11; offsetDigits = 8; } // show at least 8 digits

    StringBuffer dump = new StringBuffer(); // build up dump lines here
    int firstText = firstHex + (3 * columnCount) + ((columnCount + 3) / 4) + 1;
    int lineLength = firstText + columnCount + 2; // size including newline
    char[] line = new char[lineLength]; // one line in output
    long thisOffset = displayStart; // file offset from beginning
    int thisIndex = (int) (displayStart - bufferStart); // first buffer index
    int lastIndex = thisIndex + displaySize; // after last buffer index

    while (thisIndex < lastIndex)
    {
      for (i = 0; i < lineLength; i ++) line[i] = ' '; // blank output line
      line[firstText - 1] = line[firstText + columnCount] = '|'; // markers
      line[lineLength - 1] = '\n'; // all lines end with newline character

      /* Show hexadecimal offset from beginning of file. */

      long longValue = thisOffset; // make copy so we can reduce pieces
      int nextHex = firstHex - 3; // where "last" digit goes (low order)
      for (i = 0; i < offsetDigits; i ++) // do digits only as necessary
      {
        line[nextHex --] = hexDigits[(int) (longValue & 0x0F)]; // convert
        longValue = longValue >>> 4; // remove last hex digit (nibble)
        if (((i % 4) == 3) && (nextHex > 0)) // finished one digit group?
          line[nextHex --] = ':'; // yes, digit separator between groups
      }

      /* Show data bytes, both in hexadecimal and as ASCII characters. */

      nextHex = firstHex;         // where "first" digit of data byte goes
      int nextText = firstText;   // where ASCII for first data byte goes
      i = 0;                      // rows may not be complete, so no <for>
      while ((i < columnCount) && (thisIndex < lastIndex))
      {
        i ++;                     // count number of data bytes done
        int intValue = (int) fileBuffer[thisIndex ++]; // get data byte
        line[nextHex + 0] = hexDigits[(intValue >>> 4) & 0x0F];
        line[nextHex + 1] = hexDigits[intValue & 0x0F];
        nextHex += ((i % 4) == 0) ? 4 : 3; // double space every four bytes
        if ((intValue >= 0x20) && (intValue < 0x7F)) // printable ASCII?
          line[nextText] = (char) intValue; // yes, safe as a character
        else                      // can't use this byte as text
          line[nextText] = '.';   // substitute with something we can use
        nextText ++;              // where next ASCII character goes, if any
      }
      dump.append(line);          // append line characters to dump buffer
      thisOffset += columnCount;  // next file offset
    }
    if (thisOffset >= fileSize)   // say nothing more in middle of file
      dump.append("-- end of file --"); // but tell user at end of file

    outputText.setText(dump.toString()); // quick replace of output text area
    outputText.setCaretPosition(dump.length()); // reset caret to after text

    /* Adjust vertical scroll bar to match, if we weren't called from a scroll
    bar event.  Disable the scroll bar listener so that setting a new value
    doesn't generate another scroll bar event.  This is easier than creating
    our own BoundedRangeModel for the scroll bar. */

    if (scrollFlag)               // should we update the scroll bar?
    {
      scrollBar.removeAdjustmentListener((AdjustmentListener) actionListener);
      if ((displayStart <= 0) || (fileSize <= 0)) // at start or file is empty
      {
        scrollBar.setValue(0);    // empty scroll bar has no value, no meaning
      }
      else if (scrollSmall)       // use normal scroll bar configuration?
      {
        scrollBar.setValue((int) (displayStart / columnCount));
      }
      else                        // too many rows for regular scroll bar
      {
                                  // leave one unit of "wiggle" room at start
        scrollBar.setValue(Math.max(1, ((int) ((((double) displayStart) /
          ((double) fileSize)) * ((double) SCROLL_MAX)))));
                                  // truncate, not round, better at end of file
      }
      scrollBar.addAdjustmentListener((AdjustmentListener) actionListener);
    }
  } // end of displayText() method


/*
  doOpenButton() method

  Ask the user to select a file for reading.
*/
  static void doOpenButton()
  {
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
      openFile(fileChooser.getSelectedFile(), 0); // open user's selected file
    else                          // user cancelled file selection dialog box
      displayText(true);          // update hex display and scroll bar
  }


/*
  openFile() method

  Open a given file for random-access reading.  Close any current file first.
*/
  static void openFile(File givenFile, long start)
  {
    closeFile();                  // close any file currently open
    if (givenFile != null) try    // open new file for reading, if given
    {
      currentFile = new RandomAccessFile(givenFile, "r");
      displayStart = start;       // start at file offset given by caller
      fileSize = currentFile.length(); // total size of file in bytes
      if ((fileSize < 0) || (fileSize > MAX_FILE_SIZE))
      {                           // there are limits, even in this program
        JOptionPane.showMessageDialog(mainFrame, ("File is too big: "
          + formatComma.format(fileSize) + " bytes."));
        closeFile();              // file is open, so close again
      }
      else                        // size okay, we can work with this file
        mainFrame.setTitle("Hex File Viewer - "
          + givenFile.getCanonicalFile().getName()); // show correct file name
    }
    catch (IOException ioe)       // error is usually "file not found"
    {
      JOptionPane.showMessageDialog(mainFrame, ("Can't open "
        + givenFile.getName() + " for random-access reading:\n"
        + ioe.getMessage()));     // tell user about the problem
      closeFile();                // file may be open, so close again
    }
    resetScrollbar();             // reconfigure the scroll bar
    displayText(true);            // update hex display and scroll bar

  } // end of openFile() method


/*
  resetScrollbar() method

  Reconfigure the scroll bar after changing the number of rows, columns, or the
  file size.  The current value of the scroll bar will be set by displayText().
  Use our "small" scroll bar when the number of rows is a non-negative integer
  safely less than <Integer.MAX_VALUE>.  Beyond that, switch to a "big" scroll
  bar with a fraction (percentage) of total file size.
*/
  static void resetScrollbar()
  {
    scrollBar.removeAdjustmentListener((AdjustmentListener) actionListener);
    long rows = (fileSize + columnCount - 1) / columnCount; // total rows
    if (rows <= SCROLL_ROWS)      // use normal scroll bar configuration?
    {
      scrollSmall = true;         // using small model for scroll bar
      scrollBar.setBlockIncrement(rowCount - SCROLL_KEEP);
                                  // jump one page, with rows to keep
      scrollBar.setValues(0,      // value (default to zero)
        rowCount,                 // extent (number of rows per page)
        0,                        // minimum
        (int) rows);              // maximum
    }
    else                          // too many rows for regular scroll bar
    {
      scrollSmall = false;        // using big model for scroll bar
      scrollBar.setBlockIncrement(SCROLL_BLOCK); // jump by fixed percentage
      scrollBar.setValues(0,      // value (default to zero)
        0,                        // extent (page size is insignificant)
        0,                        // minimum
        SCROLL_MAX);              // maximum
    }
    scrollBar.addAdjustmentListener((AdjustmentListener) actionListener);

  } // end of resetScrollbar() method


/*
  resizeWindow() method

  Redo the main window layout after changing the number of rows, columns, or
  the font size.  Only applies to "normal" windows that are not maximized or
  minimized (iconified).
*/
  static void resizeWindow()
  {
    if (mainFrame.getExtendedState() == Frame.NORMAL)
      mainFrame.pack();           // do component layout with minimum size
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  HexView1  [options]  [fileName]");
    System.err.println();
    System.err.println("This is a graphical application.  You may give options on the command line:");
    System.err.println();
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -c# = number of columns (bytes per row); default is: -c" + COLUMN_DEFAULT);
    System.err.println("  -f# = monospaced font name for display text; example: -f\"Lucida Console\"");
    System.err.println("  -g# = starting file offset in hexadecimal, for file on command line");
    System.err.println("  -r# = number of rows (lines) in hex display; default is: -r" + ROW_DEFAULT);
    System.err.println("  -s# = font point size for display text; default is: -s" + SIZE_DEFAULT);
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main HexView1 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == columnSizeDialog) // number of columns (bytes per row)
    {
      int size = -1;              // default value for number of columns
      try { size = Integer.parseInt((String) columnSizeDialog.getSelectedItem()); }
      catch (NumberFormatException nfe) { size = -1; } // mark as error
      if ((size < COLUMN_MIN) || (size > COLUMN_MAX))
      {
        JOptionPane.showMessageDialog(mainFrame, (COLUMN_TEXT + "."));
        columnSizeDialog.setSelectedItem(Integer.toString(COLUMN_DEFAULT));
                                  // setSelectedItem() will call us again
      }
      else                        // user entered or selected a valid number
      {
        columnCount = size;       // use this number of columns in hex display
        resetScrollbar();         // reconfigure the scroll bar
        displayText(true);        // update hex display and scroll bar
        outputText.setColumns((columnCount * 4) + ((columnCount + 3) / 4) + 25);
                                  // set preferred columns for text area
        resizeWindow();           // redo window layout with new sizes
      }
    }
    else if (source == exitButton) // "exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fileEndButton) // "end of file" button
    {
      /* All GUI changes to <displayStart> are approximate.  The displayText()
      method will limit the range.  Here, for the end of file, any number equal
      to or greater than the file size will do. */

      displayStart = fileSize;    // go to end of file (after last file offset)
      displayText(true);          // update hex display and scroll bar
    }
    else if (source == fileStartButton) // "start of file" button
    {
      displayStart = 0;           // go to start of file (first file offset)
      displayText(true);          // update hex display and scroll bar
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      int size = -1;              // default value for font point size
      try { size = Integer.parseInt((String) fontSizeDialog.getSelectedItem()); }
      catch (NumberFormatException nfe) { size = -1; } // mark as error
      if ((size < SIZE_MIN) || (size > SIZE_MAX))
      {
        JOptionPane.showMessageDialog(mainFrame, (SIZE_TEXT + "."));
        fontSizeDialog.setSelectedItem(Integer.toString(SIZE_DEFAULT));
                                  // setSelectedItem() will call us again
      }
      else                        // user entered or selected a valid number
      {
        fontSize = size;          // use this point size for output text area
        outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
        resizeWindow();           // redo window layout with new sizes
      }
    }
    else if (source == gotoText)  // "go to" file location or offset
    {
      long offset = -1;           // default value for file offset
      try { offset = Long.parseLong(gotoText.getText(), 16); }
      catch (NumberFormatException nfe) { offset = -1; } // mark as error
      if (offset < 0)             // recognize most errors, ignore maximum
      {
        JOptionPane.showMessageDialog(mainFrame, (HEX_TEXT + "."));
                                  // no need to clean up input text field
      }
      else                        // accept any non-negative number
      {
        displayStart = offset;    // displayText() will limit the range
        displayText(true);        // update hex display and scroll bar
      }
    }
    else if (source == lineDownButton) // "down one line" button
    {
      displayStart += columnCount; // plus one line or row
      displayText(true);          // update hex display and scroll bar
    }
    else if (source == lineUpButton) // "up one line" button
    {
      displayStart -= columnCount; // minus one line or row
      displayText(true);          // update hex display and scroll bar
    }
    else if (source == openButton) // "open file" button
    {
      doOpenButton();             // ask user to select a file for reading
    }
    else if (source == pageDownButton) // "down one page" button
    {
      displayStart += (rowCount - SCROLL_KEEP) * columnCount;
                                  // plus one page, with rows to keep
      displayText(true);          // update hex display and scroll bar
    }
    else if (source == pageUpButton) // "up one page" button
    {
      displayStart -= (rowCount - SCROLL_KEEP) * columnCount;
                                  // minus one page, with rows to keep
      displayText(true);          // update hex display and scroll bar
    }
    else if (source == rowSizeDialog) // number of rows (lines)
    {
      int size = -1;              // default value for number of rows
      try { size = Integer.parseInt((String) rowSizeDialog.getSelectedItem()); }
      catch (NumberFormatException nfe) { size = -1; } // mark as error
      if ((size < ROW_MIN) || (size > ROW_MAX))
      {
        JOptionPane.showMessageDialog(mainFrame, (ROW_TEXT + "."));
        rowSizeDialog.setSelectedItem(Integer.toString(ROW_DEFAULT));
                                  // setSelectedItem() will call us again
      }
      else                        // user entered or selected a valid number
      {
        rowCount = size;          // use this number of rows in hex display
        resetScrollbar();         // reconfigure the scroll bar
        displayText(true);        // update hex display and scroll bar
        outputText.setRows(rowCount + 2); // set preferred rows for text area
        resizeWindow();           // redo window layout with new sizes
      }
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method


/*
  userDropFile() method

  Open a new file by drag-and-drop onto our main window.  Since this program
  does not have multiple subwindows or tabs, only one file is accepted.
*/
  static void userDropFile(DropTargetDropEvent event)
  {
    java.util.List fileList;      // list of File objects from drag-and-drop
    File givenFile;               // one File object given to us by user

    event.acceptDrop(event.getDropAction()); // accept all drops, all types
    try                           // ask for list of files, if available
    {
      fileList = (java.util.List) event.getTransferable().getTransferData(
        DataFlavor.javaFileListFlavor);
    }
    catch (IOException ioe) { fileList = null; }
    catch (UnsupportedFlavorException ufe) { fileList = null; }
    if ((fileList == null)        // if data was not a list of files
      || (fileList.size() != 1)   // or the list has more than one file
      || ((givenFile = (File) fileList.get(0)).isFile() == false))
    {
      event.dropComplete(false);  // we're done, but don't want this data
      JOptionPane.showMessageDialog(mainFrame,
        "Drag-and-drop only accepts one file at a time.");
    }
    else                          // user gave us exactly one file
    {
      event.dropComplete(true);   // accept this file from the user
      openFile(givenFile, 0);     // open user's drag-and-drop file
    }
  } // end of userDropFile() method


/*
  userKey() method

  The caller gives us a command string for a keyboard action.  The only actions
  currently defined are those to mimic navigation buttons for scrolling.
*/
  static void userKey(String command)
  {
    if (command.equals(ACTION_FILE_END))
    {
      fileEndButton.doClick();
    }
    else if (command.equals(ACTION_FILE_START))
    {
      fileStartButton.doClick();
    }
    else if (command.equals(ACTION_LINE_DOWN))
    {
      lineDownButton.doClick();
    }
    else if (command.equals(ACTION_LINE_UP))
    {
      lineUpButton.doClick();
    }
    else if (command.equals(ACTION_PAGE_DOWN))
    {
      pageDownButton.doClick();
    }
    else if (command.equals(ACTION_PAGE_UP))
    {
      pageUpButton.doClick();
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userKey(): unknown command: "
        + command);               // should never happen, so write on console
    }
  } // end of userKey() method


/*
  userMouseWheel() method

  Mouse wheel messages that would normally go to JTextArea.  For anything but
  plain rotation (block scroll, Alt key, Ctrl key, Shift key), do full pages.
*/
  static void userMouseWheel(MouseWheelEvent event)
  {
    int rows;                     // number of rows to scroll, up or down

    if ((event.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL)
      || event.isAltDown() || event.isControlDown() || event.isShiftDown())
    {
      rows = event.getWheelRotation() * (rowCount - SCROLL_KEEP);
                                  // pages up or down, with rows to keep
    }
    else                          // unit scroll, no modifier keys
    {
      rows = event.getUnitsToScroll(); // conform to platform settings
      rows = Math.max(rows, (SCROLL_KEEP - rowCount)); // limit to one page up
      rows = Math.min(rows, (rowCount - SCROLL_KEEP)); // and to one page down
    }
    displayStart += rows * columnCount; // up or down, rows or pages
    displayText(true);            // update hex display and scroll bar
    event.consume();              // nobody else needs to see this event

  } // end of userMouseWheel() method


/*
  userScrollBar(() method

  Vertical scroll bar.  Despite the official documentation, JScrollBar returns
  only TRACK events.  Unit and block increments would be helpful in moving up
  or down exactly one line or page in very big files.  The older AWT Scrollbar
  class has increments.  Unfortunately, it's ugly and doesn't play well with
  newer Swing components.
*/
  static void userScrollBar(AdjustmentEvent event)
  {
    if (scrollSmall)              // use normal scroll bar configuration?
    {
      displayStart = ((long) scrollBar.getValue()) * columnCount;
                                  // convert scroll row to file offset
    }
    else                          // too many rows for regular scroll bar
    {
      displayStart = (long) (((double) fileSize) * ((double)
        scrollBar.getValue()) / ((double) SCROLL_MAX));
                                  // convert scroll fraction to file offset
    }
    displayText(false);           // update hex display, but not scroll bar

  } // end of userScrollBar(() method

} // end of HexView1 class

// ------------------------------------------------------------------------- //

/*
  HexView1User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class HexView1User extends AbstractAction implements ActionListener,
  AdjustmentListener, DropTargetListener, FocusListener, MouseWheelListener
{
  /* constructor */

  public HexView1User(String command)
  {
    super();                      // initialize our superclass (AbstractAction)
    this.putValue(Action.NAME, command); // save action name for later decoding
  }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    String command = (String) this.getValue(Action.NAME); // get saved action
    if (command == null)          // was there a keyboard action name?
      HexView1.userButton(event); // no, process as regular button or dialog
    else                          // yes, there was a saved keyboard action
      HexView1.userKey(command);  // process as a regular keyboard command
  }

  /* vertical scroll bar */

  public void adjustmentValueChanged(AdjustmentEvent event)
  {
    HexView1.userScrollBar(event);
  }

  /* drag-and-drop file listener */

  public void dragEnter(DropTargetDragEvent event) { /* do nothing */ }
  public void dragExit(DropTargetEvent event) { /* do nothing */ }
  public void dragOver(DropTargetDragEvent event) { /* do nothing */ }
  public void drop(DropTargetDropEvent event)
  {
    HexView1.userDropFile(event);
  }
  public void dropActionChanged(DropTargetDragEvent event) { /* do nothing */ }

  /* focus listener for output text area */

  public void focusGained(FocusEvent event)
  {
    HexView1.outputText.getCaret().setVisible(true); // show text caret
  }

  public void focusLost(FocusEvent event)
  {
    HexView1.outputText.getCaret().setVisible(false); // hide text caret
  }

  /* mouse wheel listener, steals from JTextArea */

  public void mouseWheelMoved(MouseWheelEvent event)
  {
    HexView1.userMouseWheel(event);
  }

} // end of HexView1User class

/* Copyright (c) 2014 by Keith Fenske.  Apache License or GNU GPL. */
