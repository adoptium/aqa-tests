/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ReadWriteTest {
    protected Charset charset;
    protected CharsetDecoder decoder;
    protected CharsetEncoder encoder;
    protected CharBuffer cbuf;
    protected JTextArea textarea = null;
    protected boolean winmode = false;

ReadWriteTest(boolean winmode){
  if (winmode) {
    this.winmode = winmode;
    JFrame frame = new JFrame("New I/O Test");
    frame.getContentPane().setLayout(new BorderLayout());
    JScrollPane scrollPane = new JScrollPane();
    textarea = new JTextArea(25, 80);
    scrollPane.getViewport().add(textarea);
    frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    frame.addWindowListener(new WindowAdapter(){
        public void windowClosing(WindowEvent e){System.exit(0);}
    });
    frame.pack();
    frame.setVisible(true);
  }
}

public void read(File f, String encoding) throws IOException {
    charset = Charset.forName(encoding);
    decoder = charset.newDecoder();

    // Open the file and then get a channel from the stream
    FileInputStream fis = new FileInputStream(f);
    FileChannel fc = fis.getChannel();

    // Get the file's size and then map it into memory
    int sz = (int)fc.size();
    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

    // Decode the file into a char buffer
    cbuf = decoder.decode(bb);

    // Show the string
    if(winmode && textarea != null){
        textarea.setText(cbuf.toString());
    }else{
        System.out.println(cbuf.toString());
    }

    // Close the channel and the stream
    fc.close();
}

public void write(File f, String encoding) throws IOException {
    charset = Charset.forName(encoding);
    encoder = charset.newEncoder();

    // Open the file and then get a channel from the stream
    FileOutputStream fos = new FileOutputStream(f);
    FileChannel fc = fos.getChannel();

    // Encode the string into a byte buffer
    ByteBuffer bb = encoder.encode(cbuf);

    // write into the file
    fc.write(bb);

    // Close the channel and the stream
    fc.close();
}

public static void main(String[] args) {
    boolean winmode = false;
    if (args.length < 4) {
        System.err.println("Usage: java ReadWriteTest infile inenc outfile outenc");
        return;
    }
    if(args.length > 4 && args[4].toLowerCase().equals("-w") ){
       winmode = true;
    }
    ReadWriteTest rwt = new ReadWriteTest(winmode);
    File fr = new File(args[0]);
    File fw = new File(args[2]);
    try {
        System.out.println("Reading:" + args[0]);
        rwt.read(fr, args[1]);
        System.out.println("Writing:" + args[2]);
        rwt.write(fw, args[3]);
    }catch(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }
}
}
